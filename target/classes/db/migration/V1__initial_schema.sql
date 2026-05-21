-- V1__initial_schema.sql
-- Full initial schema for the Theology Study Tracker.
-- All tables created here correspond to Phase 2 of the project task list.
-- SQLite notes:
--   • INTEGER PRIMARY KEY is an alias for the rowid (auto-increment).
--   • TEXT stores all string data; SQLite is dynamically typed.
--   • FOREIGN KEY constraints are enforced because PRAGMA foreign_keys = ON
--     is set at connection startup in DatabaseConfig.java.
--   • Booleans stored as INTEGER (0/1); dates stored as TEXT (ISO-8601).

-- ─────────────────────────────────────────────────────────────────────────────
-- COURSES
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS courses (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    title                TEXT    NOT NULL,
    description          TEXT,
    status               TEXT    NOT NULL DEFAULT 'ACTIVE'   -- ACTIVE | PAUSED | COMPLETE
                             CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETE')),
    start_date           TEXT,    -- ISO-8601 date e.g. 2025-09-01
    target_completion    TEXT,    -- ISO-8601 date
    actual_completion    TEXT,    -- set automatically when status → COMPLETE
    created_at           TEXT    NOT NULL DEFAULT (datetime('now'))
);

-- ─────────────────────────────────────────────────────────────────────────────
-- UNITS  (belong to a Course)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS units (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    course_id            INTEGER NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title                TEXT    NOT NULL,
    description          TEXT,
    unit_order           INTEGER NOT NULL DEFAULT 0,   -- sequential position within course
    target_completion    TEXT,    -- ISO-8601 date
    actual_completion    TEXT,    -- set automatically when all work items complete
    created_at           TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_units_course_id ON units(course_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- TOPICS
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS topics (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    title                TEXT    NOT NULL,
    description          TEXT,
    type                 TEXT    NOT NULL DEFAULT 'OTHER'
                             CHECK (type IN ('BOOK_OF_BIBLE', 'THEOLOGICAL_THEME', 'DOCTRINE', 'OTHER')),
    parent_topic_id      INTEGER REFERENCES topics(id) ON DELETE SET NULL,   -- one level deep
    created_at           TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_topics_parent ON topics(parent_topic_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- WORK ITEMS
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS work_items (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    type                 TEXT    NOT NULL
                             CHECK (type IN ('READING', 'ASSIGNMENT', 'PAPER', 'PRACTICE_SESSION')),
    title                TEXT    NOT NULL,
    status               TEXT    NOT NULL DEFAULT 'NOT_STARTED'
                             CHECK (status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETE')),
    estimated_duration   INTEGER,   -- minutes
    due_date             TEXT,      -- ISO-8601 date
    completion_date      TEXT,      -- set automatically when status → COMPLETE
    general_notes        TEXT,
    unit_id              INTEGER REFERENCES units(id) ON DELETE CASCADE,
    owning_topic_id      INTEGER REFERENCES topics(id) ON DELETE RESTRICT,
    created_at           TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at           TEXT    NOT NULL DEFAULT (datetime('now')),

    -- Enforcement: must have at least a unit parent OR an owning topic.
    -- This is also enforced in application logic, but we document intent here.
    CHECK (unit_id IS NOT NULL OR owning_topic_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_work_items_unit     ON work_items(unit_id);
CREATE INDEX IF NOT EXISTS idx_work_items_topic    ON work_items(owning_topic_id);
CREATE INDEX IF NOT EXISTS idx_work_items_status   ON work_items(status);
CREATE INDEX IF NOT EXISTS idx_work_items_due_date ON work_items(due_date);

-- ─────────────────────────────────────────────────────────────────────────────
-- READING  (type-specific fields for work items of type READING)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS readings (
    work_item_id         INTEGER PRIMARY KEY REFERENCES work_items(id) ON DELETE CASCADE,
    source               TEXT    NOT NULL,   -- book title, article name, or "Scripture"
    author               TEXT,
    location             TEXT,    -- page range or scripture reference
    format               TEXT    NOT NULL DEFAULT 'PHYSICAL_BOOK'
                             CHECK (format IN ('PHYSICAL_BOOK', 'PDF', 'ONLINE_ARTICLE', 'SCRIPTURE'))
);

-- ─────────────────────────────────────────────────────────────────────────────
-- ASSIGNMENT  (type-specific fields)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS assignments (
    work_item_id         INTEGER PRIMARY KEY REFERENCES work_items(id) ON DELETE CASCADE,
    description          TEXT
);

-- ─────────────────────────────────────────────────────────────────────────────
-- PAPER  (type-specific fields)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS papers (
    work_item_id         INTEGER PRIMARY KEY REFERENCES work_items(id) ON DELETE CASCADE,
    prompt_or_topic      TEXT,
    word_count_target    INTEGER,
    score_or_grade       TEXT    -- freeform: letter grade, percentage, or narrative
);

-- ─────────────────────────────────────────────────────────────────────────────
-- PRACTICE_SESSION_ITEMS  (type-specific fields)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS practice_session_items (
    work_item_id         INTEGER PRIMARY KEY REFERENCES work_items(id) ON DELETE CASCADE,
    method_id            INTEGER REFERENCES methods(id) ON DELETE SET NULL,
    scripture_passage    TEXT,
    duration_minutes     INTEGER
);

-- ─────────────────────────────────────────────────────────────────────────────
-- METHODS  (forward-declared before practice_session_items references it)
-- Note: SQLite resolves FK references at runtime, not parse time, so the
-- ordering above is acceptable. Provided here for logical clarity.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS methods (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    name                 TEXT    NOT NULL,
    description          TEXT,
    personal_notes       TEXT,
    created_at           TEXT    NOT NULL DEFAULT (datetime('now'))
);

-- ─────────────────────────────────────────────────────────────────────────────
-- NOTES
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notes (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    title                 TEXT    NOT NULL,
    body                  TEXT    NOT NULL DEFAULT '',   -- markdown
    primary_parent_type   TEXT    NOT NULL
                              CHECK (primary_parent_type IN ('COURSE', 'UNIT', 'TOPIC', 'METHOD', 'SESSION')),
    primary_parent_id     INTEGER NOT NULL,
    created_at            TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at            TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_notes_parent ON notes(primary_parent_type, primary_parent_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- STUDY SESSIONS
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS study_sessions (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    session_date         TEXT    NOT NULL,   -- ISO-8601 date
    duration_minutes     INTEGER NOT NULL,
    work_item_id         INTEGER REFERENCES work_items(id) ON DELETE SET NULL,
    method_id            INTEGER REFERENCES methods(id) ON DELETE SET NULL,
    reflection_note      TEXT,
    created_at           TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_sessions_date      ON study_sessions(session_date);
CREATE INDEX IF NOT EXISTS idx_sessions_work_item ON study_sessions(work_item_id);

-- ─────────────────────────────────────────────────────────────────────────────
-- SCRIPTURE TAGS
-- Polymorphic: can tag a work_item, topic, or study_session
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS scripture_tags (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    reference            TEXT    NOT NULL,   -- e.g. "Rom 8:28", "Gen 1:1-5"
    entity_type          TEXT    NOT NULL
                             CHECK (entity_type IN ('WORK_ITEM', 'TOPIC', 'SESSION')),
    entity_id            INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_scripture_tags_entity ON scripture_tags(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_scripture_tags_ref    ON scripture_tags(reference);

-- ─────────────────────────────────────────────────────────────────────────────
-- TOPIC TAG JOIN TABLES  (many-to-many: entity ↔ topics)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS course_topics (
    course_id   INTEGER NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    topic_id    INTEGER NOT NULL REFERENCES topics(id)  ON DELETE CASCADE,
    PRIMARY KEY (course_id, topic_id)
);

CREATE TABLE IF NOT EXISTS unit_topics (
    unit_id     INTEGER NOT NULL REFERENCES units(id)   ON DELETE CASCADE,
    topic_id    INTEGER NOT NULL REFERENCES topics(id)  ON DELETE CASCADE,
    PRIMARY KEY (unit_id, topic_id)
);

CREATE TABLE IF NOT EXISTS work_item_topics (
    work_item_id  INTEGER NOT NULL REFERENCES work_items(id) ON DELETE CASCADE,
    topic_id      INTEGER NOT NULL REFERENCES topics(id)     ON DELETE CASCADE,
    PRIMARY KEY (work_item_id, topic_id)
);

CREATE TABLE IF NOT EXISTS note_topics (
    note_id     INTEGER NOT NULL REFERENCES notes(id)   ON DELETE CASCADE,
    topic_id    INTEGER NOT NULL REFERENCES topics(id)  ON DELETE CASCADE,
    PRIMARY KEY (note_id, topic_id)
);

CREATE TABLE IF NOT EXISTS session_topics (
    session_id  INTEGER NOT NULL REFERENCES study_sessions(id) ON DELETE CASCADE,
    topic_id    INTEGER NOT NULL REFERENCES topics(id)         ON DELETE CASCADE,
    PRIMARY KEY (session_id, topic_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- NOTE ↔ WORK ITEM REFERENCES  (many-to-many)
-- Only Assignment, Paper, Practice Session types reference notes.
-- Application logic enforces the type restriction.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS note_work_items (
    note_id      INTEGER NOT NULL REFERENCES notes(id)      ON DELETE CASCADE,
    work_item_id INTEGER NOT NULL REFERENCES work_items(id) ON DELETE CASCADE,
    PRIMARY KEY (note_id, work_item_id)
);
