# PLAN

## auth

### aprobacion humana
- Jackelyn
- 2026/10/26
- H-AUTH-01, H-AUTH-02, H-AUTH-03, H-AUTH-04, H-AUTH-05, H-AUTH-06, H-AUTH-07, H-AUTH-08, H-AUTH-09, H-AUTH-10, H-AUTH-11, H-AUTH-12

### Seleccion y prioridad

El registro de aprobacion de este plan incluye H-AUTH-01 a H-AUTH-12. En esta revision, H-AUTH-01, H-AUTH-02, H-AUTH-04, H-AUTH-06, H-AUTH-08, H-AUTH-10 y H-AUTH-12 se mantienen `Resuelto`; H-AUTH-03, H-AUTH-05, H-AUTH-07, H-AUTH-09 y H-AUTH-11 estan `Pendiente`. H-AUTH-03 requiere cerrar la coordinacion de compatibilidad y la seleccion/aprobacion de una dependencia concreta si se requiere. H-AUTH-05 sigue sin cambio de valores de error mientras falte decision de compatibilidad.

### Contrato y limites

- Contrato observado en docs/ENDPOINTS.md: POST /auth/login con JSON; respuestas 200, 400, 401 y 500 con las claves documentadas. No cambiar ruta, metodo, estados, claves ni campos de respuesta en este plan.
- Aprobacion humana registrada: Jackelyn, 2026/10/26, para H-AUTH-01 a H-AUTH-12. No consta una decision que apruebe cambios al formato HTTP, valores de error o compatibilidad de token; se mantienen sin cambios.
- Mantener `token` como string y `expiresIn` con el valor observado de 3600. El formato interno del token solo se cambiara tras caracterizar consumidores y coordinar compatibilidad; no leer ni modificar otros componentes como parte de este alcance.
- Mantener las claves `message` y `exception` del 500, pero proponer valores genericos que no expongan texto de excepcion ni nombre de clase. Requiere revisar la compatibilidad de valores, pues el documento actual describe el comportamiento observado.
- En el trabajo registrado se implementaron cambios para H-AUTH-02, H-AUTH-04, H-AUTH-09, H-AUTH-10 y H-AUTH-12; se ampliaron pruebas para H-AUTH-01, H-AUTH-06 y H-AUTH-08. La revision local reabrio H-AUTH-06 y H-AUTH-09; la caracterizacion adicional volvio a resolver H-AUTH-06. H-AUTH-09 permanece pendiente por la evidencia indicada en HALLAZGOS.md.

### Plan por hallazgo

#### H-AUTH-02 - SQL concatenado (alta)

