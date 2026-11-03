import sqlite3
from typing import Any

from app.task_repository import TaskRepository


class TaskService:
    def __init__(self, repository: TaskRepository) -> None:
        self._repository = repository

    def list_tasks(self, owner: str) -> list[dict[str, Any]]:
        return [
            self._as_task(row)
            for row in self._repository.list_for_owner(owner)
        ]

    def create_task(
        self,
        payload: dict[str, Any],
        owner: str,
    ) -> dict[str, Any]:
        row = self._repository.create(
            title=str(payload.get("title", "")),
            description=str(payload.get("description", "")),
            status=str(payload.get("status", "TODO")),
            owner=owner,
            priority=str(payload.get("priority", "MEDIUM")),
        )
        return self._as_task(row)

    def summarize(self, owner: str) -> dict[str, Any]:
        rows = self._repository.list_status_and_priority_for_owner(owner)
        response: dict[str, Any] = {
            "owner": owner,
            "total": 0,
            "byStatus": {},
            "highPriorityOpen": 0,
        }
        for row in rows:
            response["total"] += 1
            status = row["status"]
            response["byStatus"][status] = response["byStatus"].get(status, 0) + 1
            if row["priority"] == "HIGH" and status not in {"DONE", "ARCHIVED"}:
                response["highPriorityOpen"] += 1
        return response

    @staticmethod
    def _as_task(row: sqlite3.Row) -> dict[str, Any]:
        return {
            "id": row["id"],
            "title": row["title"],
            "description": row["description"],
            "status": row["status"],
            "owner": row["owner"],
            "priority": row["priority"],
            "createdAt": row["created_at"],
        }
