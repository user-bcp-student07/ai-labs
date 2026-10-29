# Design

Cambio: `auth-token-firmado-login`. Referencia: GitHub Issue [#24](https://github.com/user-bcp-student07/ai-labs/issues/24) (HU-01). Componente: `auth/`.

## Contexto técnico

- `POST /auth/login` (`AuthResource`) valida la petición, llama a `AuthService.authenticate`, que consulta `LegacyDatabase` y emite el token con `JwtTokenService.issue`.
- `JwtTokenService` usa `io.jsonwebtoken` (jjwt) con HMAC-SHA256 (HS256). La clave llega por `auth.token.key`, enlazada a la variable `AUTH_TOKEN_KEY`. La expiración es constante (`EXPIRATION_SECONDS = 3600`). Los claims son `sub`, `role`, `iat` y `exp`.
- `application.properties` define `auth.token.key=${AUTH_TOKEN_KEY:}` en el perfil por defecto y un literal solo en `%test`.
- Las pruebas existentes ya cubren parte del comportamiento: `JwtTokenServiceTest` (emisión válida, claims y expiración, payload alterado, otra clave, token malformado, clave en blanco) y `AuthResourceTest` (login exitoso, errores 400 y 401, método y content-type no permitidos, error 500 controlado, CORS).
- El cambio es, en esencia, de **verificación y especificación**: el comportamiento exigido por el Issue ya existe en el código.

## Goals

1. Demostrar con pruebas automatizadas que un token emitido pasa la verificación de firma y que uno alterado se rechaza (criterios 1 y 2 del Issue).
2. Demostrar con una prueba de contrato que `POST /auth/login` conserva `token`, `expiresIn`, `user` y `debug` con sus tipos (criterios 3 y 4).
3. Cerrar en la spec las brechas entre el comportamiento actual y los escenarios de la spec (ver Riesgos), sin cambiar el comportamiento observable salvo que se decida explícitamente.
4. Mantener la cobertura del código modificado en 85 % o más, según el contrato de agente de `auth/`.

## Non-goals

- No cambiar el algoritmo (HS256), el formato del token, los claims ni la expiración.
- No añadir endpoint de validación, refresh, revocación ni rotación de claves.
- No modificar la respuesta 500 ni su exposición de `message` y `exception` (H-AUTH-05).
- No eliminar el campo `debug` (H-AUTH-07).
- No cambiar `PasswordHasher`, `LegacyDatabase` ni CORS.
- No refactorizar `AuthResource` por capas. La regla de clean code 8 aplica, pero la separación debe ir en un cambio propio y revisable, no en este.
- No crear `tasks.md` en esta etapa.

## Contratos protegidos

| Contrato | Fuente | Protección |
|---|---|---|
| Ruta `POST /auth/login`, `Content-Type: application/json`, sin `Authorization` | `docs/ENDPOINTS.md` | Pruebas existentes (`AuthResourceTest`) |
| 200: `token` string, `expiresIn` 3600, `user` {`username`, `displayName`, `role`}, `debug` = `authenticated-with-legacy-flow` | `docs/ENDPOINTS.md`, `LoginResponse` | Prueba de contrato, token tratado como opaco |
| 400 con `Request is null`, `Username is required`, `Password is required` | `AuthResource` | Pruebas existentes |
| 401 con `Invalid user <username> or password` | `AuthResource` | Prueba existente `reportsInvalidCredentials` |
| JWT HS256 con `sub`, `role`, `iat`, `exp` = `iat` + 3600 s | `JwtTokenService` | `JwtTokenServiceTest` |
| Verificación rechaza alterados, otra clave y malformados | `JwtTokenService.isValid` | `JwtTokenServiceTest` |

## Componentes afectados

- `auth/src/main/java/com/bcp/workshop/auth/JwtTokenService.java`: solo si se decide corregir una brecha (ver Decisiones D2 y D3). Por defecto sin cambios.
- `auth/src/main/resources/application.properties`: sin cambios por defecto (ver D4).
- `auth/src/test/java/com/bcp/workshop/auth/JwtTokenServiceTest.java` y `AuthResourceTest.java`: pruebas de caracterización y de contrato que faltan.
- `openspec/changes/auth-token-firmado-login/`: spec y, si se decide, ajuste de escenarios.
- Documentos de referencia (`HU-01.md`, `HALLAZGOS.md`, `PLAN.md`): correcciones documentales, solo si se aprueban; el alcance de este cambio no las exige.
- `web/` y `api/`: no se tocan. Son consumidores del contrato, no parte del cambio.

## Decisiones técnicas y alternativas

**D1. No reimplementar la firma; verificar y especificar.**
- Elegida: mantener JWT HS256 de `JwtTokenService` y añadir pruebas.
- Alternativa: cambiar a RS256 o EdDSA con clave pública. Descartada: cambia el formato y exige coordinación con consumidores, que el Issue y la propuesta excluyen.

**D2. Clave ausente: qué ocurre hoy y qué se decide.**
- Hoy: `issue` lanza `IllegalStateException`. `AuthResource` lo captura y responde 500 con `message` y `exception`.
- La spec dice "falla de forma controlada". Eso es cierto solo en el sentido de que no se emite token; la respuesta expone el nombre de la clase, lo que es H-AUTH-05.
- Elegida: no cambiar el código. Añadir una prueba de caracterización que fije el 500 actual y registrar la brecha. Si se quiere un 503 sin detalle, debe decidirse en un cambio aparte junto con H-AUTH-05.
- Alternativa: validar la clave al arrancar y fallar el despliegue. Es más seguro, pero cambia el arranque del servicio; queda fuera de este cambio.

**D3. Clave corta y verificación con clave en blanco.**
- jjwt exige al menos 256 bits para HS256. `Keys.hmacShaKeyFor` con una clave más corta lanza `WeakKeyException` en `issue`, que no está cubierto por `IllegalStateException`. El resultado sería un 500 igual que D2.
- `isValid` llama a `signingKey()` fuera del `try` para la parte de la clave en blanco, así que con clave vacía lanza `IllegalStateException` en lugar de devolver `false`. El escenario "cadena malformada" de la spec no se ve afectado porque la clave sí está configurada en ese caso.
- Elegida: caracterizar ambos comportamientos en pruebas. Si se decide que `isValid` nunca lance, es un cambio pequeño en `JwtTokenService`, aprobado aparte.
- Requisito de longitud: documentar en el README o en la spec que `AUTH_TOKEN_KEY` debe tener al menos 32 caracteres ASCII. Esta es una restricción de configuración, no un cambio de código.

**D4. Literal de clave solo en `%test`.**
- Hoy: `%test.auth.token.key=test-only-signing-key-...` está aislado al perfil de pruebas de Quarkus. El perfil por defecto usa `AUTH_TOKEN_KEY`.
- Alternativa A (elegida): mantenerlo. Cumple el escenario de la spec, no requiere cambios y no hay riesgo de que el perfil por defecto lo use.
- Alternativa B: moverlo a `src/test/resources/application.properties`. Descartada: un archivo con el mismo nombre en test reemplaza la configuración principal completa, con riesgo de perder ajustes de `auth/` en las pruebas.

**D5. Cómo se verifica la firma (quién puede verificar).**
- HS256 es simétrico: verificar exige conocer la clave. El consumidor (`web/`) no puede verificar el token sin recibir `AUTH_TOKEN_KEY`, y compartirla rompería la propiedad que busca el Issue.
- Este cambio lo deja explícito: la verificación de firma se prueba dentro de `auth/`. Un consumidor externo no debe tratar el token como verificable sin un acuerdo de compartición de clave o un cambio a clave pública. Ese acuerdo es una decisión humana pendiente y debe registrarse antes de declarar el token "verificable" para terceros.

## Pruebas de caracterización antes de modificar comportamiento crítico

Antes de cualquier cambio en `JwtTokenService` o `AuthService`, ejecutar la línea base:

Ya existentes (verificar que siguen pasando):
- `JwtTokenServiceTest`: `issuesTokenThatCanBeValidated`, `issuedTokenCarriesSubjectRoleAndOneHourLifetime`, `rejectsTokenWithAlteredPayload`, `rejectsTokenSignedWithAnotherKey`, `rejectsMalformedToken`, `failsWhenSigningKeyIsBlank`.
- `AuthResourceTest`: `logsInWithWorkshopUser`, `reportsInvalidCredentials`, `rejectsNullRequest`, `rejectsBlankUsername`, `rejectsBlankPassword`, `returnsCurrentErrorDetailsForControlledFailure`.

Brechas a añadir (todas en `auth/src/test`):
1. **Contrato de login con tipos.** En `AuthResourceTest`, validar en el 200 que `token` es string no vacío, `expiresIn` es 3600, `user` es objeto con `username`, `displayName`, `role`, y `debug` es `authenticated-with-legacy-flow`. No comparar el valor de `token`.
2. **Firma verificable en el endpoint.** Extraer el token de la respuesta del login y comprobar con `JwtTokenService` (misma clave `%test`) que la firma es válida; luego alterar el payload y comprobar que se rechaza. Así se cubren los criterios 1 y 2 a través del contrato HTTP, no solo a nivel de servicio.
3. **Clave ausente en el endpoint (D2).** Fijar el 500 actual con `message` y `exception`, marcado como caracterización de una brecha conocida.
4. **Clave corta (D3).** Caracterizar que `issue` falla con clave menor a 256 bits. Debe confirmarse la excepción real antes de fijarla.
5. **Clave en blanco en `isValid` (D3).** Caracterizar la excepción actual.

Ejecución: `python tools/lab.py test auth` desde la raíz (las pruebas se ejecutan con el directorio `auth/`). La evidencia debe registrar el resultado real, sin tokens ni claves en la salida.

## Estrategia incremental

1. **Paso 0 (sin cambio de código):** ejecutar la línea base y registrar resultados.
2. **Paso 1:** añadir las brechas 1 y 2 (contrato y firma en el endpoint). Son pruebas nuevas, sin cambio de producción.
3. **Paso 2:** añadir las brechas 3, 4 y 5 como caracterización. Si alguna falla de forma inesperada, se corrige la spec, no el código, salvo que haya decisión aprobada.
4. **Paso 3:** medir cobertura de `JwtTokenService`, `AuthService` y `AuthResource` con JaCoCo y confirmar 85 % o más en el código modificado. Ajustar la spec si el escenario de clave ausente no coincide con el 500 real.
5. **Paso 4 (solo si se aprueba):** correcciones documentales en `HU-01.md` y `PLAN.md` y resolución de la contradicción H-AUTH-03 (Resuelto vs. Pendiente).
6. **Paso 5 (fuera de este cambio):** cualquier cambio de D2, D3 o D5 se propone como cambio OpenSpec independiente.

## Riesgos y trade-offs

- **Spec vs. comportamiento en clave ausente (D2):** el escenario "falla de forma controlada" no describe el 500 con detalle de excepción. Riesgo de que la prueba de aceptación se marque como cumplida sin serlo. Mitigación: documentar la brecha y decidir en la revisión humana.
- **Exposición de detalle en 500 (H-AUTH-05):** la caracterización fija un comportamiento inseguro. Trade-off aceptado para no cambiar el contrato de error en este cambio; se corrige en su propio cambio.
- **HS256 y verificación por terceros (D5):** el token es verificable solo por quien tiene la clave. Riesgo de falsa expectativa para `web/` u otros consumidores.
- **Clave corta (D3):** un despliegue con `AUTH_TOKEN_KEY` corta falla en el login. Mitigación: documentar la longitud mínima; la validación al arranque queda como trade-off a favor de no tocar el arranque.
- **Cobertura:** el código modificado puede tener poca superficie, por lo que el 85 % se mide sobre las clases tocadas, no sobre todo el módulo.
- **Duplicación de pruebas:** el endpoint y el servicio verifican la firma por separado. Se acepta por claridad de cada capa.

## Rollback

- Este diseño no cambia código de producción en el Paso 0 al Paso 3: el rollback es revertir las pruebas añadidas y la spec o el documento de diseño (`git revert` de los commits del cambio o eliminar los archivos nuevos).
- Si se aprueba el Paso 4 (documentos), revertir `HU-01.md`, `HALLAZGOS.md` y `PLAN.md` al estado previo.
- Si en el futuro se cambia `JwtTokenService` (D2 o D3), el rollback es restaurar el archivo anterior. No hay estado persistido: los tokens emitidos son stateless y dejan de ser válidos al expirar o al cambiar la clave. Un rollback de clave invalida los tokens activos, lo que obliga a los usuarios a volver a iniciar sesión; debe comunicarse antes de hacerlo.
- No hay migración de datos ni cambio de esquema de base de datos.

## Preguntas abiertas para revisión humana

1. ¿Se acepta que el 500 con `exception` sea el comportamiento de referencia de la clave ausente en este cambio, o se exige un fallo controlado sin detalle (D2)?
2. ¿Se fija una longitud mínima para `AUTH_TOKEN_KEY` en la documentación (D3)?
3. ¿Quién debe poder verificar la firma fuera de `auth/`? Si la respuesta es "nadie externo", el requisito "verificable" se limita al servicio (D5).
4. ¿Se aprueban las correcciones documentales de H-AUTH-03 (Paso 4)?
