# HALLAZGOS

## auth

| id | componente | hallazgo | tipo | explicacion | evidencia (archivo:linea) | severidad | estado |
|---|---|---|---|---|---|---|---|
| H-AUTH-02 | auth | La consulta de credenciales concatenaba username y password; resuelto con `PreparedStatement` parametrizado. | Vulnerabilidad | La concatenacion podia alterar la consulta SQL; los valores ahora se enlazan como datos y no como sintaxis. JaCoCo cubre 12/13 lineas del metodo (92,31%). | auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:85-98; auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:65 | alta | Resuelto |
| H-AUTH-03 | auth | El token es Base64 del contenido y agrega un secreto constante, sin firma criptografica. | Vulnerabilidad | Base64 solo codifica el contenido; no autentica su integridad y el secreto incluido en el codigo no proporciona firma verificable. | auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:8, auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:14-15 (original); resolucion: auth/src/main/java/com/bcp/workshop/auth/JwtTokenService.java (JWT HS256, clave desde AUTH_TOKEN_KEY), auth/src/main/java/com/bcp/workshop/auth/AuthService.java, auth/src/test/java/com/bcp/workshop/auth/JwtTokenServiceTest.java; `python tools/lab.py test auth` BUILD SUCCESS, 29 pruebas, 0 fallos | alta | Resuelto |
| H-AUTH-09 | auth | El almacenamiento y verificacion usan PBKDF2, pero la migracion no identifica de forma segura todos los hashes existentes. | Vulnerabilidad | `isEncodedHash` solo comprueba el prefijo. Una contraseña antigua en texto plano que empiece por `pbkdf2-sha256$` se omite en la migracion y permanece sin hash; la prueba cubre un valor ordinario, no esa colision. | auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:63-70; auth/src/main/java/com/bcp/workshop/auth/PasswordHasher.java:57-58; auth/src/test/java/com/bcp/workshop/auth/LegacyDatabaseTest.java:17-29 | alta | Pendiente |
| H-AUTH-05 | auth | La respuesta 500 expone el mensaje y la clase de la excepcion. | Vulnerabilidad | Los detalles internos pueden revelar informacion de implementacion al cliente y facilitar reconocimiento del servicio. | auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:55-57 | media | Pendiente |
| H-AUTH-10 | auth | CORS ahora permite por defecto solo `http://localhost:4200`; `AUTH_CORS_ORIGINS` permite declarar los origenes del despliegue. | Vulnerabilidad | Se rechazo el origen no configurado con 403. Las pruebas confirmaron el origen local, el rechazo de otro origen y la sustitucion del default mediante variable de entorno. Cada despliegue no local debe definir su lista permitida. | auth/src/main/resources/application.properties:4-7; auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:167,177,188 | media | Resuelto |
| H-AUTH-11 | auth | Quarkus 3.27.4 y Commons Text 1.9 coinciden con las versiones del reto que requieren investigacion SCA. | Sugerencia | Las versiones estan declaradas, pero sin ejecutar un analisis de dependencias no se afirma que tengan un CVE aplicable ni que el artefacto resuelto sea vulnerable. | auth/pom.xml:14, auth/pom.xml:49-51 | media | Pendiente |
| H-AUTH-12 | auth | Se separaron responsabilidades: Resource valida y mapea HTTP, AuthService autentica y crea el resultado, y LoginResponse define DTOs. | Refactor | La separacion reduce el acoplamiento y permite probar reglas sin mezclar persistencia, token y serializacion. `mvn verify` paso con 17 tests; JaCoCo: AuthResource 17/17 (100%), AuthService 12/12 (100%), LoginResponse 1/1 (100%). | auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:23-58; auth/src/main/java/com/bcp/workshop/auth/AuthService.java:10-33; auth/src/main/java/com/bcp/workshop/auth/LoginResponse.java:3-4; auth/src/test/java/com/bcp/workshop/auth/AuthServiceTest.java:12-36 | media | Resuelto |
| H-AUTH-01 | auth | El endpoint observado es POST /auth/login; se preservo y se verifico en pruebas de login. | Sugerencia | Cambiar método o ruta rompería clientes existentes; los escenarios de login siguen usando el mismo endpoint. | auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:15, auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:22-23; auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:22-35 | baja | Resuelto |
| H-AUTH-04 | auth | El valor de expiracion estaba duplicado; resuelto con `LegacyTokenUtil.EXPIRATION_SECONDS` compartida entre token y respuesta. Se conservaron `expiresIn=3600` y `debug`. | Refactor | La constante evita que la duracion informada diverja de la usada al crear el token, sin alterar campos ni valores observables del contrato. Caracterizacion previa/posterior y `mvn verify` pasaron; ambas lineas ejecutables modificadas estan cubiertas. | auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:9, auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java:14; auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:42; auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:32-33 | baja | Resuelto |
| H-AUTH-06 | auth | La caracterizacion cubre request nulo, username/password nulos y en blanco, JSON malformado, 401, errores de protocolo y 500 controlado. | Refactor | Se ampliaron las pruebas del endpoint; `AuthResourceTest` pasa con 18 tests y cubre las ramas de validacion 400, 401 y la respuesta 500 controlada. | auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:24-45; auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:112-229 | baja | Resuelto |
| H-AUTH-07 | auth | El contrato observado incluye campos de respuesta cuya eliminacion podria ser incompatible. | Sugerencia | Quitar debug, cambiar token o alterar campos antes de confirmar el contrato puede romper consumidores; el documento existente registra el contrato observado, no una aprobacion de cambio. | auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:39-49; docs/ENDPOINTS.md | baja | Pendiente |
| H-AUTH-08 | auth | La cobertura de validaciones y errores se midio con JaCoCo despues de ampliar la caracterizacion. | Sugerencia | En la verificacion final, JaCoCo informa AuthResource 17/17 lineas (100%), AuthService 12/12 (100%), LegacyDatabase 56/59 (94,92%), PasswordHasher 28/30 (93,33%) y LegacyTokenUtil 3/3 (100%). | auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:22-195; auth/target/site/jacoco/index.html | baja | Resuelto |
| H-AUTH-13 | auth | La inicializacion siembra usuarios con contrasenas fijas cada vez que faltan. | Candidato | Si se despliega una base nueva sin cambiar este comportamiento, las cuentas predecibles podrian permitir acceso no autorizado. No se confirma que el perfil productivo las use; requiere revision humana. | auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:28-29,36-50 | alta | CANDIDATO |
| H-AUTH-14 | auth | Los errores al consultar la base de datos se convierten en ausencia de usuario. | Candidato | Una falla de persistencia puede traducirse a una respuesta de credenciales invalidas, ocultando una indisponibilidad a clientes de login. | auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:86-98; auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:34-38 | media | CANDIDATO |
| H-AUTH-15 | auth | Los errores de inicializacion de la base se imprimen y se absorben. | Candidato | La aplicacion puede continuar tras fallar la preparacion de la persistencia; el efecto en el arranque y en health no se ha verificado. | auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:21-33 | media | CANDIDATO |

