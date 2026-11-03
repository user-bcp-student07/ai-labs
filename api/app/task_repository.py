import sqlite3


class TaskRepository:
    def __init__(self, connection: sqlite3.Connection) -> None:
        self._connection = connection

    def list_for_owner(self, owner: str) -> list[sqlite3.Row]:
        return self._connection.execute(
            "SELECT id,title,description,status,owner,priority,created_at "
            "FROM tasks WHERE owner=? ORDER BY id",
            (owner,),
        ).fetchall()

    def create(
        self,
        title: str,
        description: str,
        status: str,
        owner: str,
        priority: str,
    ) -> sqlite3.Row:
        cursor = self._connection.execute(
            "INSERT INTO tasks(title,description,status,owner,priority,created_at) "
            "VALUES (?, ?, ?, ?, ?, datetime('now'))",
            (title, description, status, owner, priority),
        )
        self._connection.commit()
        row = self._connection.execute(
            "SELECT id,title,description,status,owner,priority,created_at "
            "FROM tasks WHERE id=?",
            (cursor.lastrowid,),
        ).fetchone()
        if row is None:
            raise RuntimeError("Created task could not be retrieved")
        return row

    def list_status_and_priority_for_owner(self, owner: str) -> list[sqlite3.Row]:
        return self._connection.execute(
            "SELECT status,priority FROM tasks WHERE owner=?",
            (owner,),
        ).fetchall()
