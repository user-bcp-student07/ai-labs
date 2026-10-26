# TESTING

## auth

### Matriz de endpoints

| endpoint | caso | tipo | resultado esperado |
|---|---|---|---|
| `GET /q/health` | Responde sin header de autenticacion | Feliz (H-AUTH-08) | 200; `status=UP`; `checks=[]`. |
| `GET /q/health` | Health DOWN / 503 | Pendiente (H-AUTH-08) | Contrato de error y cuerpo no definidos ni reproducibles con los checks actuales; no se afirma un resultado. |
| `POST /auth/login` | Login `student` con `client` | Feliz (H-AUTH-01, H-AUTH-04, H-AUTH-12) | 200; token string, `expiresIn=3600`, datos de usuario y `debug` observados. |
| `POST /auth/login` | Login `trainer` | Feliz (H-AUTH-01, H-AUTH-09) | 200; username `trainer` y token no nulo. |
| `POST /auth/login` | Usuario recien sembrado | Borde (H-AUTH-09) | 200; autentica con password recien almacenado como hash. |
| `POST /auth/login` | Username almacenado con comilla | Borde (H-AUTH-02) | 200 para credencial correcta; 401 para password incorrecto; el username es dato SQL. |
| `POST /auth/login` | JSON `null` | Error (H-AUTH-06) | 400; `{"message":"Request is null"}`. |
| `POST /auth/login` | Username ausente, null, vacio o espacios | Borde/error (H-AUTH-06) | 400; `{"message":"Username is required"}`. |
| `POST /auth/login` | Password ausente, null, vacio o espacios | Borde/error (H-AUTH-06) | 400; `{"message":"Password is required"}`. |
| `POST /auth/login` | JSON malformado | Error (contrato HTTP) | 400; body vacio en Quarkus local. |
| `POST /auth/login` | Credenciales incorrectas | Error (H-AUTH-02, H-AUTH-09) | 401; el mensaje incluye el username recibido. |
| `POST /auth/login` | Content-Type `text/plain` | Error (contrato HTTP) | 415; body vacio en Quarkus local. |
| `GET /auth/login` | Metodo no declarado | Error (contrato HTTP) | 405; body vacio en Quarkus local. |
| `POST /auth/login` | Excepcion controlada en `AuthService` | Error (H-AUTH-05) | 500; message y nombre de clase reflejan la excepcion controlada. Prueba directa del recurso, no llamada HTTP. |
| `OPTIONS /auth/login` | Preflight desde localhost | Borde/configuracion (H-AUTH-10) | Origen localhost permitido. |
| `OPTIONS /auth/login` | Preflight desde origen configurado | Borde/configuracion (H-AUTH-10) | Origen definido en `AUTH_CORS_ORIGINS` permitido. |
| `OPTIONS /auth/login` | Preflight desde origen no configurado | Error/configuracion (H-AUTH-10) | 403; sin `Access-Control-Allow-Origin`. |

Los casos ejecutables del endpoint estan en [AuthResourceTest.java](../auth/src/test/java/com/bcp/workshop/auth/AuthResourceTest.java). `AuthServiceTest`, `LegacyDatabaseTest` y `PasswordHasherTest` complementan las reglas y persistencia; no sustituyen los casos HTTP de esta matriz.

### Codigo de los casos agregados

Estos metodos se agregaron a `AuthResourceTest` para cubrir health, campos nulos y errores de protocolo:

```java
@Test
void reportsHealthyStatusWithoutAuthentication() {
    given()
            .when().get("/q/health")
            .then()
            .statusCode(200)
            .body("status", is("UP"))
            .body("checks", hasSize(0));
}

@Test
void rejectsNullUsername() {
    given()
            .contentType("application/json")
            .body("{\"username\":null,\"password\":\"password\"}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Username is required"));
}

@Test
void rejectsNullPassword() {
    given()
            .contentType("application/json")
            .body("{\"username\":\"student\",\"password\":null}")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body("message", is("Password is required"));
}

@Test
void rejectsMalformedJson() {
    given()
            .contentType("application/json")
            .body("{bad")
            .when().post("/auth/login")
            .then()
            .statusCode(400)
            .body(is(""));
}

@Test
void rejectsUnsupportedContentType() {
    given()
            .contentType("text/plain")
            .body("{}")
            .when().post("/auth/login")
            .then()
            .statusCode(415)
            .body(is(""));
}

@Test
void rejectsUnsupportedHttpMethod() {
    given()
            .when().get("/auth/login")
            .then()
            .statusCode(405)
            .body(is(""));
}
```

El 500 controlado existente esta en `AuthResourceTest.returnsCurrentErrorDetailsForControlledFailure`; los caminos felices, los blancos, 401, SQL con username entrecomillado y CORS tambien se conservan en ese archivo.

### Comandos y resultados

Componente: `auth`. Directorio de ejecucion: `auth/`. Base SQLite de pruebas: `AUTH_DB_URL=jdbc:sqlite:target/h-auth-02-characterization.db`; `AUTH_CORS_ORIGINS` se dejo sin definir para verificar el default local.

| comando | codigo de salida | resultado |
|---|---:|---|
| `mvn '-Dtest=AuthResourceTest' test` | 0 | 18 tests, 0 fallos, 0 errores, 0 omitidos. Incluye los seis casos agregados. |
| `mvn verify` | 0 | BUILD SUCCESS; 23 tests, 0 fallos, 0 errores, 0 omitidos. AuthResourceTest 18; AuthServiceTest 2; LegacyDatabaseTest 1; PasswordHasherTest 2. |

La baseline inmediatamente anterior al cambio fue `mvn verify`, codigo 0, 17 tests sin fallos. El reporte posterior esta en `auth/target/site/jacoco/index.html` y `auth/target/site/jacoco/jacoco.xml`.

Metodo de cobertura: contadores de lineas `LINE` por clase en `jacoco.xml`; porcentaje = lineas cubiertas / (cubiertas + no cubiertas). No se modifico codigo de produccion en este paso; las clases ejercitadas superan el 85%: `AuthResource` 17/17 (100%), `AuthService` 12/12 (100%), `LoginResponse` 1/1 (100%), `LoginResponse.User` 1/1 (100%), `LegacyDatabase` 56/59 (94,92%), `PasswordHasher` 28/30 (93,33%) y `LegacyTokenUtil` 3/3 (100%).

### Fallos, pendientes y propuestas

- **Fallos:** ninguno en las ejecuciones de este paso.
- **Pendiente:** la respuesta 503 y su body para health DOWN no se probaron porque `auth/` no registra un health check que pueda ponerse en DOWN. `docs/ENDPOINTS.md` deja este comportamiento como pregunta abierta; no se agrego un check ficticio al producto.
- **Pendiente:** el 500 esta comprobado invocando `AuthResource.login` con un servicio controlado, no mediante una llamada HTTP al endpoint. La clase no tiene un seam de inyeccion para sustituir el servicio en la prueba REST.
- **Propuesta:** cuando se acuerde el contrato de health DOWN, agregar un perfil/check de prueba aislado para comprobar el 503 y su esquema sin afectar la suite normal.
- **Propuesta:** cuando se agregue un seam de prueba aprobado para excepciones del servicio, cubrir el 500 a traves de HTTP; no cambiar los valores de error sin resolver H-AUTH-05/compatibilidad.