- Hallazgo: `LegacyDatabase.findByCredentials` concatena username y password en la consulta.
- Problema: la entrada se interpreta como sintaxis SQL y puede alterar la validacion de credenciales.
- Cambio propuesto: sustituir `Statement` por `PreparedStatement`, con parametros para ambos valores; mantener el mismo resultado para credenciales validas/invalidas y no cambiar los status ni el cuerpo HTTP.
- Archivos previstos: `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java`; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` y/o nueva prueba de repositorio bajo `auth/src/test/java/com/bcp/workshop/auth/`.
- Caracterizacion previa: probar login valido y credenciales invalidas, incluyendo entradas con caracteres especiales como datos ordinarios; verificar que solo las credenciales legitimas obtienen 200 y que el rechazo conserva status 401 y el mensaje observado. No incluir payloads de explotacion.

#### H-AUTH-03 - Token sin firma verificable (alta)

- Hallazgo: token Base64 con secreto embebido, no firmado criptograficamente.
- Problema: Base64 no garantiza integridad; un consumidor no puede verificar criptograficamente que claims y expiracion fueron emitidos por auth.
- Cambio propuesto: emitir token firmado con expiracion y claims explicitos de usuario/rol; conservar el campo `token`, su tipo string, `expiresIn=3600` y el resto de la respuesta. Antes de cambiar el formato, coordinar compatibilidad con consumidores y obtener decision humana. No inspeccionar ni modificar otros componentes.
- Archivos previstos: `auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java` (o reemplazo con nombre de proposito), `auth/src/main/resources/application.properties`, configuracion de prueba bajo `auth/src/test/resources/` si se requiere y `auth/pom.xml` solo si la solucion seleccionada necesita una extension.
- Dependencia: preferir una implementacion JWT mantenida e integrada con Quarkus frente a criptografia propia; justificar y aprobar la dependencia concreta antes de agregarla. Usar secreto ficticio solo en pruebas y variable de entorno/configuracion externa en ejecucion.
- Caracterizacion previa: fijar status, claves y tipos del 200, token no nulo y `expiresIn=3600`; tratar el token como opaco en la caracterizacion para no convertir el formato inseguro actual en contrato.

#### H-AUTH-09 - Contrasenas en texto plano (alta)

- Hallazgo: el esquema y los usuarios de demostracion guardan contrasenas directamente y la consulta compara ese valor.
- Problema: una lectura de la base expone credenciales reutilizables.
- Cambio propuesto: almacenar hashes con salt mediante algoritmo lento, verificar con una funcion de comprobacion y migrar de forma idempotente los registros existentes. Mantener usuarios ficticios de laboratorio sin registrar contrasenas en logs; preservar login y respuesta.
- Archivos previstos: `auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java` (o repositorio separado), nueva clase de servicio/codificador bajo `auth/src/main/java/com/bcp/workshop/auth/`, `auth/src/main/resources/application.properties` si la configuracion lo requiere, y pruebas bajo `auth/src/test/java/com/bcp/workshop/auth/`.
- Dependencia: justificar la seleccion del algoritmo/libreria mantenida y el costo de verificacion antes de agregarla; no reutilizar el secreto del token como salt.
- Caracterizacion previa: probar el login de los dos usuarios ficticios existentes, rechazo de contrasena incorrecta y forma exacta de la respuesta exitosa; usar base temporal aislada para probar migracion sin tocar datos locales.

#### H-AUTH-05 - Detalles de excepcion en respuesta 500 (media)

- Hallazgo: el 500 devuelve `error.getMessage()` y `error.getClass().getName()`.
- Problema: texto interno y clase concreta pueden revelar implementacion.
- Cambio propuesto: registrar detalle solo en el servidor y devolver valores genericos; mantener status 500 y las claves `message` y `exception` observadas hasta que se apruebe un cambio de contrato.
- Archivos previstos: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java` y, si la extraccion de capas ya fue aprobada, el manejador HTTP correspondiente; prueba en `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.
- Caracterizacion previa: provocar una excepcion controlada mediante un doble de `LegacyDatabase` en una prueba unitaria del mismo paquete; verificar status 500, claves y valores actuales en la baseline. La prueba posterior debe verificar que los valores no contienen mensaje/clase internos.

#### H-AUTH-10 - CORS acepta cualquier origen (media)

- Hallazgo: `quarkus.http.cors.origins=/.*/` permite cualquier origen.
- Problema: origenes no previstos pueden realizar solicitudes desde navegador; el impacto depende de credenciales y controles adicionales.
- Cambio aplicado: `quarkus.http.cors.origins` usa `AUTH_CORS_ORIGINS` con default `http://localhost:4200`; se conservaron metodos y headers CORS existentes. No se cambiaron rutas ni payloads HTTP.
- Archivos: `auth/src/main/resources/application.properties` y `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.
- Caracterizacion previa: dos pruebas de preflight confirmaron que el origen local y uno no configurado eran reflejados antes del cambio.
- Verificacion posterior: pruebas enfocadas confirmaron allow del origen local, 403 sin `Access-Control-Allow-Origin` para origen arbitrario, y allow de `https://frontend.example` al ejecutar con `AUTH_CORS_ORIGINS=https://frontend.example`. La regresion final `mvn verify` paso con 17 tests, 0 fallos y 0 errores.
- Resultado: H-AUTH-10 queda `Resuelto` para el default local y la configuracion externalizada. En despliegues no locales, el operador debe establecer una lista explicita; no se ejecutaron controles remotos.

#### H-AUTH-11 - Versiones a investigar con SCA (media)

- Hallazgo: pom declara Quarkus 3.27.4 y Commons Text 1.9, versiones indicadas por RETO.md para investigacion.
- Problema: sin analisis no se sabe si el grafo resuelto contiene advisories aplicables; actualizar a ciegas puede introducir regresiones.
- Cambio propuesto: ejecutar el control SCA/Dependency Review aprobado, guardar identificadores y versiones afectadas, y actualizar/remover solo dependencias con evidencia. No silenciar alertas ni agregar una dependencia scanner al producto sin justificacion.
- Archivos previstos: primero `auth/pom.xml` solo si el informe justifica el cambio; pruebas de auth existentes para regresion.
- Caracterizacion previa: ejecutar `mvn test` desde `auth/` y registrar resultado como baseline antes de modificar versiones; ejecutar el analisis de dependencias antes y despues. No hay prueba funcional nueva especifica para un advisory.

