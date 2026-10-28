# ENDPOINTS

## auth

### POST /auth/login

#### 1. Que hace

Autentica a un usuario del workshop y devuelve sus datos de perfil junto con el token que emite auth. El recurso delega la autenticacion y la construccion de la respuesta a `AuthService`; la ruta es `/auth/login` (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:14,22-23`).

#### 2. Autenticacion requerida

- No requiere autenticacion previa.
- No se requiere ni se procesa un header `Authorization` en este endpoint.
- Enviar `Content-Type: application/json`. La respuesta es JSON.

#### 3. Request

| campo | tipo | obligatorio | descripcion |
|---|---|---|---|
| `username` | string | Si, no nulo ni vacio/espacios | Identificador usado para buscar al usuario. |
| `password` | string | Si, no nulo ni vacio/espacios | Contrasena comprobada por auth. |
| `client` | string | No | Campo aceptado por el DTO; la implementacion actual no lo consulta. |

Los campos y sus tipos proceden de `LoginRequest` (`auth/src/main/java/com/bcp/workshop/auth/LoginRequest.java:3-7`); las validaciones de presencia estan en `AuthResource` (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:24-31`).

Ejemplo JSON completo, usado por la prueba de login exitoso (`auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:30-38`):

```json
{
  "username": "student",
  "password": "Workshop123!",
  "client": "angular-workshop"
}
```

#### 4. Response exitosa

**200 OK**

| campo | tipo | descripcion |
|---|---|---|
| `token` | string | Token emitido por auth. El valor varia en cada emision. |
| `expiresIn` | integer | Duracion declarada en segundos; actualmente `3600`. |
| `user` | object | Datos del usuario autenticado. |
| `user.username` | string | Nombre de usuario. |
| `user.displayName` | string | Nombre para mostrar; auth escapa caracteres HTML. |
| `user.role` | string | Rol almacenado para el usuario, por ejemplo `STUDENT` o `TRAINER`. |
| `debug` | string | Valor fijo actual `authenticated-with-legacy-flow`. |

Los campos se definen en `LoginResponse` y `AuthService` (`auth/src/main/java/com/bcp/workshop/auth/LoginResponse.java:3-4`; `auth/src/main/java/com/bcp/workshop/auth/AuthService.java:23-35`). Este ejemplo completo fue capturado en una ejecucion local; el token es dinamico y no debe reutilizarse (`auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:30-38`):

```json
{
  "token": "<jwt-hs256-firmado>",
  "expiresIn": 3600,
  "user": {
    "username": "student",
    "displayName": "Workshop Student",
    "role": "STUDENT"
  },
  "debug": "authenticated-with-legacy-flow"
}
```

#### 5. Responses de error

**400 Bad Request**, si el body JSON es `null`, falta `username`/`password`, o alguno es nulo, vacio o solo espacios. Los ejemplos siguientes son los cuerpos exactos que construye `AuthResource` (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:24-31`):

```json
{"message":"Request is null"}
```

```json
{"message":"Username is required"}
```

```json
{"message":"Password is required"}
```

Quarkus tambien devolvio **400 Bad Request** con body vacio para JSON malformado en la verificacion local. Ese caso lo genera el parser HTTP/JSON, no el recurso.

**401 Unauthorized**, si no existe el usuario o la contrasena no coincide. El mensaje incluye el username recibido (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:35-38`):

```json
{"message":"Invalid user no-such-user or password"}
```

**415 Unsupported Media Type**, si el request no usa un tipo de contenido aceptado por `@Consumes(application/json)`. Quarkus respondio con body vacio en la verificacion local (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:14-15`).

**405 Method Not Allowed**, si se usa `GET` en la ruta de login, que solo declara `POST`. Quarkus respondio con body vacio en la verificacion local (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:21-23`).

**500 Internal Server Error**, si una excepcion escapa de `AuthService`. El mensaje y nombre de clase se devuelven literalmente; este comportamiento se verifico con una excepcion controlada (`auth/src/main/java/com/bcp/workshop/auth/AuthResource.java:40-45`; `auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java:145-165`):

```json
{
  "message": "controlled failure",
  "exception": "java.lang.IllegalStateException"
}
```

#### 6. Notas de seguridad

