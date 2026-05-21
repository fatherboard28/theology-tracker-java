## Phase 1 — Project Setup & Infrastructure
- Initialize Go module, directory structure (cmd/, internal/handlers, internal/store, internal/models, internal/templates, static/)
- Add all dependencies (chi, modernc/sqlite, sqlx, a-h/templ)
- Wire up chi router with logging and panic recovery middleware
- Set up air for hot reload in development
- Docker + docker-compose.yml with bind mount for ./data/theology.db
- Multi-stage Dockerfile (build stage → lean runtime image)
- Database connection setup with WAL mode and foreign key enforcement enabled
- Migration system — simple versioned SQL files with a schema_migrations table to track applied versions
- Base Templ layout shell (HTML5 boilerplate, nav, HTMX + Alpine.js + Sortable.js + Marked.js loaded from static or CDN)
- Static file serving for CSS and any local JS assets

## Phase 2 — Database Schema (Migrations)
- courses table
- units table (with order column)
- topics table (with nullable parent_topic_id)
- work_items table (with type discriminator, nullable unit_id, nullable owning_topic_id)
- readings, assignments, papers, practice_sessions tables for type-specific fields
- notes table (with primary_parent_type + primary_parent_id polymorphic FK)
- methods table
- study_sessions table
- scripture_tags table (with entity_type + entity_id polymorphic FK)
- Join tables: course_topics, unit_topics, work_item_topics, note_topics, session_topics
- Join table: note_work_items (note ↔ work item many-to-many references)

## Phase 3 — Courses & Units
- Course list view
- Course create / edit / delete
- Course status transitions (Active → Paused → Complete), auto-sets completion date
- Unit list within a course (ordered)
- Unit create / edit / delete
- Unit drag-and-drop reorder (Sortable.js → HTMX POST to persist order)
- Unit auto-complete logic (all work items done → unit marked complete, date set)
- Course progress percentage display (completed work items / total)
- Course and Unit target/start date fields

# Incomplete

## Phase 4 — Topics
- Topic list view
- Topic create / edit / delete
- Parent topic assignment (one level deep) and subtopic display
- Topic type selector (Book of Bible / Theological Theme / Doctrine / Other)
- Reusable topic tag input component (used across all entity types)
- Topic tagging on Courses (UI + store queries)
- Topic tagging on Units (UI + store queries)

## Phase 5 — Work Items (Core)
- Work item create/edit/delete — Reading type
- Work item create/edit/delete — Assignment type
- Work item create/edit/delete — Paper type
- Work item create/edit/delete — Practice Session type
- Work item completion checkbox with auto-date logic (and un-complete clearing it)
- Estimated duration and due date fields on all work item types
- Topic tagging on work items
- Owning topic enforcement at creation (no unit parent → owning topic required)
- Scripture tag attachment to work items

## Phase 6 — Notes
- Note create / edit / delete
- Markdown editor with live split-pane or toggle preview (Marked.js)
- Auto-save while editing (HTMX hx-trigger="keyup changed delay:800ms")
- Last-saved timestamp display
- Primary parent assignment and display in parent views (Course, Unit, Topic, Method, Session)
- Note reassignment to different primary parent
- Topic tagging on notes (tag input within editor view)
- Work item reference management on notes (attach/detach via search — Assignment, Paper, Practice Session types only)
- Deletion confirmation warning when note has work item references
- Note listing section in each parent entity view (title, modified date, topic tags, one-line preview)

## Phase 7 — Methods & Practices
- Method list view
- Method create / edit / delete
- Notes section on method view
- Method usage count display with linked session list

## Phase 8 — Study Sessions
- Session log form (date, duration, optional work item, optional method, optional reflection, optional topic tags, optional scripture tags)
- Free-form session logging (no work item required)
- Session list view per work item
- Session list view per topic
- Session list view per method
- Scripture tag attachment to sessions

## Phase 9 — Progress & Streak Tracking
- Total logged time aggregation at work item level
- Total logged time aggregation at unit and course level
- Total logged time aggregation at topic level
- Current streak calculation (consecutive calendar days with at least one session)
- All-time longest streak calculation
- Last session date + duration retrieval for dashboard

## Phase 10 — Scripture Reference System
- Scripture reference validator (format check against known book abbreviation list)
- Reusable scripture tag input component with validation feedback
- Reference View — browse all entities tagged with a given reference or chapter range
- Partial reference matching (e.g. Rom 8 returns all verses in Romans 8)

## Phase 11 — Dashboard
- Active courses widget (up to 5, with progress % and target date)
- Active topics widget (up to 5, with associated item count)
- Streak display (current + all-time)
- Last session + weekly/monthly time summary
- Upcoming due dates widget (next 7 days — work items, units, courses)
- Recently modified work items (last 5, with links)
- Recently modified notes (last 5, with links)
- Calendar heatmap (trailing 3 months, session intensity shading, future due date indicators) — server-rendered SVG via Templ

## Phase 12 — Calendar & Planning View
- Month view — session intensity shading, completion markers, due date markers
- Week view — per-day detail listing of sessions logged and items due
- Agenda view — chronological list of upcoming due dates and milestones from today
- Overdue item visual distinction (past due date, not complete)
- Click interactions — past day → session log; completion marker → work item; due date marker → work item/unit/course

## Phase 13 — Topic View
- Topic View layout and routing
- Owned work items section (grouped by type)
- Tagged work items section (from any course/unit)
- Tagged courses and units section
- Tagged notes section (from any primary parent)
- Tagged sessions section
- Subtopics section (child topics, one level deep, linking to their own Topic View)
- Informational summary (X complete of Y total work items, total logged time)
- Actions: create owned work item, create owned note, tag/untag existing entities via search

## Phase 14 — Search
- Global search bar accessible from all views
- Search query handler across all entity types (courses, units, topics, work items, notes, sessions, scripture tags)
- Results grouped by type in results view
- Scripture partial reference matching in search

## Phase 15 — Data Export / Import
- JSON full export (all entities, relationships, join table data)
- JSON import with full relationship restoration
- Markdown export (all notes organized by primary parent, plus session reflections)
- Settings / Export page with export trigger UI







