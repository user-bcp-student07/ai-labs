# Hallazgos

## Contrato observado — api

`docs/ENDPOINTS.md` no existe; por tanto, lo siguiente describe el comportamiento
que se observa en el código y no constituye un contrato acordado.

| Método y ruta | Autenticación | Comportamiento observado |
|---|---|---|
| `GET /api/tasks` | `Authorization: Bearer <token>` | Devuelve una lista con `id`, `title`, `description`, `status`, `owner`, `priority` y `createdAt`. Acepta `owner` como query param; si falta o está vacío, usa el usuario del token. |
| `POST /api/tasks` | `Authorization: Bearer <token>` | Acepta un objeto JSON; `status` y `priority` tienen valores por defecto `TODO` y `MEDIUM`. Devuelve la tarea con estado HTTP 201. El cliente puede enviar `owner`. |
| `GET /api/summary` | `Authorization: Bearer <token>` | Devuelve `owner`, `total`, `byStatus` y `highPriorityOpen`. Acepta `owner` como query param y usa el usuario del token solo si no se especifica. |
| `POST /api/requirements` | `Authorization: Bearer <token>` | Recibe el campo multipart `file`, guarda su contenido y devuelve `owner`, `fileName`, `storedAt` y `size`, con estado HTTP 201. |
| `GET /api/backend-health` | `Authorization: Bearer <token>` | Consulta la URL de `AUTH_HEALTH_URL` (por defecto, `http://localhost:8080/q/health`) y devuelve `requestedBy`, `status` y `body`. |

El backend no declara una ruta `/auth/login`: las rutas anteriores dependen de
`legacy_identity`. Esta función quita el prefijo `Bearer `, decodifica Base64 y
espera `username|role|expiración|secreto`. Compara el secreto fijo y rechaza una
expiración menor que la hora actual, pero no verifica firma, emisor ni
audiencia; los errores de decodificación se incluyen en el detalle HTTP 401.

`create_app` abre una conexión SQLite al crear la aplicación, crea la tabla
`tasks` si falta y carga tres registros solo cuando la tabla está vacía. Las
rutas consultan y modifican esa conexión directamente. La carga de archivos
construye la ruta de destino con el nombre recibido, lee el archivo completo en
memoria y lo escribe sin validación observada de nombre, extensión o tamaño.

## Matriz de seguimiento

| id | componente | hallazgo | tipo | explicacion | evidencia (archivo:linea) | severidad | estado |
|---|---|---|---|---|---|---|---|
| H-API-01 | api | Token Base64 con secreto embebido | Vulnerabilidad | El contenido solo estaba codificado, no firmado; el secreto estaba fijado en el código y no se validaban emisor ni audiencia. Remediación API implementada: JWT HS256 valida firma, issuer, audience, expiración y claims requeridas, con configuración externa y errores genéricos. La actualización/compatibilidad del emisor externo y la configuración integrada de despliegue aún no se verificaron. | api/app/main.py:13-60, api/tests/test_api.py:13-63, api/tests/test_api.py:158-184 | alta | Pendiente |
| H-API-02 | api | Autorización permite consultar o crear para otro `owner` | Vulnerabilidad | `owner` de la consulta y `owner` del cuerpo se aceptaban sin vincularlos a la identidad del token. Remediación verificada: consultas y creación se limitan al `username` autenticado; las pruebas cubren query/body con `owner` de otro usuario. | api/app/main.py:99-125, api/app/main.py:129-135, api/tests/test_api.py:65-93 | alta | Resuelto |
| H-API-03 | api | Filtros e inserts SQL concatenados | Vulnerabilidad | Los valores de `owner` y los campos de la tarea se concatenaban en SQL. Remediación verificada: filtros, insert y selección por ID usan parámetros enlazados; la prueba conserva texto con comillas como dato. | api/app/main.py:101-124, api/app/main.py:131-135, api/tests/test_api.py:74-93 | alta | Resuelto |
| H-API-04 | api | Nombre de archivo sin normalizar ni límites de tipo y tamaño | Vulnerabilidad | El nombre proporcionado se usaba directamente y el archivo se leía completo. Remediación verificada: se rechazan separadores de ruta POSIX/Windows y extensiones no permitidas; la lectura es por bloques con límite de 5 MiB y se elimina cualquier carga parcial rechazada. | api/app/main.py:20-23, api/app/main.py:149-185, api/tests/test_api.py:96-156 | alta | Resuelto |
| H-API-05 | api | Versiones de dependencias candidatas a revisión SCA | Sugerencia | `urllib3` y `python-multipart` están fijadas a las versiones indicadas por el reto. No se verificaron advisories en este Discovery; debe comprobarse si les aplica un CVE antes de afirmar vulnerabilidad o elegir una actualización. | api/requirements.txt:4-5 | media | Aprobado |
| H-API-06 | api | Tipado débil, excepciones amplias, lógica anidada y repetida | Refactor | Los payloads y respuestas usan diccionarios sin modelos de dominio; hay capturas de `Exception`, condicionales anidados y lógica de acceso a datos/serialización mezclada. Esto reduce validación y claridad, y el detalle de excepciones puede filtrarse al cliente. | api/app/main.py:17-26, api/app/main.py:56-68, api/app/main.py:78-88, api/app/main.py:94-104, api/app/main.py:120-121 | media | Aprobado |
| H-API-07 | api | Rutas, reglas, persistencia y serialización concentradas en un archivo | Refactor | `create_app` contiene configuración y acceso a SQLite, y las rutas realizan consultas, reglas y construcción de respuestas directamente. Esta concentración dificulta probar y cambiar responsabilidades sin afectar otras rutas. | api/app/main.py:29-54, api/app/main.py:56-125 | baja | Aprobado |
