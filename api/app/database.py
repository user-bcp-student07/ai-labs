import sqlite3
from pathlib import Path


def initialize_database(db_path: str) -> sqlite3.Connection:
    Path(db_path).parent.mkdir(parents=True, exist_ok=True)
    connection = sqlite3.connect(db_path, check_same_thread=False)
    connection.row_factory = sqlite3.Row
    connection.execute(
        "CREATE TABLE IF NOT EXISTS tasks ("
        "id INTEGER PRIMARY KEY AUTOINCREMENT, "
        "title TEXT NOT NULL, "
        "description TEXT NOT NULL, "
        "status TEXT NOT NULL, "
        "owner TEXT NOT NULL, "
        "priority TEXT NOT NULL, "
        "created_at TEXT NOT NULL)"
    )
    count = connection.execute(
        "SELECT COUNT(*) AS total FROM tasks"
    ).fetchone()["total"]
    if count == 0:
        connection.execute(
            "INSERT INTO tasks(title,description,status,owner,priority,created_at) "
            "VALUES ('Refactor login','Separar responsabilidades en frontend',"
            "'TODO','student','HIGH',datetime('now'))"
        )
        connection.execute(
            "INSERT INTO tasks(title,description,status,owner,priority,created_at) "
            "VALUES ('Review API','Revisar seguridad de endpoints',"
            "'IN_PROGRESS','student','MEDIUM',datetime('now'))"
        )
        connection.execute(
            "INSERT INTO tasks(title,description,status,owner,priority,created_at) "
            "VALUES ('Trainer notes','Preparar retrospectiva',"
            "'TODO','trainer','LOW',datetime('now'))"
        )
        connection.commit()
    return connection
