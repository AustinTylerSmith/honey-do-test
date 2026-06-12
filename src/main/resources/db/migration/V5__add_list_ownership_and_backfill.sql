ALTER TABLE lists ADD COLUMN owner_id INTEGER REFERENCES users(id);

-- Backfill: give every existing user their own list, owned by them
INSERT INTO lists (name, owner_id, created_at)
SELECT 'My Tasks', id, datetime('now') FROM users;

-- Make every user a member of their own list
INSERT INTO user_list (user_id, list_id, created_at)
SELECT l.owner_id, l.id, datetime('now')
FROM lists l;

-- Attach existing tasks to their owner's list
INSERT INTO task_list (task_id, list_id, created_at)
SELECT t.id, l.id, datetime('now')
FROM tasks t
JOIN lists l ON l.owner_id = t.user_id;
