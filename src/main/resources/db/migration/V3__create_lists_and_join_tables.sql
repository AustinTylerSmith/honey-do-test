CREATE TABLE lists (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE user_list (
    user_id INTEGER NOT NULL REFERENCES users(id),
    list_id INTEGER NOT NULL REFERENCES lists(id),
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    PRIMARY KEY (user_id, list_id)
);

CREATE TABLE task_list (
    task_id INTEGER NOT NULL REFERENCES tasks(id),
    list_id INTEGER NOT NULL REFERENCES lists(id),
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    PRIMARY KEY (task_id, list_id)
);

CREATE INDEX idx_user_list_list_id ON user_list(list_id);
CREATE INDEX idx_task_list_list_id ON task_list(list_id);
