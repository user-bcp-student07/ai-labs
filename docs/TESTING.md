# Testing — Lab 01

Evidencia real de las ejecuciones de esta sesión (Día 1, componente `auth`,
cambio OpenSpec `parameterize-auth-login-queries`, Issue #24 / H-AUTH-01,
rama `feature/H-AUTH-01`). Ninguna cobertura ni resultado fue inventado; ver
comandos y conteos de pruebas abajo.

## auth — H-AUTH-01: caracterización, prueba de la vulnerabilidad y corrección

Estado: `LegacyDatabase.findByCredentials` usa `PreparedStatement` con
parámetros para `username` y `password`. Secuencia ejecutada (tareas de
`openspec/changes/parameterize-auth-login-queries/tasks.md`):

1. Caracterización sobre código sin modificar (1.1 a 1.5): 7 pruebas, 0 fallos.
2. Pruebas de inyección sobre código sin modificar (2.1 a 2.3): 9 pruebas,
   **2 fallos** (`Expected status code <401> but was <200>`), lo que demuestra
   la vulnerabilidad.
3. Corrección de `findByCredentials` (3.1) y `mvn test` (3.2): 9 pruebas, 0
   fallos.
4. `mvn verify` (4.1): `BUILD SUCCESS`, 9 pruebas, 0 fallos.

Comandos (desde `auth/`):

```bash
cd auth
mvn test "-Dquarkus.http.test-port=18081"
mvn verify "-Dquarkus.http.test-port=18081"
```

Cobertura JaCoCo (`auth/target/site/jacoco/`): `findByCredentials` 94% de
instrucciones y 75% de ramas (meta 85% cumplida); solo el `catch` con
`error.printStackTrace()` queda sin cubrir.

| Comportamiento protegido | Tipo | Prueba | Resultado final |
|---|---|---|---|
| Spec: `student` / `Workshop123!` → 200, `user.username` y `token` | Válido | `logsInWithWorkshopUser` | 200 |
| Contrato de éxito con otra cuenta: `trainer` / `Trainer123!` → `user.role` = `TRAINER` | Válido | `logsInWithTrainerUser` | 200 |
| Contraseña incorrecta de usuario existente | Error | `rejectsIncorrectPasswordForExistingUser` | 401, sin `token` |
| Usuario inexistente | Error | `rejectsUnknownUser` | 401, sin `token` |
| Usuario vacío: mensaje "Username is required" | Borde | `rejectsEmptyUsername` | 400 |
| Contraseña vacía: mensaje "Password is required" | Borde | `rejectsEmptyPassword` | 400 |
| Spec (escenario de usuario): `' OR '1'='1` con contraseña arbitraria no autentica | Error / seguridad | `rejectsSqlLikeUsernameWithArbitraryPassword` | 401, sin `token` (pasaba antes y después) |
| Spec (escenario de contraseña): `student` + `' OR '1'='1` no autentica | Error / seguridad | `rejectsSqlLikePasswordForExistingUser` | **Antes: 200 (fallaba). Después: 401, sin `token`** |
| `' OR '1'='1` en usuario y contraseña no autentica | Error / seguridad | `rejectsSqlLikeUsernameAndPassword` | **Antes: 200 (fallaba). Después: 401, sin `token`** |

Pendiente: revisión humana del diff y de las pruebas antes de integrar.

## api — `pytest --cov=app --cov-fail-under=85` (venv `api/.venv`, Python 3.13)

Comando:

```bash
cd api
python tools/lab.py setup api   # primera vez
.venv/Scripts/python.exe -m pytest --cov=app --cov-report=term-missing --cov-fail-under=85
```

Resultado: 6 pruebas, 0 fallos. Cobertura total de `app/main.py`:
**92.52%** (gate de 85% de toda la app, ya existente en `tools/lab.py`,
superado).

| Caso | Tipo | Prueba | Resultado |
|---|---|---|---|
| Token válido lista tareas sembradas | Válido | `test_lists_seeded_tasks` | 200 |
| Crear tarea y ver resumen | Válido | `test_creates_task_and_summary` | 201 / 200 |
| Subir archivo de requerimiento | Válido | `test_uploads_requirement` | 201 |
| Token con formato inválido | Error | `test_rejects_invalid_token` | 401 |
| Token JWT expirado | Borde / seguridad | `test_rejects_expired_token` (nueva, H-AUTH-05/06) | 401 |
| Token JWT con firma alterada | Error / seguridad | `test_rejects_token_with_wrong_signature` (nueva, H-AUTH-05/06) | 401 |

## Cómo se midió "código modificado"

Para `auth`, el cambio se limita a `LegacyDatabase.findByCredentials`; su
cobertura medida es 94% de instrucciones (meta 85%). Para `api`, el
cambio se limitó a
`legacy_identity` dentro de `app/main.py`; esa función queda cubierta por las
6 pruebas (incluidas las 2 nuevas), y el resto del archivo (no tocado hoy) ya
tenía cobertura previa cubierta por los tests existentes.

## Pendiente para una próxima sesión

- Revisión humana del diff de `auth` y archivado del cambio OpenSpec.
- Cobertura del `catch` de `findByCredentials` y de la rama 500 de
  `AuthResource`, hoy sin ejercitar.
- Discovery propio de `api` y `web` si se continúa con esos componentes.
