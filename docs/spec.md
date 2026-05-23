# Theology Study Tracker — Specification

## Vision

A personal, local-first tool for serious theological study built around three pillars:

1. **Knowledge Bank** — a living tree of topics enriched with freestanding notes
2. **Papers** — a first-class Turabian-style academic writing environment
3. **Learning Paths** — structured courses with kanban task boards, fulfilled by notes and papers

Everything is organized by **Topics**. Notes are tagged to topics. Papers are tagged to topics. Course tasks reference notes and papers. The topic tree is the spine of the whole system.

---

## Domain Model

### Topic

The backbone of the knowledge bank. Topics form an unlimited-depth tree (a topic can have one parent, and any number of children, recursively).

| Field         | Type   | Notes                                      |
|---------------|--------|--------------------------------------------|
| id            | Long   | PK                                         |
| title         | String | required                                   |
| description   | String | optional; markdown                         |
| parent_topic  | Topic  | nullable; self-referential FK              |
| created_at    | DateTime |                                          |

A topic page shows: its description, all direct children topics, and all Notes + Papers tagged to it.

---

### Note

Freestanding markdown document. The atom of the knowledge bank. Notes are not owned by any single topic — they are tagged with zero or more topics and exist independently.

| Field      | Type     | Notes                             |
|------------|----------|-----------------------------------|
| id         | Long     | PK                                |
| title      | String   | required                          |
| body       | Text     | markdown                          |
| starred    | Boolean  | default false                     |
| created_at | DateTime |                                   |
| updated_at | DateTime |                                   |

**Relationships:**
- Tagged with zero or more Topics (many-to-many via `note_topics`)
- Referenced by zero or more Tasks (many-to-many via `task_note_refs`)

**Editor:** A split-pane markdown editor — raw text on the left, live rendered preview on the right. Topic tags are selected via a searchable multi-select.

---

### Paper

A first-class Turabian-style academic document. Not a note. A paper has structure, footnotes, a bibliography, and is meant to be exported as a formatted Word document.

| Field         | Type     | Notes                                               |
|---------------|----------|-----------------------------------------------------|
| id            | Long     | PK                                                  |
| title         | String   | required                                            |
| thesis        | Text     | the paper's thesis/abstract statement               |
| author        | String   | defaults to user's name                             |
| body           | Text     | stored as TipTap/ProseMirror JSON (rich text)      |
| schema_version | Integer  | TipTap document schema version; default 1           |
| footnotes      | Text     | stored as JSON array of footnote objects            |
| bibliography   | Text     | stored as JSON array of bibliography entries        |
| word_count     | Integer  | auto-calculated from body on save                   |
| status         | Enum     | `DRAFT`, `IN_PROGRESS`, `COMPLETE`                  |
| created_at    | DateTime |                                                     |
| updated_at    | DateTime |                                                     |

**Relationships:**
- Tagged with zero or more Topics (many-to-many via `paper_topics`)
- Referenced by zero or more Tasks (many-to-many via `task_paper_refs`)

**Editor:** A WYSIWYG rich-text editor (TipTap) with:
- Toolbar: Bold, Italic, Underline, H1, H2, H3, Block Quote, Footnote insertion
- Inline footnote markers (superscript numbers) that link to a footnote panel below the editor
- Footnote panel: ordered list of footnotes with Turabian-formatted content fields
- Bibliography panel: list of sources with structured fields (author, title, place, publisher, year, pages)
- Word count display (live)
- Export to `.docx` button (Apache POI — Turabian-formatted Word document)
- Export to `.pdf` button (OpenHTMLtoPDF — renders paper HTML+CSS to print-quality PDF)

**Turabian Notes-Bibliography system:**
- Footnotes use the Notes-Bibliography (NB) style, not Author-Date
- Each footnote has: author(s), title, publication info, page(s)
- Bibliography is a separate alphabetical list of all cited sources

---

### Course

A structured learning path. Contains an ordered list of Tasks displayed as a kanban board.

| Field       | Type     | Notes                                    |
|-------------|----------|------------------------------------------|
| id          | Long     | PK                                       |
| title       | String   | required                                 |
| description | String   | optional                                 |
| status      | Enum     | `ACTIVE`, `PAUSED`, `COMPLETE`           |
| created_at  | DateTime |                                          |

---

### Task

A unit of work within a Course. Lives on the course's kanban board. A Task is "fulfilled" by linking Notes and/or Papers to it — those documents are the evidence the task is done.

| Field          | Type     | Notes                                         |
|----------------|----------|-----------------------------------------------|
| id             | Long     | PK                                            |
| course_id      | Long     | FK → courses                                  |
| title          | String   | required                                      |
| description    | Text     | optional                                      |
| status         | Enum     | `TO_DO`, `IN_PROGRESS`, `DONE`                |
| board_position | Integer  | sort order within its status column           |
| due_date       | Date     | optional                                      |
| created_at     | DateTime |                                               |

