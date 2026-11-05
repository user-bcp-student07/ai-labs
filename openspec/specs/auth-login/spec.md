# auth-login Specification

## Purpose
Define el comportamiento observable del inicio de sesión para preservar el acceso válido y rechazar intentos de autenticación con credenciales manipuladas.

## Requirements

### Requirement: Preserve successful login contract
The login endpoint SHALL preserve the successful response contract for valid credentials.

#### Scenario: Existing student credentials authenticate successfully
- **WHEN** a client sends `student` and `Workshop123!` to `POST /auth/login`
- **THEN** the response has HTTP 200, `user.username` is `student`, and `token` is present

### Requirement: Reject SQL injection credentials
The login endpoint SHALL reject credentials that do not match an authorized account, and SQL-like input SHALL NOT authenticate a user or return a token.

#### Scenario: SQL-like username is rejected
- **WHEN** a client sends `' OR '1'='1` as the username to `POST /auth/login`
- **THEN** the response has HTTP 401 and does not contain `token`

#### Scenario: SQL-like password is rejected
- **WHEN** a client sends `' OR '1'='1` as the password to `POST /auth/login`
- **THEN** the response has HTTP 401 and does not contain `token`
