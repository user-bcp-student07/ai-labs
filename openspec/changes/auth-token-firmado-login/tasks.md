# Tasks

Cambio: `auth-token-firmado-login`. Issue: GitHub [#24](https://github.com/user-bcp-student07/ai-labs/issues/24) (HU-01). Componente: `auth/`.

Este checklist implementa `proposal.md`, `specs/auth-login-token/spec.md` y `design.md`. Las pruebas se ejecutan con `python tools/lab.py test auth` desde la raíz de ai-labs.

Regla general: no modificar `auth/src/main` hasta completar la fase 0 y aprobar las decisiones D2, D3 y D5. Ninguna tarea toca `api/`, `web/`, `PasswordHasher`, `LegacyDatabase` ni CORS.

## Fase 0. Línea base y decisiones

- [x] **0.1 Ejecutar la línea base de `auth/` sin cambios.**
  - Comando: `python tools/lab.py test auth`.
  - Hecho cuando: el resultado real (éxito o fallos) queda registrado. Si hay fallos previos, se documentan y no se corrigen en este cambio.
- [x] **0.2 Confirmar qué criterios del Issue cubren las pruebas existentes.**
  - Revisar `JwtTokenServiceTest` (`issuesTokenThatCanBeValidated`, `rejectsTokenWithAlteredPayload`, `rejectsTokenSignedWithAnotherKey`, `rejectsMalformedToken`, `failsWhenSigningKeyIsBlank`).
  - Hecho cuando: se anota qué criterio cubre cada prueba.
- [x] **0.3 Obtener decisiones humanas antes de cualquier cambio de producción.**
  - Respuestas registradas: D2 aprobada (el 500 no debe exponer detalle); D3 sin longitud mínima; D4 sin cambio; D5 aprobada (verificación limitada a `auth/`).
  - Hecho cuando: las respuestas quedan registradas. Sin respuesta, solo se ejecutan las fases 1, 2, 4 y 6 (pruebas y evidencia, sin cambio de producción).

## Fase 1. Pruebas de caracterización del contrato (sin cambio de producción)

- [x] **1.1 Agregar prueba de contrato del 200 en `AuthResourceTest`.**
  - Verifica: `token` string no vacío, `expiresIn` igual a 3600, `user` objeto con `username`, `displayName` y `role`, `debug` igual a `authenticated-with-legacy-flow`.
  - No compara el valor de `token`.
  - Cubre: requisito "Contrato de respuesta de login preservado" y criterio 3 del Issue.
- [x] **1.2 Agregar prueba de firma a través del endpoint.**
  - Extrae el `token` de la respuesta del login, lo verifica con la clave del perfil `%test` y comprueba que es aceptado.
  - Altera el payload del mismo token y comprueba que es rechazado.
  - Cubre: criterios 1 y 2 del Issue, a nivel HTTP.
- [x] **1.3 Agregar prueba de expiración en el endpoint.**
  - Verifica que `exp - iat` es 3600 segundos y que `expiresIn` coincide.
  - Cubre: escenario "Expiración de una hora".
- [x] **1.4 Verificar que las pruebas nuevas pasan sin cambiar `auth/src/main`.**
  - Comando: `python tools/lab.py test auth`.
  - Hecho cuando: pasan, o se documenta la brecha y se detiene la tarea.
- [x] **1.5 Verificar que la salida no expone tokens ni claves.**
  - Revisar la salida y los reportes en `auth/target/surefire-reports`.
  - Hecho cuando: no aparece ningún token, valor de `AUTH_TOKEN_KEY` ni la clave literal de `%test` en la evidencia.

## Fase 2. Pruebas de caracterización de comportamientos de borde

Estas pruebas fijan el comportamiento actual. No corrigen nada.

- [x] **2.1 Caracterizar clave ausente en el endpoint (D2).**
  - Prueba con clave vacía en la configuración del servicio.
  - Fija: status 500 con `message` y `exception` = `java.lang.IllegalStateException`.
  - Marcar la prueba como caracterización de la brecha H-AUTH-05.
- [x] **2.2 Caracterizar clave corta (D3).**
  - Prueba `issue` con una clave de menos de 256 bits.
  - Confirmar primero la excepción real antes de fijarla; no asumir el tipo.
- [x] **2.3 Caracterizar `isValid` con clave en blanco (D3).**
  - Fija que lanza `IllegalStateException` y no devuelve `false`.
- [x] **2.4 Caracterizar `isValid` con token nulo o vacío.**
  - Confirmar el comportamiento real antes de fijarlo. Si lanza una excepción no controlada, la prueba la documenta como brecha frente al requisito "rechazar sin excepción no controlada".
- [x] **2.5 Ejecutar `python tools/lab.py test auth`.**
  - Hecho cuando: todas las pruebas de caracterización pasan con el comportamiento real registrado.

## Fase 3. Implementación incremental (solo si D2, D3 o D5 se aprueban)

Cada subtarea es un cambio independiente y revisable. Si una decisión no se aprueba, su subtarea se omite y su prueba de la fase 2 queda como registro.

- [ ] **3.1 (D3) Validar longitud mínima de `AUTH_TOKEN_KEY`.** Omitida: D3 se respondió sin longitud mínima.
  - Cambio mínimo en `JwtTokenService`: rechazar claves de menos de 32 caracteres con un mensaje controlado.
  - Actualizar la prueba 2.2 al nuevo comportamiento.
  - Verificar: `python tools/lab.py test auth`.
- [ ] **3.2 (D3) Hacer que `isValid` no lance por configuración.** Pendiente: la aprobación de D3 no cubrió el comportamiento de `isValid` con clave en blanco.
  - Si se aprueba devolver `false` con clave en blanco, capturar la excepción de configuración o moverla fuera del `try` según corresponda.
  - Actualizar las pruebas 2.3 y 2.4.
  - Verificar: `python tools/lab.py test auth`.
- [x] **3.3 (D2) Cambiar el fallo de clave ausente, solo si se exige sin detalle.**
  - Ajustar `AuthResource` o `AuthService` para responder un error sin `exception`.
  - Este cambio altera el contrato de error del 500; si se aprueba, debe documentarse en `docs/ENDPOINTS.md`.
  - Actualizar la prueba 2.1.
  - Verificar: `python tools/lab.py test auth`.
- [x] **3.4 Confirmar que no cambió el contrato observable.**
  - Los 400, 401, 405, 415 y CORS siguen con sus respuestas actuales (pruebas existentes en `AuthResourceTest`).
  - Verificar: `python tools/lab.py test auth`.

## Fase 4. Cobertura y evidencia

- [x] **4.1 Medir cobertura del código modificado.**
  - Ejecutar la cobertura de JaCoCo para `auth/` (reporte en `auth/target/site/jacoco`).
  - Meta: 85 % o más en las clases tocadas.
  - Si no se alcanza, agregar pruebas de caracterización; no reducir el umbral.
- [x] **4.2 Registrar evidencia en la raíz de ai-labs.**
  - Completar el archivo de evidencias generales con IDs `H-AUTH-03` y `H-AUTH-07`.
  - Incluir: comando ejecutado, resultado real, cobertura medida y pruebas que cubren cada criterio del Issue.
  - No incluir tokens, claves ni credenciales.
- [x] **4.3 Confirmar que los quality gates no se silenciaron.**
  - Sin exclusiones de JaCoCo, `@SuppressWarnings`, ni cambios en CodeQL o Dependency Review.

## Fase 5. Documentos de referencia (solo si se aprueba)

- [x] **5.1 Actualizar `docs/ENDPOINTS.md` (sección `auth`) si cambia el contrato de error o la longitud de clave.**
  - No tocar otras secciones.
- [ ] **5.2 Resolver la contradicción de H-AUTH-03 en `HALLAZGOS.md` y `PLAN.md`.**
  - Hoy `HALLAZGOS.md` dice `Resuelto` y `PLAN.md` dice `Pendiente`.
  - Reemplazar la referencia a `LegacyTokenUtil.java` en `HU-01.md` y `PLAN.md` por el archivo real, o marcarla como obsoleta.
  - Requiere aprobación explícita.
- [ ] **5.3 Documentar la longitud mínima de `AUTH_TOKEN_KEY` (solo si D3 se aprueba).** Omitida: D3 sin longitud mínima.
  - Agregar la nota en la sección de `auth/` de la documentación de configuración.

## Fase 6. Cierre del cambio OpenSpec

- [x] **6.1 Validar el cambio.**
  - Comando desde la raíz: `openspec validate auth-token-firmado-login --strict`.
- [ ] **6.2 Verificar que cada requisito de la spec tiene al menos una prueba.**
  - Requisitos: token verificable, rechazo de tokens alterados o no válidos, clave externa, contrato preservado, disponibilidad del endpoint.
- [ ] **6.3 Revisión humana antes de archivar.**
  - No archivar hasta registrar las decisiones de la fase 0 y completar la evidencia de la fase 4.
  - No hacer push, PR ni tags sin instrucción explícita.

## Fuera de este checklist

- Cambiar el algoritmo, el formato del token o los claims.
- Endpoint de validación, refresh o revocación.
- Cambios de H-AUTH-05 o H-AUTH-07 más allá de lo que D2 apruebe.
- Refactor de `AuthResource` por capas.
- Cambios en `PasswordHasher`, `LegacyDatabase` o CORS.
- Cambios en `api/` o `web/`.