**Relationships:**
- References zero or more Notes (many-to-many via `task_note_refs`)
- References zero or more Papers (many-to-many via `task_paper_refs`)

---

## Feature Specifications

### 1. Knowledge Bank

#### Topic Tree
- Sidebar or dedicated page shows all root-level topics, expandable to show children
- Topics can be created, edited, renamed, re-parented, and deleted
- Deleting a topic with children: children become root-level (not deleted)
- Deleting a topic only removes the topic tag from notes/papers — it does not delete the documents

#### Topic Page
Shows:
- Topic title and description (editable inline)
- Breadcrumb showing parent path up the tree
- Child topics (as cards or list)
- All Notes tagged to this topic (sorted by updated_at desc)
- All Papers tagged to this topic (sorted by updated_at desc)
- A quick "New Note for this topic" button that pre-tags the new note with this topic

#### Global Search
- Search bar in the top navigation
- Searches across: topic titles + descriptions, note titles + bodies, paper titles + theses
- Results grouped by type (Topics / Notes / Papers)
- Keyboard shortcut: `Cmd+K`

---

### 2. Notes

#### Notes List
- Filterable by topic tag, starred status
- Sortable by title, created, updated
- "New Note" button opens the note editor

#### Note Editor
- Split pane: raw markdown on left, rendered preview on right (using marked.js)
- Toggle to full-width edit mode or full-width preview mode
- Topic tag selector: searchable multi-select, can create new topics inline
- Starred toggle
- Auto-save on typing pause (debounced, ~1.5s)
- "Last saved" timestamp shown

#### Note Page (read view)
- Rendered markdown
- Topic tags shown as clickable chips (navigate to that topic)
- Edit button → opens editor
- **Backlinks section:** other Notes whose body contains `[[This Note's Title]]`. Computed on read by scanning the `notes.body` column for the title string. No stored index — fast enough for a local single-user SQLite database.

---

### 3. Papers

#### Papers List
- Filterable by topic tag, status (Draft / In Progress / Complete)
- Shows word count and last-updated date
- "New Paper" button opens metadata form, then enters editor

#### Paper Editor
- Top bar: Title (editable), Status selector, Word Count (live)
- Thesis field below title (styled distinctly)
- Main body area: TipTap WYSIWYG editor
  - Toolbar: Bold, Italic, Underline, H1, H2, H3, Block Quote, `Insert Footnote`
  - Pressing `Insert Footnote` inserts a superscript marker `[1]` in the text and adds a new entry in the Footnotes panel
- **Footnotes Panel** (below body): ordered list; each entry has content field for the Turabian-formatted footnote text
- **Bibliography Panel** (below footnotes): list of source entries; each has fields:
  - Last Name, First Name
  - Title (italicized in output)
  - Place of Publication, Publisher, Year
  - Page(s) cited
  - Source type (Book, Journal Article, Essay in Collection, etc.)
- Topic tag selector (same as notes)
- Author field (prefilled from settings)
- Export to `.docx` (Turabian formatted with title page, body, footnotes, bibliography)

#### Paper Page (read view)
- Rendered rich text
- Footnotes displayed at bottom
- Bibliography displayed at end
- Topic tags as chips
- Edit button

---

### 4. Learning Paths (Courses)

#### Course List
- Shows all courses with status and task progress (e.g. "4 / 12 done")
- "New Course" button

#### Course Kanban Board
- Three columns: **To Do** / **In Progress** / **Done**
- Tasks displayed as cards within each column
- Drag-and-drop to reorder within a column or move between columns (updates status)
- Task card shows: title, due date (if set), count of linked notes + papers
- "Add Task" button at bottom of any column

#### Task Detail (modal or side panel)
- Title (editable)
- Description (markdown textarea)
- Due date picker
- Status selector (or moved via drag-and-drop)
- **Linked Documents section**: search and attach existing Notes and Papers; clicking a linked document opens it in a new pane or navigates to it
- The intention: a task like "Read and respond to Calvin on justification" is fulfilled by linking the paper you wrote in response

---

## Navigation Structure

```
/ (dashboard — simple: recent notes, recent papers, active courses)
/topics           — topic tree browser
/topics/{id}      — topic page (notes + papers + children)
/notes            — all notes
/notes/new        — new note editor
/notes/{id}       — note read view
/notes/{id}/edit  — note editor
/papers           — all papers
/papers/new       — new paper (metadata form)
/papers/{id}      — paper read view
/papers/{id}/edit — paper editor
/courses          — course list
/courses/new      — new course form
/courses/{id}     — course kanban board
/search           — search results
/settings         — author name + dark/light mode toggle
```

---

## Out of Scope

The following are explicitly excluded:

- Study session time tracking
- Calendar / heatmap activity views
- Scripture reference tagging system (a topic named "Romans 8:28" is sufficient)
- Study methods catalog
- Work item subtypes (Reading, Assignment, Practice Session)
- Units within courses
- Data export / import (can be added later)
- User accounts / authentication (local-only, single user)