- El token es un JWT HS256 firmado con la clave de `AUTH_TOKEN_KEY` (`auth/src/main/java/com/bcp/workshop/auth/JwtTokenService.java`). Contiene `sub`, `role`, `iat` y `exp`; la firma permite detectar alteraciones. La clave es obligatoria: sin `AUTH_TOKEN_KEY` el login falla. Nunca documentar ni versionar su valor.
- La expiracion es de 3600 segundos (`exp - iat`) y `expiresIn` refleja ese valor. Los consumidores deben validar la firma y `exp` antes de confiar en los claims.
- Las contrasenas se verifican con PBKDF2 y se parametriza la consulta SQL; la migracion de contrasenas antiguas conserva un riesgo documentado en H-AUTH-09 (`auth/src/main/java/com/bcp/workshop/auth/LegacyDatabase.java:63-80`; `auth/src/main/java/com/bcp/workshop/auth/PasswordHasher.java:12-58`).
- El 500 expone mensaje y clase de excepcion (H-AUTH-05 pendiente). No mostrar esa informacion a usuarios finales.
- CORS esta habilitado; el origen predeterminado es `http://localhost:4200` y otros origenes deben declararse con `AUTH_CORS_ORIGINS`. Un preflight desde un origen no permitido se rechaza (`auth/src/main/resources/application.properties:4-7`).
- El endpoint no requiere `Authorization`; esto no implica que el token emitido sea seguro para otros componentes. La compatibilidad y validacion del token siguen pendientes.

### GET /q/health

#### 1. Que hace

Informa si el servicio auth esta disponible. El endpoint lo provee la extension SmallRye Health de Quarkus, incluida en el proyecto (`auth/pom.xml:39-42`); no es una ruta implementada por `AuthResource`.

#### 2. Autenticacion requerida

- No requiere autenticacion.
- No se requiere header `Authorization`.
- `Accept: application/json` es opcional para solicitar JSON; no se requiere ningun header de autenticacion.

#### 3. Request

| campo | tipo | obligatorio | descripcion |
|---|---|---|---|
| body | ninguno | No | `GET` no envia body ni campos JSON. |

Ejemplo de request HTTP sin body:

```http
GET /q/health HTTP/1.1
Host: 127.0.0.1:8080
```

No hay ejemplo JSON de request porque esta ruta no recibe un documento JSON.

#### 4. Response exitosa

**200 OK** cuando el estado agregado es saludable. En la ejecucion local se observo:

| campo | tipo | descripcion |
|---|---|---|
| `status` | string | Estado agregado; el ejemplo actual devuelve `UP`. |
| `checks` | array | Checks registrados por Quarkus; actualmente esta vacio. |

Ejemplo JSON completo capturado en `http://127.0.0.1:8080/q/health`:

```json
{
  "status": "UP",
  "checks": []
}
```

#### 5. Responses de error

- En el codigo actual de `auth/` no hay health checks propios ni respuestas de error definidas por la aplicacion; en esta verificacion el endpoint respondio `200 OK`.
- Un estado `DOWN`/503 depende de health checks o condiciones del runtime que no estan definidos ni se pudieron provocar en este componente. Su cuerpo exacto queda como pregunta abierta; no se documenta un ejemplo inventado.
- La ruta `/q/health` es la ruta observada del endpoint de SmallRye Health; no se verificaron respuestas para rutas incorrectas ni fallos de red, que no son cuerpos de error de esta API.

#### 6. Notas de seguridad

- Esta ruta informa salud, no autentica usuarios ni valida el token de login.
- Puerto y host locales actuales: `8080` y `127.0.0.1`; en perfil prod el host se configura como `0.0.0.0` (`auth/src/main/resources/application.properties:1-3`).
- No se registraron checks de readiness personalizados en el codigo auth consultado. No interpretar `UP` como prueba de que consumidores, base de datos u otros servicios esten listos.

### Contrato acordado y preguntas abiertas

- **Contrato observado:** los codigos, campos y ejemplos anteriores proceden del codigo/pruebas de auth y de una consulta local al endpoint SmallRye Health.
- **Contrato acordado:** no hay en estos documentos una decision que cambie el formato HTTP, los valores de error o el formato del token. Integradores deben tratar los campos observados como contrato vigente sin asumir que el token sea verificable.
- **Pendiente:** confirmar si `debug` y `exception` deben conservarse; ambos siguen presentes en la respuesta actual.
- **Cambio aplicado (H-AUTH-03):** el token pasa de Base64 a JWT HS256 firmado (corte directo, sin formato dual). Consumidores que decodificaban el Base64 anterior deben migrar a validar el JWT.
- **Pendiente:** registrar los origenes CORS autorizados para cada despliegue no local.
- **Pendiente:** definir/ejecutar un health check DOWN para fijar status y cuerpo de fallo; el ejemplo de error no se inventa.
