# Spec Delta

## Purpose

Define el comportamiento observable del token devuelto por `POST /auth/login` en `auth/` (HU-01, Issue #24): un token cuya firma puede verificarse, clave de firma externa y un contrato de respuesta estable para los consumidores.

## ADDED Requirements

### Requirement: Token de login verificable criptográficamente
El sistema SHALL devolver en `POST /auth/login`, tras un login exitoso, un token con firma criptográfica que permita comprobar su integridad y autenticidad. El token SHALL identificar al usuario autenticado y contener las fechas de emisión y expiración.

#### Scenario: Token recién emitido supera la verificación
- **WHEN** un login exitoso devuelve un `token`
- **THEN** la verificación de firma con la clave de firma configurada acepta ese token

#### Scenario: Expiración de una hora
- **WHEN** se emite un token tras un login exitoso
- **THEN** la expiración del token es la fecha de emisión más 3600 segundos

### Requirement: Rechazo de tokens alterados o no válidos
El sistema SHALL rechazar cualquier token cuya firma no corresponda a la clave de firma configurada, incluido un token cuyo contenido haya sido modificado, y SHALL rechazar cadenas que no sean un token válido sin propagar una excepción no controlada.

#### Scenario: Token alterado es rechazado
- **WHEN** se modifica el contenido de un token emitido por el login y se verifica la firma
- **THEN** la verificación falla y el token se considera inválido

#### Scenario: Token firmado con otra clave es rechazado
- **WHEN** se verifica un token firmado con una clave distinta a la configurada
- **THEN** la verificación falla y el token se considera inválido

#### Scenario: Cadena malformada es rechazada
- **WHEN** se verifica una cadena que no es un token válido
- **THEN** la verificación devuelve inválido sin lanzar una excepción no controlada

### Requirement: Clave de firma externa
El sistema SHALL obtener la clave de firma desde configuración externa (variable de entorno `AUTH_TOKEN_KEY`). El perfil por defecto SHALL NOT contener una clave de firma constante en el código o en la configuración versionada; el valor literal usado en el perfil de pruebas SHALL estar aislado a ese perfil y no SHALL usarse fuera de pruebas.

#### Scenario: Clave ausente
- **WHEN** `AUTH_TOKEN_KEY` no está configurada o está vacía
- **THEN** el login no devuelve ningún token y la emisión falla de forma controlada

#### Scenario: Clave configurada
- **WHEN** `AUTH_TOKEN_KEY` contiene un valor no vacío
- **THEN** los tokens emitidos se firman con ese valor y la verificación con el mismo valor los acepta

### Requirement: Contrato de respuesta de login preservado
El sistema SHALL mantener en la respuesta exitosa de `POST /auth/login` los campos `token` (string), `expiresIn` (entero, 3600), `user` (objeto con `username`, `displayName` y `role`) y `debug` (string), con los nombres y tipos documentados en `docs/ENDPOINTS.md`. Las pruebas de contrato SHALL verificar presencia y tipo del `token` sin fijar su valor.

#### Scenario: Campos y tipos del 200
- **WHEN** el login es exitoso
- **THEN** la respuesta tiene status 200, `token` es string no vacío, `expiresIn` es 3600, `user` es objeto y `debug` es string

#### Scenario: Token tratado como opaco en el contrato
- **WHEN** una prueba de contrato valida la respuesta de login
- **THEN** verifica la presencia y el tipo de `token` sin comparar su valor

### Requirement: Disponibilidad del endpoint de login
El sistema SHALL mantener la ruta `POST /auth/login` y sus respuestas de error observadas (400 para campos ausentes, 401 para credenciales inválidas) sin cambios como parte de este requisito.

#### Scenario: Credenciales inválidas
- **WHEN** un cliente envía un usuario inexistente o una contraseña que no coincide
- **THEN** la respuesta tiene status 401 y el mensaje `Invalid user <username> or password`

#### Scenario: Campo requerido ausente
- **WHEN** un cliente omite `username` o `password`
- **THEN** la respuesta tiene status 400 y el mensaje `Username is required` o `Password is required`, según el campo ausente
