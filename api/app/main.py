from __future__ import annotations

import os
import sqlite3
from pathlib import Path, PureWindowsPath
from typing import Any

import jwt
import urllib3
from fastapi import Body, Depends, FastAPI, File, Header, HTTPException, Query, UploadFile
from fastapi.middleware.cors import CORSMiddleware

JWT_ALGORITHM = "HS256"
JWT_SECRET_ENV = "AUTH_JWT_SECRET"
JWT_ISSUER_ENV = "AUTH_JWT_ISSUER"
JWT_AUDIENCE_ENV = "AUTH_JWT_AUDIENCE"
JWT_MINIMUM_SECRET_BYTES = 32
INVALID_TOKEN_DETAIL = "Invalid token"
AUTH_CONFIGURATION_DETAIL = "Authentication is not configured"
ALLOWED_UPLOAD_EXTENSIONS = frozenset({".txt", ".md", ".pdf"})
BYTES_PER_KIBIBYTE = 1024
MAX_UPLOAD_SIZE_BYTES = 5 * BYTES_PER_KIBIBYTE * BYTES_PER_KIBIBYTE
UPLOAD_CHUNK_SIZE_BYTES = 64 * BYTES_PER_KIBIBYTE


def verified_identity(authorization: str = Header(default="")) -> dict[str, str]:
    scheme, separator, token = authorization.partition(" ")
    if scheme.lower() != "bearer" or not separator or not token.strip():
        raise HTTPException(status_code=401, detail=INVALID_TOKEN_DETAIL)

    secret = os.getenv(JWT_SECRET_ENV, "")
    issuer = os.getenv(JWT_ISSUER_ENV, "")
    audience = os.getenv(JWT_AUDIENCE_ENV, "")
    if (
        len(secret.encode("utf-8")) < JWT_MINIMUM_SECRET_BYTES
        or not issuer
        or not audience
    ):
        raise HTTPException(status_code=503, detail=AUTH_CONFIGURATION_DETAIL)

    try:
        claims = jwt.decode(
            token.strip(),
            secret,
            algorithms=[JWT_ALGORITHM],
            issuer=issuer,
            audience=audience,
            options={"require": ["exp", "iss", "aud", "sub", "role"]},
        )
    except jwt.PyJWTError as error:
        raise HTTPException(status_code=401, detail=INVALID_TOKEN_DETAIL) from error

    username = claims.get("sub")
    role = claims.get("role")
    if not isinstance(username, str) or not username or not isinstance(role, str) or not role:
        raise HTTPException(status_code=401, detail=INVALID_TOKEN_DETAIL)
    return {"username": username, "role": role}


