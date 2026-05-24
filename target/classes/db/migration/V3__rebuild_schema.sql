-- V3__rebuild_schema.sql
-- Drops the old schema and rebuilds for the three-pillar architecture:
-- Knowledge Bank (Topics + Notes), Papers, Learning Paths (Courses + Tasks).
-- SQLite notes: DROP TABLE is not constrained by FK at DDL time (only DML).

-- ─────────────────────────────────────────────────────────────────────────────
-- Drop all old tables (order: dependents first)
-- ─────────────────────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS session_topics;
DROP TABLE IF EXISTS note_work_items;
DROP TABLE IF EXISTS work_item_topics;
DROP TABLE IF EXISTS unit_topics;
DROP TABLE IF EXISTS course_topics;
DROP TABLE IF EXISTS note_topics;
DROP TABLE IF EXISTS scripture_tags;
DROP TABLE IF EXISTS study_sessions;
DROP TABLE IF EXISTS practice_session_items;
DROP TABLE IF EXISTS assignments;
DROP TABLE IF EXISTS papers;
DROP TABLE IF EXISTS readings;
DROP TABLE IF EXISTS work_items;
DROP TABLE IF EXISTS units;
DROP TABLE IF EXISTS methods;
DROP TABLE IF EXISTS courses;

-- ─────────────────────────────────────────────────────────────────────────────
-- Migrate topics: recreate without 'type' column
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE topics_new (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    title           TEXT    NOT NULL,
    description     TEXT,
    parent_topic_id INTEGER REFERENCES topics_new(id) ON DELETE SET NULL,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now'))
);

INSERT INTO topics_new (id, title, description, parent_topic_id, created_at)
SELECT id, title, description, parent_topic_id, created_at FROM topics;

DROP TABLE topics;
ALTER TABLE topics_new RENAME TO topics;

CREATE INDEX IF NOT EXISTS idx_topics_parent ON topics(parent_topic_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- Migrate notes: recreate without primary_parent_type / primary_parent_id
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE notes_new (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    title      TEXT    NOT NULL,
    body       TEXT    NOT NULL DEFAULT '',
    starred    INTEGER NOT NULL DEFAULT 0,
    created_at TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT    NOT NULL DEFAULT (datetime('now'))
);

INSERT INTO notes_new (id, title, body, starred, created_at, updated_at)
SELECT id, title, body, starred, created_at, updated_at FROM notes;

DROP TABLE notes;
ALTER TABLE notes_new RENAME TO notes;

-- ─────────────────────────────────────────────────────────────────────────────
-- note_topics join table (fresh)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS note_topics (
    note_id  INTEGER NOT NULL REFERENCES notes(id)  ON DELETE CASCADE,
    topic_id INTEGER NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    PRIMARY KEY (note_id, topic_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Papers (new standalone entity, not a work_item subtype)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS papers (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    title          TEXT    NOT NULL,
    thesis         TEXT,
    author         TEXT,
    body           TEXT    NOT NULL DEFAULT '{}',   -- TipTap JSON
    schema_version INTEGER NOT NULL DEFAULT 1,
    footnotes      TEXT    NOT NULL DEFAULT '[]',   -- JSON array
    bibliography   TEXT    NOT NULL DEFAULT '[]',   -- JSON array
    word_count     INTEGER NOT NULL DEFAULT 0,
    status         TEXT    NOT NULL DEFAULT 'DRAFT'
                               CHECK (status IN ('DRAFT', 'IN_PROGRESS', 'COMPLETE')),
    created_at     TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at     TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS paper_topics (
    paper_id INTEGER NOT NULL REFERENCES papers(id)  ON DELETE CASCADE,
    topic_id INTEGER NOT NULL REFERENCES topics(id)  ON DELETE CASCADE,
    PRIMARY KEY (paper_id, topic_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Courses (new simplified schema)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS courses (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    title       TEXT    NOT NULL,
    description TEXT,
    status      TEXT    NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETE')),
    created_at  TEXT    NOT NULL DEFAULT (datetime('now'))
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Tasks (kanban cards inside a Course)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tasks (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    course_id      INTEGER NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title          TEXT    NOT NULL,
    description    TEXT,
    status         TEXT    NOT NULL DEFAULT 'TO_DO'
                               CHECK (status IN ('TO_DO', 'IN_PROGRESS', 'DONE')),
    board_position INTEGER NOT NULL DEFAULT 0,
    due_date       TEXT,
    created_at     TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_tasks_course  ON tasks(course_id);
CREATE INDEX IF NOT EXISTS idx_tasks_status  ON tasks(status);

-- ─────────────────────────────────────────────────────────────────────────────
-- Task attachment join tables
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS task_note_refs (
    task_id INTEGER NOT NULL REFERENCES tasks(id)  ON DELETE CASCADE,
    note_id INTEGER NOT NULL REFERENCES notes(id)  ON DELETE CASCADE,
    PRIMARY KEY (task_id, note_id)
);

CREATE TABLE IF NOT EXISTS task_paper_refs (
    task_id  INTEGER NOT NULL REFERENCES tasks(id)   ON DELETE CASCADE,
    paper_id INTEGER NOT NULL REFERENCES papers(id)  ON DELETE CASCADE,
    PRIMARY KEY (task_id, paper_id)
);