#### H-AUTH-12 - Responsabilidades mezcladas en AuthResource (media)

- Hallazgo: `login` valida, consulta persistencia, emite token y crea la respuesta HTTP.
- Problema: cambios de negocio, base de datos o serializacion quedan acoplados y son mas dificiles de probar de forma aislada.
- Cambio propuesto: separar Resource (HTTP), Service (autenticacion/reglas) y Repository (persistencia); usar DTO de salida propio en vez de exponer el registro. Extraer responsabilidades en cambios revisables, sin duplicar validaciones ni anidar condiciones.
- Archivos previstos: `AuthResource.java`, `LegacyDatabase.java` (o repositorio con nombre de proposito), `UserRecord.java`, nuevos archivos de servicio/DTO/repositorio bajo `auth/src/main/java/com/bcp/workshop/auth/`, y pruebas bajo `auth/src/test/java/com/bcp/workshop/auth/`.
- Caracterizacion previa: cubrir 200, cada validacion 400, 401 y 500 con status, mensajes/claves y campos de respuesta observados antes de extraer clases.

#### H-AUTH-01 - Preservacion de POST /auth/login (baja)

- Hallazgo: la ruta observada forma parte del contrato y debe conservarse.
- Problema: cambiar metodo o ruta rompe clientes existentes.
- Cambio propuesto: no cambiar `@Path("/auth")`, `@POST` ni `@Path("/login")`; asegurar que las extracciones se mantengan detras del mismo endpoint.
- Archivos previstos: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java` y `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`.
- Caracterizacion previa: verificar que POST `/auth/login` responde con el status/payload observado y que no se sustituye por otra ruta.

#### H-AUTH-04 - Campo debug y expiracion literal (baja)

- Hallazgo: respuesta incluye `debug` y `expiresIn` usa 3600 literal.
- Problema: valor duplicado puede divergir del token y `debug` puede revelar flujo interno; quitar/cambiar campos sin acuerdo puede romper clientes.
- Cambio propuesto: definir una constante nombrada unica para expiracion y usarla tanto al emitir token como al responder; mantener valor 3600 y campo `debug` mientras el contrato no apruebe su retiro. No cambiar el cuerpo observado en esta etapa.
- Archivos previstos: `auth/src/main/java/com/bcp/workshop/auth/AuthResource.java`, `auth/src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java` y prueba `AuthResourceTest.java`.
- Caracterizacion previa: afirmar las claves exactas de respuesta exitosa, `expiresIn=3600`, contenido actual de `debug`, y tipo string/no nulo del token.

#### H-AUTH-06 - Casos de caracterizacion faltantes (baja)

- Hallazgo: la prueba visible cubre login exitoso, no validaciones ni rechazos.
- Problema: refactorizar sin baseline de errores puede cambiar accidentalmente respuestas observables.
- Cambio propuesto: ampliar pruebas de caracterizacion antes de cambiar implementacion; mantenerlas organizadas por escenario y reutilizar helpers solo si eliminan duplicacion real.
- Archivos previstos: `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java` y, si se requiere aislamiento de persistencia, pruebas de repositorio en el mismo arbol.
- Caracterizacion previa: agregar request nulo, username nulo/vacio, password nulo/vacio, credenciales invalidas y excepcion controlada; verificar status y contenido segun docs/ENDPOINTS.md.

#### H-AUTH-07 - Riesgo de incompatibilidad de campos/token (baja)

- Hallazgo: retirar campos o cambiar el formato interno del token puede afectar consumidores.
- Problema: el contrato observado no define consumidor ni formato interno del token; asumir que es opaco o que se parsea puede causar regresion.
- Cambio propuesto: mantener todas las claves/tipos de respuesta observados; no cambiar token firmado/semantica de campos hasta tener decision humana y coordinacion con consumidores. Este ID comparte pruebas y archivos con H-AUTH-01, H-AUTH-03 y H-AUTH-04; no crear una segunda implementacion.
- Archivos previstos: `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`; `AuthResource.java` y `LegacyTokenUtil.java` solo tras aprobacion.
- Caracterizacion previa: comparar conjunto de claves, tipos, status y valores contractuales documentados; token se comprueba como string no nulo sin fijar su codificacion.

#### H-AUTH-08 - Cobertura no medida (baja)

- Hallazgo: no hay evidencia de porcentaje JaCoCo ni de cobertura del codigo modificado.
- Problema: no puede verificarse la meta de 85% para codigo modificado sin reporte y metodo reproducible.
- Cambio propuesto: ejecutar JaCoCo con `mvn verify`, guardar el reporte real y calcular cobertura de las lineas/archivos modificados con numerador, denominador y metodo; no cambiar umbrales para aprobar.
- Archivos previstos: pruebas bajo `auth/src/test/java/com/bcp/workshop/auth/`; no cambiar configuracion JaCoCo salvo que una falla reproducible lo requiera.
- Caracterizacion previa: ejecutar y registrar la suite existente como baseline antes de tocar codigo; despues, correr toda la suite y revisar `auth/target/site/jacoco/index.html`.

### Orden de ejecucion propuesto

1. Escribir primero las caracterizaciones de H-AUTH-01, H-AUTH-04, H-AUTH-06 y H-AUTH-07; ejecutar baseline y registrar resultado real.
2. Tratar H-AUTH-02 y H-AUTH-09 en persistencia. Mantener pruebas aisladas de SQL, hash y migracion; separar commits/cambios revisables.
3. Resolver H-AUTH-03 junto con coordinacion de compatibilidad de token y configuracion externa; no cambiar formato hasta decision humana.
4. Resolver H-AUTH-10 en configuracion, con origenes autorizados confirmados por entorno.
5. Extraer capas para H-AUTH-12 y sanear errores de H-AUTH-05, respaldado por pruebas previas.
6. Investigar H-AUTH-11 con SCA y cambiar dependencias solo con evidencia; ejecutar regresion.
7. Medir H-AUTH-08 con JaCoCo y verificar la meta de cobertura para el codigo modificado.

### Dependencias, decisiones y evidencia pendiente

- Aprobacion humana pendiente para cualquier cambio observable en formato/semantica del token, valores de error o campo `debug`.
- Coordinacion pendiente con consumidores externos para validar migracion de tokens; no implica leer o modificar otros componentes.
- El default CORS corresponde al origen local documentado (`http://localhost:4200`); cada despliegue no local debe configurar y validar `AUTH_CORS_ORIGINS` con su allowlist real. No se probaron despliegues remotos.
- H-AUTH-09 usa `PBKDF2WithHmacSHA256` del JDK y no agrega dependencia. Para H-AUTH-03 sigue pendiente la decision sobre la libreria JWT y coordinacion de compatibilidad; no se agrego paquete.
- Herramienta/informe SCA y controles remotos pendientes. Para H-AUTH-02 se ejecuto `mvn verify` y se midio cobertura del metodo modificado; no se extrapola ese porcentaje al resto del componente.
- La cobertura del codigo modificado por futuros hallazgos debe medirse por separado con reporte y calculo reproducibles; objetivo >=85%.

