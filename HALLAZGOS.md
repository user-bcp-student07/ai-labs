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
| H-API-07 | api | Rutas, reglas, persistencia y serialización concentradas en un archivo | Refactor | Remediado mediante la separación de composición de aplicación, rutas, autenticación, servicio de tareas, repositorio SQLite e inicialización de base de datos. Se conservaron los contratos observados; baseline previo 9/9 y regresión posterior 13/13, con cobertura total 95.11%. | api/app/main.py, api/app/routes.py, api/app/task_service.py, api/app/task_repository.py, api/app/database.py, api/app/security.py, api/tests/test_api.py | baja | Resuelto |
| H-API-08 | api | Creación de tareas acepta campos vacíos y convierte tipos arbitrarios a texto | Calidad de datos / contrato | El endpoint no valida campos con un modelo: convierte valores a `str` y permite que falten `title` y `description`, almacenándolos como cadenas vacías. | api/app/main.py:68, api/app/main.py:111-118 | media | CANDIDATO |
| H-API-09 | api | Fallos del servicio de salud se responden como éxito HTTP | Fiabilidad / exposición de información | Si falla la llamada a `AUTH_HEALTH_URL`, la ruta devuelve su respuesta normal con `status: 0` y el texto de la excepción, por lo que el HTTP queda en 200 y puede revelar detalles internos. | api/app/main.py:183-188 | media | CANDIDATO |
| H-API-10 | api | Conexión SQLite compartida sin serialización explícita | Fiabilidad | La aplicación comparte una conexión SQLite entre solicitudes y desactiva la comprobación de hilo, pero las rutas consultan y confirman operaciones sobre esa conexión sin sincronización explícita. Bajo concurrencia, el comportamiento depende de la serialización efectiva del driver y puede causar fallos intermitentes. | api/app/main.py:66-67, api/app/main.py:101-123, api/app/main.py:190-192 | media | CANDIDATO |
| H-API-11 | api | Las cargas de archivos comparten un espacio de nombres entre usuarios | Fiabilidad / aislamiento | El destino se forma solo con el nombre recibido y se crea en modo exclusivo; cualquier usuario que ya haya subido ese nombre impide que otro usuario lo use, aunque sean identidades distintas. | api/app/main.py:160-179 | baja | CANDIDATO |

## Candidatos para revisión humana — api

### H-API-08 — Creación de tareas acepta campos vacíos y convierte tipos arbitrarios a texto
- Estado: CANDIDATO
- Evidencia: `api/app/main.py:68` declara `title` y `description` como `NOT NULL`, pero no impide cadenas vacías; `api/app/main.py:111-118` convierte con `str()` los valores recibidos y usa `""` cuando faltan título o descripción. La prueba existente de creación en `api/tests/test_api.py` cubre datos textuales válidos, no esos casos de borde.
- Impacto: Se pueden persistir tareas sin título o descripción y valores con tipos inesperados convertidos a su representación textual; consumidores reciben registros que podrían incumplir el contrato de tarea útil.
- Prioridad propuesta: Media.
- Contrato potencialmente afectado: `POST /api/tasks`, formato de entrada y garantías de campos de la tarea en la respuesta 201.
- Cómo validarlo: Añadir pruebas para título/descripción ausentes, vacíos, `null` y valores no textuales; comprobar el código HTTP y que no se persistan tareas inválidas conforme al contrato que se acuerde.

### H-API-09 — Fallos del servicio de salud se responden como éxito HTTP
- Estado: CANDIDATO
- Evidencia: `api/app/main.py:183-188` captura cualquier excepción de la consulta y devuelve un diccionario ordinario con `status: 0` y `str(error)`. No se asigna un código HTTP de error a esa respuesta.
- Impacto: Monitores y clientes que evalúan el código HTTP pueden interpretar como exitoso el endpoint aunque el backend consultado no responda; el cuerpo también puede exponer detalles de conexión o configuración.
- Prioridad propuesta: Media.
- Contrato potencialmente afectado: `GET /api/backend-health`, semántica del código HTTP y contenido de `body` ante fallo de dependencia.
- Cómo validarlo: Simular una excepción de `urllib3.PoolManager().request` y verificar el código HTTP y el cuerpo observados; acordar y probar una respuesta de fallo que no filtre detalles internos.

### H-API-10 — Conexión SQLite compartida sin serialización explícita
- Estado: CANDIDATO
- Evidencia: `api/app/main.py:66-67` crea una sola conexión con `check_same_thread=False`; las rutas la usan directamente para lecturas, inserciones y `commit` en `api/app/main.py:101-123`. No se observa un bloqueo ni una conexión por operación.
- Impacto: Solicitudes concurrentes pueden usar simultáneamente el mismo objeto de conexión; hay riesgo de errores transitorios o interferencia entre operaciones, sujeto a la configuración de SQLite y del driver.
- Prioridad propuesta: Media.
- Contrato potencialmente afectado: Disponibilidad y consistencia de `GET /api/tasks`, `GET /api/summary` y `POST /api/tasks` bajo concurrencia.
- Cómo validarlo: Ejecutar una prueba concurrente repetible de lecturas y creaciones sobre la misma instancia de aplicación, verificar errores/respuestas y comprobar que el conjunto persistido coincide con las creaciones confirmadas.

### H-API-11 — Las cargas de archivos comparten un espacio de nombres entre usuarios
- Estado: CANDIDATO
- Evidencia: `api/app/main.py:160-165` construye `target` dentro de un único directorio a partir de `filename` y lo abre con modo exclusivo `"xb"`; `api/app/main.py:168-169` traduce un nombre ya existente a HTTP 409. La identidad solo se incluye en la respuesta (`api/app/main.py:179`), no en la ruta de almacenamiento.
- Impacto: Un usuario puede ocupar anticipadamente nombres previsibles y causar conflictos de carga a otros usuarios con el mismo nombre; los archivos tampoco quedan separados por identidad.
- Prioridad propuesta: Baja; confirmar si el producto exige aislamiento de archivos por usuario.
- Contrato potencialmente afectado: `POST /api/requirements`, unicidad de `fileName`, semántica de HTTP 409 y aislamiento asociado a `owner`.
- Cómo validarlo: Subir el mismo nombre desde dos identidades autenticadas en el mismo directorio y observar que la segunda solicitud recibe 409; decidir si el comportamiento es el contrato deseado o si los archivos deben tener espacios de nombres separados.