def create_app(db_path: str | None = None, upload_dir: str | None = None) -> FastAPI:
    selected_db = db_path or os.getenv("WORKSHOP_DB_PATH", "./data/workshop.db")
    selected_uploads = upload_dir or os.getenv("WORKSHOP_UPLOAD_DIR", "./uploads")
    Path(selected_db).parent.mkdir(parents=True, exist_ok=True)
    Path(selected_uploads).mkdir(parents=True, exist_ok=True)

    connection = sqlite3.connect(selected_db, check_same_thread=False)
    connection.row_factory = sqlite3.Row
    connection.execute("CREATE TABLE IF NOT EXISTS tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, description TEXT NOT NULL, status TEXT NOT NULL, owner TEXT NOT NULL, priority TEXT NOT NULL, created_at TEXT NOT NULL)")
    count = connection.execute("SELECT COUNT(*) AS total FROM tasks").fetchone()["total"]
    if count == 0:
        connection.execute("INSERT INTO tasks(title,description,status,owner,priority,created_at) VALUES ('Refactor login','Separar responsabilidades en frontend','TODO','student','HIGH',datetime('now'))")
        connection.execute("INSERT INTO tasks(title,description,status,owner,priority,created_at) VALUES ('Review API','Revisar seguridad de endpoints','IN_PROGRESS','student','MEDIUM',datetime('now'))")
        connection.execute("INSERT INTO tasks(title,description,status,owner,priority,created_at) VALUES ('Trainer notes','Preparar retrospectiva','TODO','trainer','LOW',datetime('now'))")
        connection.commit()

    app = FastAPI(title="Workshop Requirements API", version="0.1.0")
    app.state.connection = connection
    app.state.upload_dir = selected_uploads
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    def as_task(row: sqlite3.Row) -> dict[str, Any]:
        return {
            "id": row["id"],
            "title": row["title"],
            "description": row["description"],
            "status": row["status"],
            "owner": row["owner"],
            "priority": row["priority"],
            "createdAt": row["created_at"],
        }

    @app.get("/api/tasks")
    def list_tasks(owner: str | None = Query(default=None), identity: dict[str, str] = Depends(verified_identity)) -> list[dict[str, Any]]:
        requested_owner = identity["username"]
        rows = connection.execute(
            "SELECT id,title,description,status,owner,priority,created_at FROM tasks WHERE owner=? ORDER BY id",
            (requested_owner,),
        ).fetchall()
        result: list[dict[str, Any]] = []
        for row in rows:
            result.append(as_task(row))
        return result

    @app.post("/api/tasks", status_code=201)
    def create_task(payload: dict[str, Any] = Body(...), identity: dict[str, str] = Depends(verified_identity)) -> dict[str, Any]:
        title = str(payload.get("title", ""))
        description = str(payload.get("description", ""))
        status = str(payload.get("status", "TODO"))
        priority = str(payload.get("priority", "MEDIUM"))
        owner = identity["username"]
        cursor = connection.execute(
            "INSERT INTO tasks(title,description,status,owner,priority,created_at) VALUES (?, ?, ?, ?, ?, datetime('now'))",
            (title, description, status, owner, priority),
        )
        connection.commit()
        row = connection.execute(
            "SELECT id,title,description,status,owner,priority,created_at FROM tasks WHERE id=?",
            (cursor.lastrowid,),
        ).fetchone()
        return as_task(row)

    @app.get("/api/summary")
    def summary(owner: str | None = Query(default=None), identity: dict[str, str] = Depends(verified_identity)) -> dict[str, Any]:
        requested_owner = identity["username"]
        rows = connection.execute(
            "SELECT status,priority FROM tasks WHERE owner=?",
            (requested_owner,),
        ).fetchall()
        response: dict[str, Any] = {"owner": requested_owner, "total": 0, "byStatus": {}, "highPriorityOpen": 0}
        for row in rows:
            response["total"] = response["total"] + 1
            if row["status"] in response["byStatus"]:
                response["byStatus"][row["status"]] = response["byStatus"][row["status"]] + 1
            else:
                response["byStatus"][row["status"]] = 1
            if row["priority"] == "HIGH":
                if row["status"] != "DONE":
                    if row["status"] != "ARCHIVED":
                        response["highPriorityOpen"] = response["highPriorityOpen"] + 1
        return response

    @app.post("/api/requirements", status_code=201)
    async def upload_requirement(file: UploadFile = File(...), identity: dict[str, str] = Depends(verified_identity)) -> dict[str, Any]:
        filename = file.filename or ""
        if (
            not filename
            or Path(filename).name != filename
            or PureWindowsPath(filename).name != filename
        ):
            raise HTTPException(status_code=400, detail="Invalid file name")
        if Path(filename).suffix.lower() not in ALLOWED_UPLOAD_EXTENSIONS:
            raise HTTPException(status_code=400, detail="Unsupported file extension")

        target = Path(selected_uploads) / filename
        size = 0
        file_created = False
        upload_completed = False
        try:
            with target.open("xb") as destination:
                file_created = True
                while chunk := await file.read(UPLOAD_CHUNK_SIZE_BYTES):
                    if size + len(chunk) > MAX_UPLOAD_SIZE_BYTES:
                        raise HTTPException(status_code=413, detail="File is too large")
                    destination.write(chunk)
                    size += len(chunk)
            upload_completed = True
        except FileExistsError as error:
            raise HTTPException(status_code=409, detail="File already exists") from error
        finally:
            if file_created and not upload_completed:
                target.unlink(missing_ok=True)
            await file.close()
        return {"owner": identity["username"], "fileName": filename, "storedAt": str(target), "size": size}

    @app.get("/api/backend-health")
    def backend_health(identity: dict[str, str] = Depends(verified_identity)) -> dict[str, Any]:
        url = os.getenv("AUTH_HEALTH_URL", "http://localhost:8080/q/health")
        try:
            response = urllib3.PoolManager().request("GET", url, timeout=3.0)
            return {"requestedBy": identity["username"], "status": response.status, "body": response.data.decode("utf-8")}
        except Exception as error:
            return {"requestedBy": identity["username"], "status": 0, "body": str(error)}

    @app.on_event("shutdown")
    def close_connection() -> None:
        connection.close()

    return app


app = create_app()
