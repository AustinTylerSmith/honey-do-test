CREATE TABLE invitations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    list_id INTEGER NOT NULL REFERENCES lists(id),
    invited_by_user_id INTEGER NOT NULL REFERENCES users(id),
    email TEXT NOT NULL,
    token TEXT NOT NULL UNIQUE,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    expires_at TEXT NOT NULL,
    accepted_at TEXT
);

CREATE INDEX idx_invitations_token ON invitations(token);
CREATE INDEX idx_invitations_list_email ON invitations(list_id, email);
