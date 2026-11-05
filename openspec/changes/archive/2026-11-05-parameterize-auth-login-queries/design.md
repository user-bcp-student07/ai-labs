# Design

## Context

Motivación y alcance: ver `proposal.md` (Issue #24, hallazgo `H-AUTH-01`) y `specs/auth-login/spec.md`. Este documento solo cubre el *cómo*.

Estado actual observado en `auth/` (leído del código, no de `docs/ENDPOINTS.md`, que no existe en el workspace):

- `AuthResource.login` (`POST /auth/login`) valida la entrada y delega en `LegacyDatabase.findByCredentials(username, password)`. Si devuelve `null` responde 401; si no, crea el token y responde 200.
- `findByCredentials` arma el SQL concatenando `username` y `password` y lo ejecuta con un `Statement` (`LegacyDatabase.java`, línea 32). Cualquier excepción se imprime y el método devuelve `null`, que el recurso traduce a 401.
- La base es SQLite (`AUTH_DB_URL`, por defecto `jdbc:sqlite:./data/workshop.db`), con la tabla `legacy_users` y las cuentas sembradas `student` y `trainer`. El login no escribe en la base.
- La única prueba existente es `AuthResourceTest.logsInWithWorkshopUser` (`@QuarkusTest`, REST-assured).
- JaCoCo ya está configurado en `auth/pom.xml`; la meta de cobertura del código modificado es 85% o superior (`AGENTS.md`, sección auth).

Comportamiento actual de la vulnerabilidad, razonado a partir del SQL (se confirma ejecutando las pruebas del paso 1 sobre el código sin modificar):

- Contraseña `' OR '1'='1` con usuario `student`: la condición queda `username='student' AND password='' OR '1'='1'`, que es verdadera para toda fila, así que el login se completa.
- Usuario `' OR '1'='1` con una contraseña cualquiera: `AND` se evalúa antes que `OR`, de modo que la condición solo es verdadera si esa contraseña existe en alguna fila; con una contraseña arbitraria hoy ya responde 401.
- Usuario y contraseña ambos `' OR '1'='1`: la condición es verdadera para toda fila y el login se completa.

## Goals / Non-Goals

**Goals:**

- Que `findByCredentials` trate `username` y `password` como valores literales y nunca como parte de la sintaxis SQL (criterio 1 del Issue).
- Dejar sin cambios todo lo observable por el cliente en `POST /auth/login`, salvo que los intentos de inyección dejen de autenticar.
- Dejar pruebas automatizadas que fallen si la vulnerabilidad reaparece (criterio 4 del Issue).

**Non-Goals:**

- Refactorizar `AuthResource` (anidamiento, constantes `3600`, separación API/servicio/repositorio). Las reglas de clean code de `AGENTS.md` aplican, pero se tratarán en un cambio aparte para mantener este diff revisable.
- Hashear contraseñas, mover credenciales sembradas, cambiar `LegacyTokenUtil` o su secreto.
- Cambiar los mensajes de error, incluido el 401 que refleja el usuario enviado, o el 500 que expone el nombre de la excepción.
- Cambiar cómo se manejan las fallas de base de datos (hoy terminan en 401).
- Tocar `api/`, `web/`, el esquema de la base, la configuración o las dependencias.

## Contratos protegidos

Se separan los comprometidos por la spec de los observados en el código que este cambio no debe alterar.

Comprometidos por la spec:

- `student` / `Workshop123!` en `POST /auth/login` responde 200, con `user.username` igual a `student` y `token` presente.
- Credenciales con sintaxis SQL no autentican: 401 y sin `token`.

Observados en el código y protegidos como no regresión (fuera de la spec, no se ampliará la spec con ellos):

- Cuerpo nulo: 400 con `message` "Request is null".
- Usuario vacío o nulo: 400, "Username is required". Contraseña vacía o nula: 400, "Password is required".
- Credenciales válidas pero incorrectas: 401 con `message` "Invalid user <usuario> or password".
- Éxito: campos `token`, `expiresIn` (3600), `user.username`, `user.displayName`, `user.role` y `debug`.
- La firma pública `UserRecord findByCredentials(String, String)` y su convención de devolver `null` cuando no hay coincidencia.

## Componentes afectados

- `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`: único archivo de producción que cambia, y solo `findByCredentials`.
- `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`: se amplía con las pruebas de caracterización e inyección.

Sin cambios: `AuthResource`, `LoginRequest`, `UserRecord`, `LegacyTokenUtil`, `application.properties`, `pom.xml`.

## Decisions

### D1. Consulta parametrizada con JDBC estándar

`findByCredentials` usará una consulta con marcadores de posición para `username` y `password` y enlazará los valores recibidos, en lugar de concatenarlos. Se conservan la tabla, las columnas seleccionadas y la construcción de `UserRecord`.

Por qué: es lo que exige el Issue, usa la API que ya está en uso (`java.sql`) y es la defensa que elimina la causa, no el síntoma.

Alternativas consideradas:

- Escapar o sanear las comillas antes de concatenar: rechazada, frágil y dependiente del dialecto; deja la construcción dinámica del SQL.
- Lista de caracteres prohibidos en `AuthResource`: rechazada, rompería contraseñas legítimas con comillas y es evadible.
- Buscar solo por usuario y comparar la contraseña en Java: rechazada, cambia el contrato interno y el Issue pide parámetros para ambos campos.
- Introducir ORM o Panache: rechazada, agrega una dependencia y amplía el alcance sin beneficio para una consulta.

### D2. Contrato interno y manejo de errores sin cambios

Se mantienen la firma, el valor `null` ante ausencia de coincidencia y el `try/catch` existente. Un fallo de base de datos seguirá resultando en 401.

Por qué: cambiarlo alteraría un contrato observable que la spec no menciona y mezclaría dos cambios en un mismo diff. El efecto de enmascarar fallas de infraestructura queda registrado en Risks.

### D3. La validación de entrada permanece en `AuthResource`

No se mueve ni se duplica validación. La parametrización hace innecesario filtrar caracteres especiales, así que cualquier cadena no vacía llega a la consulta como dato.

### D4. Pruebas sobre HTTP con el stack existente

Se usan `@QuarkusTest` y REST-assured, como la prueba actual, y las cuentas ya sembradas. No se añade infraestructura de pruebas ni se escribe en la base.

Por qué: la spec define comportamiento observable en el endpoint, y ejercerlo por HTTP cubre `AuthResource` y `LegacyDatabase` a la vez.

## Pruebas de caracterización necesarias antes de modificar

Se agregan antes de tocar `findByCredentials`, en `AuthResourceTest`, y se ejecutan primero sobre el código sin cambios.

Deben pasar antes y después (fijan el comportamiento a preservar):

1. `student` / `Workshop123!` responde 200, `user.username` es `student` y `token` presente (ya existe).
2. `trainer` / `Trainer123!` responde 200 con `user.role` igual a `TRAINER`.
3. Usuario existente con contraseña incorrecta responde 401 sin `token`.
4. Usuario inexistente responde 401 sin `token`.
5. Usuario vacío y contraseña vacía responden 400 con sus mensajes.
6. Usuario `' OR '1'='1` con una contraseña arbitraria responde 401 sin `token` (guarda de regresión; hoy ya es 401).

Deben fallar antes y pasar después (demuestran el defecto y su corrección):

7. Usuario `student` con contraseña `' OR '1'='1`: hoy 200, esperado 401 sin `token`.
8. Usuario y contraseña `' OR '1'='1`: hoy 200, esperado 401 sin `token`.

Los casos 7 y 8 cubren los escenarios "usuario" y "contraseña" de la spec con datos que efectivamente explotan la concatenación actual (ver Context). Si al ejecutarlos sobre el código sin modificar alguno no falla, se documenta el resultado real y se ajusta el dato de prueba, no la spec.

## Estrategia incremental

1. Agregar las pruebas 2 a 6 y ejecutarlas sobre el código actual: deben pasar. Esto fija el baseline.
2. Agregar las pruebas 7 y 8 y ejecutarlas sin modificar producción: deben fallar. Se conserva la evidencia de la falla.
3. Cambiar únicamente `findByCredentials` según D1.
4. Ejecutar `mvn test` desde `auth/`: todas deben pasar. Revisar el reporte de JaCoCo para el método modificado.
5. Revisión humana del diff y de las pruebas antes de integrar.

Cada paso es un cambio revisable por separado; no se mezclan pruebas y producción en el mismo commit cuando sea posible.

## Risks / Trade-offs

- [Las fallas de base de datos siguen apareciendo como 401] → Se acepta: es el comportamiento actual y está fuera de alcance. Queda como seguimiento.
- [La cobertura del bloque `catch` puede quedar baja porque no hay forma sencilla de provocar una falla de SQLite en las pruebas] → Las rutas de coincidencia y no coincidencia sí se cubren; si la medición del método modificado queda bajo 85%, se reporta el valor real en vez de forzar una prueba artificial.
- [Los datos de prueba dependen de las cuentas sembradas, y el archivo `./data/workshop.db` es relativo al directorio de ejecución] → Las pruebas se ejecutan desde `auth/`, como indica `AGENTS.md`, y no escriben ni modifican la base.
- [El baseline `docs/ENDPOINTS.md` no existe] → Los contratos protegidos se derivan del código y del Issue, como se declaró en la propuesta. Si aparece ese documento, debe reconciliarse con la lista de arriba.
- [Reglas de clean code incumplidas en `AuthResource` (anidamiento, constantes)] → No se corrigen aquí; mantener el diff mínimo reduce el riesgo de regresión en un cambio de seguridad.
- [Mensaje 401 que refleja el usuario enviado] → No se modifica por estar fuera del Issue; se deja como hallazgo potencial para el registro del Día 1.

## Migration Plan

No hay migración de datos, configuración ni dependencias: el cambio es de comportamiento interno de una consulta.

Rollback: revertir el commit que modifica `findByCredentials`. No deja estado que limpiar. Revertir restaura la vulnerabilidad, por lo que solo se justifica si se detecta una regresión funcional, y las pruebas 1 a 6 sirven para detectarla. No se requiere compatibilidad hacia atrás adicional porque el contrato HTTP no cambia.
