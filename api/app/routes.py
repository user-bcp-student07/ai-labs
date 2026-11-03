import os
from pathlib import Path, PureWindowsPath
from typing import Any

import urllib3
from fastapi import (
    APIRouter,
    Body,
    Depends,
    File,
    HTTPException,
    Query,
    Request,
    UploadFile,
)

from app.security import verified_identity

ALLOWED_UPLOAD_EXTENSIONS = frozenset({".txt", ".md", ".pdf"})
BYTES_PER_KIBIBYTE = 1024
MAX_UPLOAD_SIZE_BYTES = 5 * BYTES_PER_KIBIBYTE * BYTES_PER_KIBIBYTE
UPLOAD_CHUNK_SIZE_BYTES = 64 * BYTES_PER_KIBIBYTE

router = APIRouter()


@router.get("/api/tasks")
def list_tasks(
    request: Request,
    owner: str | None = Query(default=None),
    identity: dict[str, str] = Depends(verified_identity),
) -> list[dict[str, Any]]:
    return request.app.state.task_service.list_tasks(identity["username"])


@router.post("/api/tasks", status_code=201)
def create_task(
    request: Request,
    payload: dict[str, Any] = Body(...),
    identity: dict[str, str] = Depends(verified_identity),
) -> dict[str, Any]:
    return request.app.state.task_service.create_task(
        payload,
        identity["username"],
    )


@router.get("/api/summary")
def summary(
    request: Request,
    owner: str | None = Query(default=None),
    identity: dict[str, str] = Depends(verified_identity),
) -> dict[str, Any]:
    return request.app.state.task_service.summarize(identity["username"])


@router.post("/api/requirements", status_code=201)
async def upload_requirement(
    request: Request,
    file: UploadFile = File(...),
    identity: dict[str, str] = Depends(verified_identity),
) -> dict[str, Any]:
    filename = file.filename or ""
    if (
        not filename
        or Path(filename).name != filename
        or PureWindowsPath(filename).name != filename
    ):
        raise HTTPException(status_code=400, detail="Invalid file name")
    if Path(filename).suffix.lower() not in ALLOWED_UPLOAD_EXTENSIONS:
        raise HTTPException(status_code=400, detail="Unsupported file extension")

    target = Path(request.app.state.upload_dir) / filename
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
    return {
        "owner": identity["username"],
        "fileName": filename,
        "storedAt": str(target),
        "size": size,
    }


@router.get("/api/backend-health")
def backend_health(
    identity: dict[str, str] = Depends(verified_identity),
) -> dict[str, Any]:
    url = os.getenv("AUTH_HEALTH_URL", "http://localhost:8080/q/health")
    try:
        response = urllib3.PoolManager().request("GET", url, timeout=3.0)
        return {
            "requestedBy": identity["username"],
            "status": response.status,
            "body": response.data.decode("utf-8"),
        }
    except Exception as error:
        return {
            "requestedBy": identity["username"],
            "status": 0,
            "body": str(error),
        }