### Registro de ejecuciones

#### 2026-10-26 - Componente auth - H-AUTH-02

- Caracterizacion antes del cambio: desde `auth/`, con `AUTH_DB_URL=jdbc:sqlite:target/h-auth-02-characterization.db`, se ejecuto `mvn -Dtest=AuthResourceTest test`. Resultado: 2 tests, 1 fallo. El login valido con username ficticio que contiene apostrofo obtuvo 401; SQLite reporto error de sintaxis en el SQL concatenado. El test previo de usuario `student` paso.
- Cambio aplicado entre ejecuciones: `LegacyDatabase.findByCredentials` ahora usa `PreparedStatement` y vincula username/password mediante parametros. No se cambio el endpoint ni la respuesta.
- Caracterizacion despues del cambio: misma orden y base aislada. Resultado: 2 tests, 0 fallos, 0 errores.
- Caracterizacion ampliada: se agrego rechazo de contrasena incorrecta para el mismo usuario ficticio; repeticion enfocada resulto en 2 tests, 0 fallos, 0 errores.
- Repeticion despues del ajuste de formato Java: `mvn -Dtest=AuthResourceTest test`, misma base aislada. Resultado: 2 tests, 0 fallos, 0 errores.
- Intento inicial de gate: `mvn verify` y la generacion separada del reporte fueron omitidos; no se registraron como ejecutados en ese momento.
- Verificacion posterior: desde `auth/`, `AUTH_DB_URL=jdbc:sqlite:target/h-auth-02-characterization.db mvn verify`. Resultado: BUILD SUCCESS; 2 tests, 0 fallos, 0 errores; se genero `auth/target/site/jacoco/index.html` y `jacoco.xml`.
- Cobertura del metodo modificado: JaCoCo `LegacyDatabase.findByCredentials`, 12 lineas cubiertas de 13, 1 no cubierta: 92,31% (12/13). Supera el minimo del 85% para este metodo. Con esta evidencia H-AUTH-02 queda `Resuelto`.
- Controles remotos: CodeQL y Dependency Review no se ejecutaron en este recorrido local.