### Candidatos para revision humana

#### H-AUTH-13 — Siembra de usuarios con contrasenas fijas
- Estado: CANDIDATO
- Evidencia: `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:28-29,36-50` — la inicializacion solicita la siembra de dos cuentas con valores de contrasena definidos en codigo; no se reproducen esos valores aqui.
- Impacto: una base inicializada sin usuarios podria recibir cuentas con credenciales predecibles, si esta ruta se ejecuta en un despliegue no aislado.
- Prioridad propuesta: alta.
- Contrato potencialmente afectado: acceso mediante `POST /auth/login` y aprovisionamiento inicial de usuarios.
- Como validarlo: probar el arranque con una base vacia bajo el perfil destinado a despliegue y comprobar si esas cuentas quedan habilitadas; confirmar la politica de aprovisionamiento esperada.

#### H-AUTH-14 — Error de consulta indistinguible de credenciales invalidas
- Estado: CANDIDATO
- Evidencia: `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:86-98` — la excepcion de consulta se captura y el metodo devuelve `null`; `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:34-38` convierte un resultado vacio en HTTP 401.
- Impacto: ante una falla de persistencia el consumidor puede recibir un rechazo de autenticacion en lugar de una indicacion de indisponibilidad.
- Prioridad propuesta: media.
- Contrato potencialmente afectado: semantica de respuestas de `POST /auth/login` para credenciales incorrectas frente a fallas internas.
- Como validarlo: provocar una falla de acceso a la base durante el login y verificar el estado HTTP y cuerpo recibidos; compararlos con el caso de credenciales invalidas.