---

## Tech Stack

| Layer        | Technology                                       |
|--------------|--------------------------------------------------|
| Language     | Java 21                                          |
| Framework    | Spring Boot 3.3                                  |
| Build        | Maven                                            |
| Database     | SQLite via JPA/Hibernate + Flyway migrations     |
| Templates    | Thymeleaf + Thymeleaf Layout Dialect             |
| Interactivity| HTMX (partial page updates) + Alpine.js (UI state)|
| Rich Text    | TipTap (ProseMirror-based, vanilla JS bundle)    |
| Markdown     | marked.js (note preview rendering)               |
| Drag & Drop  | SortableJS (kanban board)                        |
| Icons        | Lucide                                           |
| Styling      | Custom CSS (Apple design system tokens)          |
| Export       | Apache POI (`.docx`) + OpenHTMLtoPDF (`.pdf`)    |

---

## Data Schema (target)

```sql
CREATE TABLE topics (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    title           TEXT    NOT NULL,
    description     TEXT,
    parent_topic_id INTEGER REFERENCES topics(id) ON DELETE SET NULL,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE notes (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    title       TEXT    NOT NULL,
    body        TEXT    NOT NULL DEFAULT '',
    starred     INTEGER NOT NULL DEFAULT 0,
    created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at  TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE papers (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    title         TEXT    NOT NULL,
    thesis        TEXT,
    author        TEXT,
    body            TEXT    NOT NULL DEFAULT '{}',   -- TipTap/ProseMirror JSON
    schema_version  INTEGER NOT NULL DEFAULT 1,      -- TipTap doc schema version
    footnotes       TEXT    NOT NULL DEFAULT '[]',   -- JSON array
    bibliography    TEXT    NOT NULL DEFAULT '[]',   -- JSON array
    word_count      INTEGER NOT NULL DEFAULT 0,
    status        TEXT    NOT NULL DEFAULT 'DRAFT'
                      CHECK (status IN ('DRAFT', 'IN_PROGRESS', 'COMPLETE')),
    created_at    TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at    TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE courses (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    title       TEXT    NOT NULL,
    description TEXT,
    status      TEXT    NOT NULL DEFAULT 'ACTIVE'
                    CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETE')),
    created_at  TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE tasks (
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

-- Tag joins
CREATE TABLE note_topics (
    note_id  INTEGER NOT NULL REFERENCES notes(id)  ON DELETE CASCADE,
    topic_id INTEGER NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    PRIMARY KEY (note_id, topic_id)
);

CREATE TABLE paper_topics (
    paper_id INTEGER NOT NULL REFERENCES papers(id) ON DELETE CASCADE,
    topic_id INTEGER NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    PRIMARY KEY (paper_id, topic_id)
);

-- Task fulfillment joins
CREATE TABLE task_note_refs (
    task_id INTEGER NOT NULL REFERENCES tasks(id)  ON DELETE CASCADE,
    note_id INTEGER NOT NULL REFERENCES notes(id)  ON DELETE CASCADE,
    PRIMARY KEY (task_id, note_id)
);

CREATE TABLE task_paper_refs (
    task_id  INTEGER NOT NULL REFERENCES tasks(id)   ON DELETE CASCADE,
    paper_id INTEGER NOT NULL REFERENCES papers(id)  ON DELETE CASCADE,
    PRIMARY KEY (task_id, paper_id)
);
```

---

## Dashboard

The home screen (`/`) shows:

- **Active Learning Paths** — all courses with status `ACTIVE` or `IN_PROGRESS`, each showing task progress (e.g. "4 / 12 done") and a link to the kanban board
- **Recent Notes** — the 8 most recently updated notes (title, updated timestamp, topic tags)
- **Recent Papers** — the 8 most recently updated papers (title, status, word count, updated timestamp)

No heatmaps, no activity tracking. Just what you're working on right now and what you touched last.

---

## Decisions Log

All open questions from the spec have been resolved:

| # | Question | Decision |
|---|----------|----------|
| 1 | Paper body storage format | TipTap/ProseMirror JSON. Added `schema_version` column so documents can be detected and migrated if TipTap ever changes its document schema in a major version. |
| 2 | Export formats | Both `.docx` (Apache POI) and `.pdf` (OpenHTMLtoPDF). OpenHTMLtoPDF renders the paper's HTML view through Turabian CSS — same styles as the screen view, print-quality output, pure Java. |
| 3 | Note backlinks | Yes, in v1. Computed on read by scanning `notes.body` for `[[Note Title]]`. No stored index — a SQLite `LIKE` query across a local single-user DB is fast enough. |
| 4 | Settings page | Author name (prefilled on new papers) + dark/light mode toggle. Nothing else in v1. |
| 5 | Dashboard | Active learning paths with task progress + recent notes + recent papers. No activity tracking. |
