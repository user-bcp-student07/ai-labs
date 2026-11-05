# Tasks

Alcance: Issue #24 (`H-AUTH-01`), solo componente `auth/`. Los comandos de prueba se ejecutan desde `auth/`; Git y OpenSpec, desde la raíz de `ai-labs`. Referencias: `specs/auth-login/spec.md` y `design.md`.

## 1. Caracterización del comportamiento a preservar

- [x] 1.1 Ejecutar `mvn test` desde `auth/` sin modificar nada y registrar que `AuthResourceTest.logsInWithWorkshopUser` pasa (baseline verde). Comando: `mvn test "-Dquarkus.http.test-port=18081"`; resultado: `BUILD SUCCESS`, 1 prueba, 0 fallos.
- [x] 1.2 Agregar en `AuthResourceTest` una prueba de `trainer` / `Trainer123!` con 200 y `user.role` igual a `TRAINER`; verificar que pasa sobre el código sin cambios. Prueba: `logsInWithTrainerUser`; comando: `mvn test "-Dquarkus.http.test-port=18081"`; resultado: pasa.
- [x] 1.3 Agregar pruebas de contraseña incorrecta para un usuario existente y de usuario inexistente, ambas con 401 y sin `token`; verificar que pasan sobre el código sin cambios. Pruebas: `rejectsIncorrectPasswordForExistingUser`, `rejectsUnknownUser`; mismo comando; resultado: pasan.
- [x] 1.4 Agregar pruebas de usuario vacío y de contraseña vacía con 400 y sus mensajes "Username is required" y "Password is required"; verificar que pasan sobre el código sin cambios. Pruebas: `rejectsEmptyUsername`, `rejectsEmptyPassword`; mismo comando; resultado: pasan.
- [x] 1.5 Agregar una prueba con usuario `' OR '1'='1` y una contraseña arbitraria que espere 401 sin `token`; verificar que pasa sobre el código sin cambios. Prueba: `rejectsSqlLikeUsernameWithArbitraryPassword`; mismo comando; resultado: pasa. Resultado global de 1.2 a 1.5: `BUILD SUCCESS`, 7 pruebas, 0 fallos, 0 errores.

## 2. Pruebas que demuestran la vulnerabilidad

- [x] 2.1 Agregar una prueba con usuario `student` y contraseña `' OR '1'='1` que espere 401 sin `token`; ejecutarla sin tocar código de producción y verificar que falla porque hoy responde 200. Prueba: `rejectsSqlLikePasswordForExistingUser`; comando: `mvn test "-Dquarkus.http.test-port=18081"`; resultado: falla con "Expected status code <401> but was <200>".
- [x] 2.2 Agregar una prueba con usuario y contraseña `' OR '1'='1` que espere 401 sin `token`; ejecutarla sin tocar código de producción y verificar que falla porque hoy responde 200. Prueba: `rejectsSqlLikeUsernameAndPassword`; mismo comando; resultado: falla con "Expected status code <401> but was <200>".
- [x] 2.3 Si 2.1 o 2.2 no fallan en el código actual, anotar el resultado real y ajustar el dato de prueba, no la spec; verificar que al menos una prueba de inyección falla antes del cambio. Ambas fallaron como se esperaba; no se ajustó dato de prueba ni spec. Resultado global: 9 pruebas, 2 fallos (solo 2.1 y 2.2), 0 errores.

## 3. Corrección de `findByCredentials`

- [x] 3.1 En `LegacyDatabase.findByCredentials`, reemplazar la concatenación de `username` y `password` por una consulta parametrizada, sin cambiar firma, columnas, construcción de `UserRecord`, retorno `null` ni el manejo de excepciones; verificar con `git diff` que solo cambia ese método y que ningún valor de entrada se concatena al SQL. Evidencia: `git diff -- auth/src/main` muestra solo el import de `PreparedStatement` y el cuerpo de `findByCredentials` (`WHERE username=? AND password=?` con `setString`).
- [x] 3.2 Ejecutar `mvn test` desde `auth/` y verificar que las pruebas de 1.x y 2.x pasan, incluidas las que fallaban en 2.1 y 2.2. Comando: `mvn test "-Dquarkus.http.test-port=18081"`; resultado: `BUILD SUCCESS`, 9 pruebas, 0 fallos, 0 errores.

## 4. Validación final del cambio

- [x] 4.1 Ejecutar `mvn verify` desde `auth/` y verificar que termina sin errores y genera el reporte de JaCoCo. Comando: `mvn verify "-Dquarkus.http.test-port=18081"`; resultado: `BUILD SUCCESS`, 9 pruebas, 0 fallos; reporte en `auth/target/site/jacoco/`.
- [x] 4.2 Revisar en el reporte de JaCoCo la cobertura de `findByCredentials` y verificar que alcanza 85% o más; si no, registrar el valor real y qué líneas quedan sin cubrir, sin agregar pruebas artificiales. Resultado: 94% de instrucciones y 75% de ramas (>= 85% de instrucciones); solo queda sin cubrir el `catch` (línea 44, `error.printStackTrace()`), no ejercitable sin forzar una falla de SQLite.
- [x] 4.3 Ejecutar `git status --short -- auth api web` desde la raíz y verificar que solo cambian `LegacyDatabase.java` y `AuthResourceTest.java`, ambos en `auth/`. Resultado: solo `M auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java` y `M auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.
- [x] 4.4 Ejecutar `openspec validate --changes "parameterize-auth-login-queries"` desde la raíz y verificar que pasa. Resultado: `1 passed, 0 failed`.

## Workflow follow-up

- Revisión humana del diff y de las pruebas antes de integrar, según `AGENTS.md`.
- Archivar el cambio OpenSpec una vez cumplidos los requisitos de revisión del proyecto.
