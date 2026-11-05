# Proposal

## Why

El Issue [#24](https://github.com/user-bcp-student07/ai-labs/issues/24) identifica que valores de credenciales con sintaxis SQL pueden alterar el resultado del login y permitir autenticación no autorizada. Corregir ese comportamiento protege las cuentas sin cambiar el contrato de acceso válido que consume el frontend.

## What Changes

- **Alcance:** definir el comportamiento observable del login de `auth/` para que las credenciales se evalúen como datos literales y los intentos inválidos que contienen sintaxis SQL sean rechazados.
- **Comportamiento que se preserva:** `POST /auth/login` continúa aceptando las credenciales válidas existentes; el caso caracterizado para `student` conserva HTTP 200, `user.username` y `token`.
- **Fuera de alcance:** cambios en `api/` o `web/`, otros endpoints, políticas generales de contraseñas, cambios del formato de respuesta o rediseño de tokens y sesiones.
- El cambio de resultado se limita al caso que el Issue señala: un intento de inyección que hoy podría autenticarse debe terminar como credenciales inválidas, HTTP 401 y sin `token`.

## Capabilities

### New Capabilities

- `auth-login`: comportamiento observable de autenticación en `POST /auth/login`, incluyendo credenciales válidas e intentos de inyección.

### Modified Capabilities

- Ninguna. `openspec/specs/` no contiene capacidades existentes.

## Impact

- Afecta únicamente el contrato observable de autenticación en `auth/`, en `POST /auth/login`.
- La descripción del Issue #24 es la fuente del alcance y de los criterios de aceptación; el Issue identifica el hallazgo como `H-AUTH-01`.
- `docs/ENDPOINTS.md`, indicado como baseline del Día 1, no existe en el workspace. Por ello, los escenarios preservan el comportamiento verificable en `AuthResourceTest` (login válido con HTTP 200, `user.username` y `token`) y el contrato que el Issue declara preservar. No se infieren otros compromisos de ese documento ausente.