#### H-AUTH-15 — Fallo de inicializacion de persistencia absorbido
- Estado: CANDIDATO
- Evidencia: `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:21-33` — el bloque de inicializacion captura la excepcion e imprime su traza sin propagar el fallo.
- Impacto: el proceso puede seguir tras una inicializacion incompleta; no se ha comprobado si el arranque o el indicador de salud reflejan el problema.
- Prioridad propuesta: media.
- Contrato potencialmente afectado: disponibilidad de autenticacion despues del arranque y significado operativo del indicador de salud.
- Como validarlo: iniciar con una configuracion de base que falle controladamente y observar el resultado del arranque, health y una solicitud a `POST /auth/login`.

### Limites de este discovery

- Se leyo el contrato observado en docs/ENDPOINTS.md y no se modifico.
- No se ejecutaron pruebas, SAST ni analisis SCA; no se afirma CVE confirmado ni porcentaje de cobertura.
- H-AUTH-01 a H-AUTH-08 conservan sus IDs previos; los hallazgos nuevos usan H-AUTH-09 a H-AUTH-12.
- No se editaron archivos de codigo, configuracion ni pruebas en auth.

## Evidencia de implementacion: auth-token-firmado-login (Issue #24, HU-01)

Comandos ejecutados desde la raiz de ai-labs:

| Comando | Resultado |
| --- | --- |
| `python tools/lab.py test auth` (con `AUTH_DB_URL=jdbc:sqlite:target/h-auth-02-characterization.db`) | 36 pruebas, 0 fallos, 0 errores, BUILD SUCCESS, exit 0 |
| `openspec validate auth-token-firmado-login --strict` | Change valid, exit 0 |

Cobertura JaCoCo de las clases tocadas (`auth/target/site/jacoco/jacoco.xml`): `AuthResource` 18/18 lineas (100%), `AuthService` 13/13 (100%), `JwtTokenService` 19/19 (100%). Meta de 85% cumplida.

| ID | Estado | Evidencia y pruebas que lo cubren |
| --- | --- | --- |
| H-AUTH-03 | Resuelto (se mantiene) | El token es JWT HS256 firmado con `AUTH_TOKEN_KEY` (`JwtTokenService.java`). `JwtTokenServiceTest`: emision valida, payload alterado, otra clave, token malformado. `AuthResourceTest.verifiesSignatureOfTokenIssuedByLogin`: el token del login se acepta y un payload alterado se rechaza. Verificacion limitada a `auth/` (D5). |
| H-AUTH-07 | Pendiente (parcial) | Contrato 200 preservado: `AuthResourceTest.keepsLoginResponseFieldTypes`. El campo `exception` del 500 se retiro por D2; el campo `debug` sigue pendiente de decision humana. |
| H-AUTH-05 | Pendiente de confirmacion humana | Decision D2 aplicada: el 500 responde `{"message":"Internal authentication error"}` sin `exception`; el detalle se registra en el log. Pruebas: `AuthResourceTest.returnsGenericErrorWithoutExceptionDetailsForControlledFailure` y `returnsGenericErrorWhenSigningKeyIsMissing`. |

Decisiones registradas: D2 aprobada (ocultar detalle de excepcion); D3 sin longitud minima de `AUTH_TOKEN_KEY` (no implementado, tareas 3.1 y 5.3 omitidas); la parte de D3 sobre `isValid` con clave en blanco no fue aprobada (tarea 3.2 pendiente); D5 aprobada (verificacion solo en `auth/`); H-AUTH-03 se mantiene `Resuelto`.

No se incluyeron tokens, valores de `AUTH_TOKEN_KEY` ni la clave literal de `%test`. No hay exclusiones de JaCoCo, `@SuppressWarnings` ni cambios en CodeQL o Dependency Review.

