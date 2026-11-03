import base64
import hashlib
import hmac
import json
import time
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.main import create_app

TEST_JWT_SECRET = "test-secret-with-at-least-32-bytes-long"
TEST_JWT_ISSUER = "workshop-auth-test"
TEST_JWT_AUDIENCE = "workshop-api-test"


def encode_segment(value: dict[str, object]) -> str:
    serialized = json.dumps(value, separators=(",", ":")).encode()
    return base64.urlsafe_b64encode(serialized).rstrip(b"=").decode()


@pytest.fixture(autouse=True)
def configure_jwt(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("AUTH_JWT_SECRET", TEST_JWT_SECRET)
    monkeypatch.setenv("AUTH_JWT_ISSUER", TEST_JWT_ISSUER)
    monkeypatch.setenv("AUTH_JWT_AUDIENCE", TEST_JWT_AUDIENCE)


def token(
    username: str = "student",
    role: str = "STUDENT",
    *,
    issuer: str = TEST_JWT_ISSUER,
    audience: str = TEST_JWT_AUDIENCE,
    expires_at: int | None = None,
    signing_secret: str = TEST_JWT_SECRET,
) -> str:
    encoded_header = encode_segment({"alg": "HS256", "typ": "JWT"})
    encoded_claims = encode_segment(
        {
            "sub": username,
            "role": role,
            "iss": issuer,
            "aud": audience,
            "exp": expires_at if expires_at is not None else int(time.time()) + 3600,
        }
    )
    signing_input = f"{encoded_header}.{encoded_claims}".encode()
    signature = hmac.new(signing_secret.encode(), signing_input, hashlib.sha256).digest()
    encoded_signature = base64.urlsafe_b64encode(signature).rstrip(b"=").decode()
    return f"{encoded_header}.{encoded_claims}.{encoded_signature}"


def headers(
    username: str = "student",
    role: str = "STUDENT",
    *,
    token_value: str | None = None,
) -> dict[str, str]:
    bearer_token = token_value or token(username, role)
    return {"Authorization": f"Bearer {bearer_token}"}


def test_lists_seeded_tasks_and_ignores_requested_owner(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.get("/api/tasks?owner=trainer", headers=headers())
    assert response.status_code == 200
    assert len(response.json()) == 2
    assert {task["owner"] for task in response.json()} == {"student"}


def test_creates_task_and_summary_for_authenticated_owner(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        created = client.post(
            "/api/tasks",
            headers=headers(),
            json={
                "title": "O'Brien task",
                "description": "Improve user's coverage",
                "status": "TODO",
                "priority": "HIGH",
                "owner": "trainer",
            },
        )
        summary = client.get("/api/summary?owner=trainer", headers=headers())
    assert created.status_code == 201
    assert created.json()["title"] == "O'Brien task"
    assert created.json()["owner"] == "student"
    assert summary.json()["owner"] == "student"
    assert summary.json()["total"] == 3


def test_uploads_requirement(tmp_path: Path) -> None:
    upload_dir = tmp_path / "uploads"
    app = create_app(str(tmp_path / "workshop.db"), str(upload_dir))
    with TestClient(app) as client:
        response = client.post(
            "/api/requirements",
            headers=headers(),
            files={"file": ("requirements.txt", b"Login must be accessible", "text/plain")},
        )
    assert response.status_code == 201
    assert (upload_dir / "requirements.txt").exists()


def test_rejects_upload_path_traversal(tmp_path: Path) -> None:
    upload_dir = tmp_path / "uploads"
    app = create_app(str(tmp_path / "workshop.db"), str(upload_dir))
    with TestClient(app) as client:
        response = client.post(
            "/api/requirements",
            headers=headers(),
            files={"file": ("../escape.txt", b"must not be written", "text/plain")},
        )
    assert response.status_code == 400
    assert not (tmp_path / "escape.txt").exists()


def test_rejects_windows_upload_path_traversal(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.post(
            "/api/requirements",
            headers=headers(),
            files={"file": (r"..\escape.txt", b"must not be written", "text/plain")},
        )
    assert response.status_code == 400
    assert not (tmp_path / "escape.txt").exists()


def test_rejects_disallowed_upload_extension(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.post(
            "/api/requirements",
            headers=headers(),
            files={"file": ("requirements.exe", b"not allowed", "application/octet-stream")},
        )
    assert response.status_code == 400


def test_rejects_upload_over_size_limit(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    too_large_content = b"x" * (5 * 1024 * 1024 + 1)
    with TestClient(app) as client:
        response = client.post(
            "/api/requirements",
            headers=headers(),
            files={"file": ("requirements.txt", too_large_content, "text/plain")},
        )
    assert response.status_code == 413
    assert not (tmp_path / "uploads" / "requirements.txt").exists()


def test_rejects_invalid_token(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    invalid_tokens = [
        "not-a-jwt",
        token(issuer="unexpected-issuer"),
        token(audience="unexpected-audience"),
        token(expires_at=int(time.time()) - 1),
        token(signing_secret="wrong-signing-secret-for-negative-test"),
    ]
    with TestClient(app) as client:
        responses = [
            client.get("/api/tasks", headers=headers(token_value=value))
            for value in invalid_tokens
        ]
    assert all(response.status_code == 401 for response in responses)
    assert all(response.json()["detail"] == "Invalid token" for response in responses)


def test_fails_closed_when_jwt_configuration_is_missing(
    tmp_path: Path,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    monkeypatch.delenv("AUTH_JWT_SECRET")
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers=headers())
    assert response.status_code == 503
    assert response.json()["detail"] == "Authentication is not configured"
