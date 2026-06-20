-- Adds per-user ownership/visibility to projects.
--   created_by : username (JWT upn) of the user that created the project.
--                NULL for legacy/seeded rows.
--   shared     : when true the project is visible to every authenticated user
--                (used for the example/case-study projects). Owned projects are
--                only visible to their creator and to the "admin" user.
ALTER TABLE project ADD COLUMN IF NOT EXISTS created_by VARCHAR(120);
ALTER TABLE project ADD COLUMN IF NOT EXISTS shared BOOLEAN NOT NULL DEFAULT FALSE;

-- The two example projects must remain visible to everyone:
--   id = 1 -> Critical Railway Control System (CRCS) - Dissertation Case Study
--   id = 2 -> Example Project
UPDATE project SET shared = TRUE WHERE id IN (1, 2);