#### 2026-10-26 - Componente auth - H-AUTH-10

- Caracterizacion previa: dos pruebas OPTIONS confirmaron que, antes del cambio, tanto `http://localhost:4200` como `https://unconfigured.invalid` recibian `Access-Control-Allow-Origin`.
- Cambio aplicado: `application.properties` limita el default a `http://localhost:4200` y permite definir la lista de despliegue mediante `AUTH_CORS_ORIGINS`.
- Verificacion posterior enfocada: 3 tests, 0 fallos y 0 errores para origen local permitido, origen arbitrario rechazado (403, sin allow-origin) y default.
- Override: con `AUTH_CORS_ORIGINS=https://frontend.example`, el caso configurado paso (1 test, 0 fallos); se removio la variable despues de la ejecucion.
- Regresion final: `mvn verify`, BUILD SUCCESS, 17 tests, 0 fallos y 0 errores. El default local es el unico origen probado dentro de la suite completa; no se ejecutaron despliegues remotos.
- Controles remotos: CodeQL y Dependency Review no se ejecutaron en este recorrido local.

#### H-AUTH-03 - Componente auth

- Sin ejecuciones ni cambios en esta iteracion. La aprobacion del ID consta en el encabezado, pero sigue pendiente la coordinacion de compatibilidad de token y la seleccion/aprobacion de la dependencia; no se agrego dependencia ni se cambio el token.

#### 2026-10-26 - Componente auth - H-AUTH-04

- Caracterizacion antes del cambio: desde `auth/`, con la base SQLite aislada, `mvn -Dtest=AuthResourceTest test` resulto en 4 tests, 0 fallos, 0 errores. Se fijaron `expiresIn=3600`, `debug`, username, displayName y role de la respuesta existente.
- Cambio aplicado: `LegacyTokenUtil.EXPIRATION_SECONDS` es ahora la constante compartida por la expiracion del token y el campo `expiresIn`; se preservaron su valor y los campos HTTP observados.
- Caracterizacion despues del cambio: misma orden y base aislada; 4 tests, 0 fallos, 0 errores.
- Regresion y cobertura: `mvn verify` resulto BUILD SUCCESS con 7 tests, 0 fallos, 0 errores. Las dos lineas ejecutables modificadas (emision de token y respuesta) estan cubiertas; el metodo `LegacyTokenUtil.create` presenta 3/3 lineas (100%).
- Controles remotos: CodeQL y Dependency Review no se ejecutaron en este recorrido local.

#### 2026-10-26 - Componente auth - H-AUTH-01, H-AUTH-06, H-AUTH-08

- Caracterizacion antes de cualquier refactor de `AuthResource`: `mvn -Dtest=AuthResourceTest test`, desde `auth/` con base SQLite aislada, paso con 9 tests, 0 fallos y 0 errores tras reparar un import faltante en la prueba.
- Se cubrieron request JSON nulo, username en blanco, password en blanco, credenciales invalidas (401) y una excepcion controlada que fija el 500 actual; se confirmo que los logins siguen en POST `/auth/login`.
- Regresion posterior: `mvn verify` resulto BUILD SUCCESS con 12 tests, 0 fallos y 0 errores.
- JaCoCo: `AuthResource` 25/25 lineas (100%), `LegacyDatabase` 56/59 (94,92%), `PasswordHasher` 28/30 (93,33%) y `LegacyTokenUtil` 3/3 (100%). Reporte en `auth/target/site/jacoco/index.html`; se supera 85% en las clases afectadas.
- Controles remotos: CodeQL y Dependency Review no se ejecutaron en este recorrido local.

#### 2026-10-26 - Componente auth - H-AUTH-12

