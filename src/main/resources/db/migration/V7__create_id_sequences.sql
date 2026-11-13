-- Hibernate generates project/project_document ids from sequences named
-- `project_seq` and `project_document_seq` (see the entities' @SequenceGenerator).
-- The original tables were created with BIGSERIAL, which only produces the
-- column-owned `*_id_seq` sequences, so inserts via Hibernate failed in prod with
-- `relation "project_seq" does not exist`. Create the expected sequences and set
-- them just past the highest seeded id so new rows never collide with seed data.

CREATE SEQUENCE IF NOT EXISTS project_seq INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS project_document_seq INCREMENT BY 1;

-- setval(..., false) leaves is_called = false so the next nextval() returns the
-- given value (max existing id + 1).
SELECT setval('project_seq', COALESCE((SELECT MAX(id) FROM project), 0) + 1, false);
SELECT setval('project_document_seq', COALESCE((SELECT MAX(id) FROM project_document), 0) + 1, false);
