# Theology Study Tracker — Application Specification

**Version:** 1.3
**Date:** May 2026
**Status:** Draft

### Revision History

| Version | Change |
|---|---|
| 1.0 | Initial draft |
| 1.1 | Renamed Module → Unit; added Notes system; added document references on work items; clarified Unit vs. Topic distinction |
| 1.2 | Confirmed Notes ↔ Work Items as explicit many-to-many; clarified Note parent vs. reference distinction |
| 1.3 | Added due dates / projected completion to Work Items, Units, Courses; calendar upgraded to planning view; Topics redesigned as owners and referencers; topic tagging added to all entities; Topics lose status/progress; cross-linking replaced by unified topic tag system; Notes tagged to Topics (many-to-many); Topic View defined |

---

## Table of Contents

1. [Overview](#1-overview)
2. [Deployment Architecture](#2-deployment-architecture)
3. [Scripture Reference Standard](#3-scripture-reference-standard)
4. [Core Concepts](#4-core-concepts)
   - 4.1 Unit vs. Topic
   - 4.2 Topic Ownership vs. Topic Tagging
   - 4.3 Work Item Parentage Rules
5. [Data Model](#5-data-model)
6. [Functional Requirements](#6-functional-requirements)
   - 6.1 Study Structure — Courses and Units
   - 6.2 Study Structure — Topics
   - 6.3 Notes
   - 6.4 Work Item Types
   - 6.5 Methods & Practices
   - 6.6 Progress Tracking
   - 6.7 Calendar & Planning View
   - 6.8 Dashboard
   - 6.9 Topic View
   - 6.10 Scripture Reference Tagging
   - 6.11 Search
   - 6.12 Data Export
7. [Non-Functional Requirements](#7-non-functional-requirements)
8. [Out of Scope](#8-out-of-scope)

---

## 1. Overview

The Theology Study Tracker is a personal, single-user web application for organizing, planning, and tracking self-directed theological study. It supports a hybrid organizational model combining structured course-based study (organized into units) with persistent topical study threads that accumulate knowledge over time. The application runs entirely on the user's local machine with no external dependencies or cloud services.

### Goals

- Provide a unified place to manage courses, units, topics, readings, assignments, papers, and notes
- Support both structured course-based study and freeform topical study through a consistent work item model
- Plan and schedule study through due dates and projected completion dates, surfaced on a calendar
- Track study habits including time spent, completion, and daily streaks
- Capture and organize personal study methods and practices
- Aggregate everything related to a subject in a single Topic View

---

## 2. Deployment Architecture

### Runtime Environment

The application is containerized using **Docker** and run locally via **Docker Compose**. No cloud hosting, authentication service, or external database is required.

### Data Persistence

All application data is stored in a single **SQLite** database file exposed to the host machine via a **Docker bind mount** — a free feature of Docker Community Edition. The database file lives on the user's machine and persists independently of the container lifecycle.

**Example `docker-compose.yml` structure:**

```yaml
services:
  app:
    build: .
    ports:
      - "3000:3000"
    volumes:
      - ./data:/app/data   # bind mount: ./data on host → /app/data in container
```

The SQLite file lives at `./data/theology.db` on the host. Copying that file is a complete backup of all application data.

### Why SQLite

- Single file — easy to back up, inspect, and move
- No separate database container required
- Well-suited to single-user, local-first applications
- Fully supported by Docker bind mounts without any premium features

---

## 3. Scripture Reference Standard

All scripture references follow this format:

```
[3-char book abbreviation] [chapter]:[verse]
```

**Examples:**

| Reference | Meaning |
|---|---|
| `Gen 1:1` | Genesis chapter 1, verse 1 |
| `Rom 8:28` | Romans chapter 8, verse 28 |
| `Jhn 3:16` | John chapter 3, verse 16 |
| `Rev 22:1–5` | Revelation chapter 22, verses 1 through 5 |
| `Psa 119` | Psalm 119 (whole chapter) |

Ranges use an en dash (`–`) between verse numbers. A reference without a verse number implies the entire chapter. The application validates references against a known list of book abbreviations and flags malformed entries at input time.

---

## 4. Core Concepts

### 4.1 Unit vs. Topic

#### Unit (inside a Course)

A **Unit** is a bounded, sequential division within a course. It represents a defined body of work — a book, a subject area, or a phase of study — with a clear beginning and end in the course's scope. Units are the primary organizational structure for tracked, goal-oriented learning.

**Use a Unit when:**
- Working through a structured syllabus or self-designed curriculum
- The study is bounded by the course context
- Order and sequence matter (Unit 1 precedes Unit 2)

**Example — Course: Hermeneutics**
- Unit 1: *How to Read a Book* (Adler)
- Unit 2: *Exegetical Fallacies* (Carson)
- Unit 3: *Biblical Hermeneutics* (Zuck)

#### Topic

A **Topic** is a persistent study subject that accumulates content over time and across contexts. Topics are not bounded by any single course. They serve two roles:

1. **Owner** — a Topic can directly own work items and notes that are not part of any course.
2. **Tagger / Referencer** — any entity (Course, Unit, Work Item, Note, Study Session) can be tagged with one or more Topics, associating it with that subject regardless of where it lives.

**Topics have no status and no progress tracking.** They do not begin or end. The Topic View surfaces an informational count of complete vs. total work items associated with the topic, but this is not a tracked goal.

**Use a Topic to:**
- Organize freeform study not tied to any course
- Aggregate everything ever studied on a subject across courses, sessions, and notes
- Tag course work items so they appear in the relevant Topic View

**Example — Topic: Hermeneutics**
This topic would surface:
- Work items it directly owns (standalone readings, exercises)
- Work items in the Hermeneutics course units tagged with this topic
- Units and courses tagged with this topic
- Study sessions tagged with this topic
- Notes tagged with this topic

### 4.2 Topic Ownership vs. Topic Tagging

These are two distinct relationships between topics and other entities:

**Ownership** applies only to Work Items and Notes. A topic *owns* a work item or note when that item has no Unit parent — the topic is its home. Owned items are created from within the Topic View and are listed there as primary content.

**Tagging** applies to all entities: Courses, Units, Work Items, Notes, and Study Sessions. A tag associates an entity with a topic for aggregation in the Topic View, without changing where the entity lives. A work item in a course unit can be tagged with multiple topics; it still belongs to the unit.

### 4.3 Work Item Parentage Rules

Every work item must satisfy at least one of the following:

- It belongs to a **Unit** (course context), or
- It is **owned by a Topic** (standalone context)

It may satisfy both. A work item in a unit may also be tagged with any number of topics. The application enforces this constraint at creation time: a work item cannot be saved without a unit parent or at least one owning topic.

| Scenario | Unit Parent | Owning Topic | Additional Topic Tags |
|---|---|---|---|
| Course work item, no topic | ✓ | — | Optional |
| Course work item with topics | ✓ | — | ✓ One or more |
| Standalone topic work item | — | ✓ Required | Optional additional |
| Cross-context item | ✓ | ✓ | Optional additional |

---

## 5. Data Model

### Entities

**Course**
- Title
- Description
- Status (Active / Paused / Complete)
- Start date (optional)
- Target completion date (optional)
- Actual completion date (set automatically when marked Complete)
- Created date

**Unit** *(belongs to a Course)*
- Title
- Order within course
- Description
- Target completion date (optional)
- Actual completion date (set automatically when all work items complete)

**Topic**
- Title
- Description
- Type (Book of Bible / Theological Theme / Doctrine / Other)
- Parent topic (optional, one level deep)
- Created date
- *(No status, no target date, no progress tracking)*

**Work Item**
- Type (Reading / Assignment / Paper / Practice Session)
- Title
- Status (Not Started / In Progress / Complete)
- Estimated duration (minutes)
- Due date (optional)
- Completion date (set automatically when marked Complete)
- General notes (freeform plain text)
- Unit parent (nullable — null if owned by a topic)
- Owning topic (nullable — required if no unit parent; see Section 4.3)

**Note**
- Title
- Body (markdown-formatted text)
- Primary parent (exactly one: Course, Unit, Topic, Method, or Study Session)
- Created date
- Last modified date

**Method**
- Name
- Description / personal notes
- Created date

**Study Session**
- Date
- Duration (minutes)
- Associated work item (optional)
- Method used (optional)
- Reflection note (freeform plain text)
- Scripture tags (optional)

**Scripture Tag**
- Reference (formatted per Section 3)
- Associated entity (Work Item, Topic, or Study Session)

### Relationships

```
Course ──< Units ──< Work Items (unit-owned)
Topic ──< Work Items (topic-owned, no unit parent)

Work Items >──< Topics          [many-to-many: tagging]
Notes >──< Topics               [many-to-many: tagging]
Courses >──< Topics             [many-to-many: tagging]
Units >──< Topics               [many-to-many: tagging]
Study Sessions >──< Topics      [many-to-many: tagging]

Notes >──< Work Items           [many-to-many: work item references]
Note ──o Primary Parent         [exactly one of: Course, Unit, Topic, Method, Session]

Study Sessions ──o Work Items   [optional association]
Study Sessions ──o Methods      [optional association]

Scripture Tags ──o Work Items / Topics / Sessions
```

### Note on Topic Relationships

Topics participate in three kinds of relationships:

1. **Ownership of work items** — a work item with no unit parent is owned by a topic (FK on work item table).
2. **Ownership of notes** — a note whose primary parent is a topic is owned by that topic.
3. **Tagging** — any entity can be tagged with any number of topics via a join table (one per entity type: `course_topics`, `unit_topics`, `work_item_topics`, `note_topics`, `session_topics`).

---

## 6. Functional Requirements

### 6.1 Study Structure — Courses and Units

- A course contains one or more **units**, ordered sequentially.
- Each unit contains one or more **work items**.
- Courses have a status: Active, Paused, or Complete.
- Courses have optional start and target completion dates. Marking a course Complete sets the actual completion date automatically.
- Units have an optional target completion date. A unit is automatically marked complete when all its work items are complete; its actual completion date is set at that moment.
- A course displays an overall progress percentage (completed work items / total work items across all units).
- Units can be reordered via drag-and-drop or a manual order field.
- Courses and Units can each be tagged with any number of Topics.

### 6.2 Study Structure — Topics

- A topic has a title, description, type (Book of Bible / Theological Theme / Doctrine / Other), and an optional parent topic (one level deep for subtopics).
- Topics have no status, no target date, and no progress goal. They do not begin or complete.
- Topics display an informational count of complete vs. total work items associated with them (both owned and tagged), for reference only.
- Work items and notes can be created directly within a Topic View, in which case the topic becomes their owner.
- Topics can be tagged onto Courses, Units, Work Items, Notes, and Study Sessions regardless of where those entities live.

### 6.3 Notes

Notes are markdown-formatted documents serving as the primary medium for capturing study content, written work, commentary, and reflections.

#### Note Properties

| Field | Description |
|---|---|
| Title | Short descriptive name |
| Body | Markdown-formatted text, rendered in the UI |
| Primary parent | Exactly one: Course, Unit, Topic, Method, or Session |
| Topic tags | Zero or more Topics (many-to-many, independent of primary parent) |
| Created / Modified | Timestamps, managed automatically |

#### Parent Attachment

- Every note has exactly one primary parent, which determines where it is listed and managed in the UI.
- A note can be moved to a different primary parent via an explicit reassignment action.

#### Topic Tagging on Notes

- Any note can be tagged with any number of Topics, regardless of its primary parent.
- A note parented to a Unit (e.g., lecture notes on Calvin's view of free will) can be tagged with Topics "Free Will," "Salvation," and "Problem of Pain," causing it to surface in all three Topic Views.
- Topic tags on notes are managed from within the note editor via a tag input field.

#### Work Item References (Many-to-Many)

- A note can be referenced by any number of work items of type Assignment, Paper, or Practice Session.
- A work item can reference any number of notes.
- References are managed from the work item view (attach/detach notes by search or browse).
- Deleting a note referenced by work items requires a confirmation warning. References are removed on deletion; work items are not affected.

#### Editor Requirements

- The note editor supports live markdown preview (split-pane or toggle).
- Supported markdown: headings, bold, italic, blockquote, ordered and unordered lists, inline code, code blocks, horizontal rules, links.
- Notes auto-save while editing. A last-saved timestamp is displayed.
- The topic tag input is accessible within the editor view.

#### Note Listing

- Each Course, Unit, Topic, Method, and Session view includes a Notes section listing attached notes with title, last modified date, topic tags, and a one-line content preview.
- Notes can be created, edited, and deleted from within their primary parent's view.

---

### 6.4 Work Item Types

All work items share these common fields:

| Field | Description |
|---|---|
| Title | Short descriptive name |
| Status | Not Started / In Progress / Complete |
| Estimated duration | In minutes |
| Actual time logged | Derived from associated study sessions |
| Due date | Optional target date for completion |
| Completion date | Set automatically when marked Complete |
| General notes | Freeform plain text for brief annotations |
| Scripture tags | Zero or more references |
| Topic tags | Zero or more Topics (many-to-many) |
| Unit parent | The unit this belongs to (null if topic-owned) |
| Owning topic | The topic that owns this item (null if unit-owned; required if no unit parent) |

#### Reading

| Field | Description |
|---|---|
| Source | Book title, article name, or "Scripture" |
| Author | Optional |
| Location | Page range (for books/articles) or scripture reference |
| Format | Physical Book / PDF / Online Article / Scripture |

*Readings do not use the note reference system.*

#### Assignment

| Field | Description |
|---|---|
| Description | What the assignment requires |
| Referenced notes | Zero or more Note entities representing the deliverable |

#### Paper

| Field | Description |
|---|---|
| Prompt or topic | The question or subject being addressed |
| Word count target | Optional |
| Score or grade | Optional, freeform (letter grade, percentage, or narrative) |
| Referenced notes | Zero or more Note entities (outline, draft, final, sections) |

#### Practice Session

| Field | Description |
|---|---|
| Method used | Reference to a Method (see Section 6.5) |
| Scripture passage | The text studied, in standard format |
| Duration | In minutes |
| Referenced notes | Zero or more Note entities representing the written output |

---

### 6.5 Methods & Practices

- The user can create named study methods (e.g., Inductive Bible Study, SOAP Method, Lectio Divina, Grammatical-Historical Analysis).
- Each method has a title, description, created date, and a Notes section for detailed personal notes on applying the method.
- Any Practice Session work item or Study Session can reference a method.
- The application tracks how often each method has been used, displayed as a usage count with a linked session list.
- Methods are not deleted when referenced sessions are deleted; the reference is nullified instead.

---

### 6.6 Progress Tracking

#### Completion

- Every work item has a completion checkbox.
- Marking complete sets the completion date automatically. Un-completing clears it.
- A unit is automatically marked complete when all its work items are complete.
- A course may be manually marked complete; doing so records the actual completion date.

#### Time Logging

- Time is logged via Study Sessions.
- Sessions can be attached to a specific work item or logged free-form.
- Sessions require date and duration in minutes. Method, reflection, and topic tags are optional.
- Total logged time is displayed at the work item, unit, course, and topic level.

#### Study Streaks

- A streak is the number of consecutive calendar days on which at least one study session was logged.
- The current streak and all-time longest streak are both displayed.
- A streak breaks if no session is logged for a full calendar day, based on local system date.

---

### 6.7 Calendar & Planning View

The Calendar is a combined retrospective and planning view. It shows both what has been done and what is scheduled, giving the user a complete picture of past activity and upcoming commitments.

#### Past (Retrospective Layer)

- Study session dates are shown as logged activity, shaded by total minutes (four-level intensity, consistent with the dashboard heatmap).
- Work item completion dates are marked on their respective days.
- Clicking a past day shows all sessions logged and items completed on that date.

#### Future (Planning Layer)

- Due dates for Work Items appear as scheduled markers on their target date.
- Target completion dates for Units appear as milestone markers.
- Target completion dates for Courses appear as milestone markers.
- Overdue items (due date in the past, not yet complete) are visually distinguished from upcoming items.

#### View Modes

- **Month view** — default. Shows the full calendar month with session intensity, completion markers, and due date markers.
- **Week view** — shows a single week in greater detail, listing each work item due and each session logged per day.
- **Agenda view** — a chronological list of upcoming due dates and milestones, starting from today, with no fixed time grid.

#### Interaction

- Clicking a future due date marker navigates to the associated work item, unit, or course.
- Clicking a past completion marker navigates to the associated work item.
- Clicking a past session marker opens the session log for that day.

---

### 6.8 Dashboard

The dashboard is the application's home view.

**Required elements:**

- Current streak and all-time longest streak
- Last session date and duration
- Total time studied this week and this month
- Active courses (up to 5) with title, completion percentage, and target completion date if set
- Active topics (up to 5) with title and associated item count
- Upcoming due dates — next 7 days across all work items, units, and courses
- Recently modified work items (last 5) with direct links
- Recently modified notes (last 5) with direct links
- Calendar heatmap (condensed, trailing 3 months, with future due date indicators)

---

### 6.9 Topic View

Each topic has a dedicated view that aggregates all content associated with it, regardless of where that content lives in the rest of the application.

**The Topic View displays:**

| Section | Contents |
|---|---|
| **Owned Work Items** | Work items for which this topic is the primary owner (no unit parent), grouped by type |
| **Tagged Work Items** | Work items in courses/units that are tagged with this topic |
| **Tagged Courses & Units** | Courses and units tagged with this topic |
| **Tagged Notes** | Notes tagged with this topic, from any primary parent |
| **Tagged Sessions** | Study sessions tagged with this topic |
| **Subtopics** | Child topics (one level deep), each linking to their own Topic View |

**Informational summary (not a tracked goal):**
- Total work items associated (owned + tagged): X complete of Y total
- Total time logged on associated sessions

**Actions available from the Topic View:**
- Create a new work item owned by this topic
- Create a new note owned by this topic
- Tag existing entities with this topic (via search/browse)
- Remove topic tags from entities
- Link to / unlink from a parent topic

---

### 6.10 Scripture Reference Tagging

- Tags are entered in the standard format defined in Section 3.
- The application validates format at entry and rejects malformed references.
- A **Reference View** allows browsing by scripture reference, returning all entities tagged with that reference or within a chapter range.
- Range lookup: entering `Rom 8` returns all items tagged with any verse in Romans 8.

---

### 6.11 Search

A global search bar is accessible from all views.

- Searches across: course titles, unit titles, topic titles and descriptions, work item titles, work item notes, note titles, note body text, session reflections, and scripture tags.
- Results are grouped by type: Courses/Units, Topics, Work Items, Notes, Sessions.
- Scripture reference search supports partial matching: `Rom 8` returns all items tagged with any verse in Romans 8.
- Search is entirely local; no network access required.

---

### 6.12 Data Export

Export is triggered manually from a Settings or Export page.

| Format | Contents | Re-importable |
|---|---|---|
| **JSON** | Full structured export of all data and relationships, including all topic tags and many-to-many joins | Yes |
| **Markdown** | All notes organized by primary parent entity, plus session reflections | No |

- JSON export must be fully round-trip re-importable, restoring all data and relationships including topic tags, note references, and session associations.
- No data is sent to any external service during export.

---

## 7. Non-Functional Requirements

| Requirement | Specification |
|---|---|
| **Single user** | No authentication, user accounts, or multi-tenancy |
| **Local only** | No network requests except to serve the application within Docker |
| **Offline capable** | Fully functional without an internet connection |
| **Data ownership** | All data lives on the user's machine in a bind-mounted SQLite file |
| **Backup** | Copying `./data/theology.db` constitutes a complete backup |
| **Performance** | All views load in under 500ms on a standard local machine |
| **Platform** | Runs on any machine with Docker Desktop (Community Edition) installed |

---

## 8. Out of Scope

| Feature | Reason for Exclusion |
|---|---|
| Cloud sync / user accounts | Single-user, local-first design; adds complexity with no benefit |
| AI-generated commentary or study content | Pulls focus away from primary sources and personal methods; theological reliability is a concern |
| Bible reading plan generator | Separate concern; the course/unit structure already handles what to read |
| Sermon or teaching prep tools | Different workflow and purpose from personal study tracking |
| Gamification (badges, XP, leaderboards) | Trivializes the subject matter; streaks are sufficient habit reinforcement |
| Social sharing | Out of scope for a personal tool |
| Church management features | Entirely different application domain |

---

*End of Specification — v1.3*