- Caracterizacion previa a la extraccion: `mvn -Dtest=AuthResourceTest test` habia pasado con 9 tests y `mvn verify` con 12 tests; esos escenarios fijan los codigos HTTP, mensajes, campos, usuario y token observados.
- Cambio aplicado: `AuthResource` conserva validacion/mapeo HTTP; `AuthService` coordina persistencia, token y resultado de autenticacion; `LoginResponse` y su DTO `User` representan la respuesta JSON. `LegacyDatabase` sigue siendo la capa de persistencia. No se cambiaron ruta, status ni campos.
- Prueba posterior enfocada: `mvn -Dtest=AuthResourceTest,AuthServiceTest test`, 11 tests, 0 fallos y 0 errores.
- Regresion y cobertura: `mvn verify`, BUILD SUCCESS, 17 tests, 0 fallos y 0 errores. JaCoCo: `AuthResource` 17/17 (100%), `AuthService` 12/12 (100%), `LoginResponse` 1/1 (100%) y `LoginResponse.User` 1/1 (100%).
- Controles remotos: CodeQL y Dependency Review no se ejecutaron en este recorrido local.

#### 2026-10-26 - Componente auth - H-AUTH-09

- Caracterizacion antes del cambio: con la base SQLite aislada y antes de cambiar el almacenamiento, `mvn -Dtest=AuthResourceTest test` resulto en 3 tests, 0 fallos, 0 errores. Se verificaron los logins existentes de `student` y `trainer`, ademas de la caracterizacion SQL de H-AUTH-02.
- Cambio aplicado: `PasswordHasher` usa PBKDF2WithHmacSHA256 del JDK con salt aleatorio; `LegacyDatabase` almacena hashes para nuevos usuarios, migra filas existentes una sola vez y verifica la contrasena contra el hash. No se agrego dependencia.
- Pruebas de correccion: `PasswordHasherTest` comprueba salts distintos, password correcto/incorrecto y hashes invalidos; `LegacyDatabaseTest` comprueba migracion e idempotencia; `AuthResourceTest` comprueba login de ambos usuarios, siembra de un usuario nuevo hasheado y rechazo de password incorrecto. Conjunto enfocado: 7 tests, 0 fallos, 0 errores.
- Regresion y cobertura: desde `auth/`, `mvn verify` resulto BUILD SUCCESS con 7 tests, 0 fallos, 0 errores. JaCoCo: `LegacyDatabase` 56/59 lineas = 94,92%; `PasswordHasher` 28/30 = 93,33%. Ambos superan el 85% para las clases modificadas.
- Fallos de prueba durante el desarrollo: una URL SQLite mal formada impidio compilar `LegacyDatabaseTest`; al corregirla, el driver no estaba registrado en el test unitario. Se cargo el driver explicitamente; la siguiente ejecucion enfocada paso. No quedan fallos conocidos de esos casos.
- Controles remotos: CodeQL y Dependency Review no se ejecutaron en este recorrido local.

#### 2026-10-26 - Componente auth - H-AUTH-06 (ampliacion de contrato)

- Baseline antes de ampliar pruebas: desde `auth/`, con `AUTH_DB_URL=jdbc:sqlite:target/h-auth-02-characterization.db` y `AUTH_CORS_ORIGINS` sin definir, `mvn verify` termino BUILD SUCCESS con 17 tests, 0 fallos, 0 errores y 0 omitidos.
- Casos agregados a `AuthResourceTest`: health feliz sin autenticacion; `username:null`; `password:null`; JSON malformado; Content-Type no soportado; y metodo GET no soportado en `/auth/login`.
- Prueba enfocada: `mvn -Dtest=AuthResourceTest test`, codigo 0; 18 tests, 0 fallos, 0 errores y 0 omitidos.
- Regresion: `mvn verify`, codigo 0; BUILD SUCCESS; 23 tests, 0 fallos, 0 errores y 0 omitidos.
- JaCoCo en `auth/target/site/jacoco/jacoco.xml`, contando lineas cubiertas / total por clase: AuthResource 17/17 (100%); AuthService 12/12 (100%); LoginResponse 1/1 (100%); LoginResponse.User 1/1 (100%); LegacyDatabase 56/59 (94,92%); PasswordHasher 28/30 (93,33%); LegacyTokenUtil 3/3 (100%).
- Health DOWN/503 queda pendiente: no existe un check de auth configurable para inducir el estado y el contrato/cuerpo siguen como pregunta abierta en `docs/ENDPOINTS.md`. No se agrego un check ficticio.
- CodeQL y Dependency Review no se ejecutaron localmente.
