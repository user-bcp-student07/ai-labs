# Proposal

Referencia: GitHub Issue [#24](https://github.com/user-bcp-student07/ai-labs/issues/24) (HU-01, "Emisión segura del token de autenticación"). Componente: `auth/`.

Nota: el título del Issue (“MCP local - write permission test”) no coincide con su cuerpo, que es HU-01. Se toma el cuerpo como fuente de alcance.

## Why

El token que devuelve `POST /auth/login` debe poder verificarse criptográficamente y no depender de un secreto constante incluido en el código. Hoy un consumidor no puede comprobar que el token no fue alterado, y un secreto embebido en el código se filtraría con el repositorio. Esto cubre H-AUTH-03 (severidad alta) y el riesgo de compatibilidad de H-AUTH-07 sobre los campos de respuesta.

Estado observado en el repositorio (verificado en código, no solo en documentos):

- `JwtTokenService.java` emite un JWT HS256 firmado. La clave viene de `auth.token.key`, que se enlaza a `AUTH_TOKEN_KEY`; sin ella la emisión falla con `IllegalStateException`.
- `LegacyTokenUtil.java`, citado por HU-01 y `PLAN.md`, ya no existe.
- `HALLAZGOS.md` marca H-AUTH-03 como `Resuelto`; `PLAN.md` lo marca como `Pendiente`. Esta propuesta no resuelve esa contradicción, solo la registra.
- `src/main/resources/application.properties` contiene un valor literal (`%test.auth.token.key`) usado solo en el perfil de pruebas. El perfil por defecto no tiene constante.

Por tanto, el cambio no reimplementa la firma. Fija como especificación verificable el comportamiento que el Issue exige y que el código ya cumple, para que la cobertura de HU-01 sea trazable.

## Comportamiento que debe preservarse

Línea base: [docs/ENDPOINTS.md](../../../docs/ENDPOINTS.md), sección `POST /auth/login`.

- Ruta `POST /auth/login`, `Content-Type: application/json`, sin header `Authorization`.
- Respuesta 200 con `token` (string), `expiresIn` (entero, 3600), `user` (objeto con `username`, `displayName`, `role`) y `debug` (string, valor `authenticated-with-legacy-flow`).
- Errores 400 (`Request is null`, `Username is required`, `Password is required`) y 401 (`Invalid user <username> or password`) con sus mensajes actuales.
- Formato del token: JWT HS256 con `sub`, `role`, `iat` y `exp`, expiración de 3600 s. Es el formato vigente desde el corte de H-AUTH-03 (sin formato dual). El Issue indica no presuponer un algoritmo: por eso la spec exige verificabilidad de firma, pero **cualquier cambio de algoritmo o de formato requiere acuerdo previo con consumidores** y queda fuera de este cambio.

## What Changes

Especificación `auth-login-token` con cuatro requisitos verificables, uno por criterio de aceptación del Issue, más la preservación del endpoint:

- Token de login firmado y verificable.
- Rechazo de tokens alterados, firmados con otra clave o malformados.
- Clave de firma tomada de configuración externa (`AUTH_TOKEN_KEY`), sin clave constante en el perfil por defecto.
- Contrato de respuesta de `POST /auth/login` conservado; el valor del token se trata como opaco en las pruebas de contrato.

No se modifica código funcional. No se crea `tasks.md` en esta etapa.

**BREAKING:** ninguno. El contrato observado se conserva.

## Capabilities

### New Capabilities

- `auth-login-token`: comportamiento observable de emisión y verificación del token devuelto por `POST /auth/login` en `auth/`.

### Modified Capabilities

- Ninguna. `openspec/specs/` no contiene capacidades previas.

## Out of Scope

- Cambiar el algoritmo, el formato del token o los claims sin acuerdo con consumidores.
- Eliminar o cambiar el campo `debug` o la exposición de `exception`/`message` en el 500 (H-AUTH-05, H-AUTH-07). El Issue exige conservar `debug`.
- Endpoint de validación de tokens para consumidores, refresh tokens o revocación.
- Migración de contraseñas a hashes (H-AUTH-09) y cambios en `PasswordHasher`.
- Política de CORS (`auth.cors` en `application.properties`).
- Eliminar o reemplazar el valor literal del perfil `%test` (se evaluará en la fase de diseño/tareas).
- Health check `/q/health`.
- Diseño técnico de la implementación y lista de tareas (`design` y `tasks` se elaboran después).
- Cambios en `api/` o `web/`.

## Impact

- Componente afectado: `auth/` (solo especificación en esta etapa).
- Documentos de referencia: `HU-01.md`, `HALLAZGOS.md` (H-AUTH-03, H-AUTH-07), `PLAN.md` (H-AUTH-03), `docs/ENDPOINTS.md`.
- Inconsistencias documentales que quedan abiertas: `HU-01.md` y `PLAN.md` citan `LegacyTokenUtil.java`, inexistente; `PLAN.md` marca H-AUTH-03 como `Pendiente` mientras `HALLAZGOS.md` lo marca como `Resuelto`.
- Dependencias: ninguna nueva.
- Consumidores (`web/` y cualquier integrador): deben seguir funcionando sin cambios; la prueba de contrato lo verifica.
- Evidencia esperada: salida de pruebas de firma (token aceptado, token alterado rechazado) y de contrato de `POST /auth/login`, sin publicar tokens, secretos ni credenciales.
