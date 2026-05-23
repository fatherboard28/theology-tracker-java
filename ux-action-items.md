# UX Action Items

## Section 1 — Topic Page

### 1.1 Replace the single-column scroll layout with a two-panel notes-first layout

In `src/main/resources/templates/topics/show.html`, split the page body into two panels:
a left panel (~280px, scrollable) listing all notes for this topic, and a right content
panel showing the selected note's body rendered as markdown. Keep the existing page header
(title, badge, description, edit/delete buttons) above both panels. This is the structural
change that all other topic items depend on.

---

### 1.2 Merge "Owned Notes" and "Tagged Notes" into one unified list in the left panel

The owned/tagged distinction is an architectural detail, not something the user needs to
see while browsing. In `topics/show.html`, combine both lists (currently rendered in
separate sections) into a single flat list sorted by `updatedAt` descending. If origin
context is useful, add a subtle muted label ("tagged") only on cross-tagged notes — not
a full section header.

---

### 1.3 Implement client-side note preview via AJAX, not an embedded data blob

Do not dump all note bodies into the initial HTML payload. Instead:

- Add a new endpoint `GET /notes/{id}/preview` to `NoteController` that returns a plain
  HTML fragment containing just the rendered note body (render the markdown server-side
  using a lightweight parser, or return the raw body and let the client render it via
  `marked.parse()` which is already available in the app).
- In `topics/show.html`, wire a JavaScript click handler on each note row that fetches
  `/notes/{id}/preview` and injects the result into the right panel, also updating the
  panel's title and last-modified date from the note row's data attributes.
- Keep an "Open" link in the preview panel that navigates to `/notes/{id}` for full editing.

This keeps initial page load lean and is consistent with how this should scale as notes grow.

---

### 1.4 Add a star/favorite toggle to notes

**Schema:** Create a new Flyway migration
`src/main/resources/db/migration/V2__add_note_starred.sql` with:

```sql
ALTER TABLE notes ADD COLUMN starred BOOLEAN NOT NULL DEFAULT FALSE;
```

**Entity:** Add `private boolean starred = false;` to `Note.java`.

**Service:** Add `NoteService.toggleStar(Long id)` that flips the field and saves.

**Controller:** Add `POST /notes/{id}/star` to `NoteController` that calls the service
method and redirects back (accept a `returnUrl` param like the complete-toggle pattern
already used in `WorkItemController`).

**Template:** In `topics/show.html`, render a ☆/★ button on each row in the left note
panel that posts to `/notes/{id}/star`. Sort the unified note list: starred first, then
by `updatedAt` descending — apply the sort in `TopicController.show()` after merging
the two lists.

---

### 1.5 Add a prominent "New Note" button at the top of the left notes panel

The route `/notes/new?parentType=TOPIC&parentId={id}` already works. In
`topics/show.html`, place a "New Note" button at the top of the left notes panel as the
primary CTA — not after the list as it is today.

---

### 1.6 Collapse all non-note content into a secondary disclosure section

In `topics/show.html`, wrap the following sections in a `<details>` element (default
closed):

- Owned Work Items
- Tagged Work Items
- Tagged Courses & Units
- Tagged Sessions

The summary card (work item count, time logged, parent topic) should shrink to a single
muted stat line directly under the topic description in the page header, not a full card.

**For subtopics:** They are navigational, not content. Rather than a full section, render
subtopics as chip links inline in the header area below the topic description. They are
currently only shown when the topic is a root topic, so this change should maintain that
condition.

---

## Section 2 — Courses / Units

### 2.1 Compress the course meta card into a header stat bar in `courses/show.html`

Remove the 3-column grid card (Progress / Dates / Status change buttons). Replace with:

- A slim progress bar directly under the course title inside `.page-header`
- A single muted line for dates: "Started Jan 5 · Target Jun 1"
- Status change buttons moved to the edit form or a small gear dropdown — they do not
  need to be primary actions on the show page

---

### 2.2 Move topic tags inline in the page header (remove the separate section)

In `courses/show.html`, render the topic chip row directly under the course
subtitle/description in the header. Remove the `<h2>Topic Tags</h2>` section header and
the "Manage Tags" button. Replace the button with a subtle "edit tags" link inline next
to the chips that goes to the edit form.

---

### 2.3 Collapse unit work items behind a count summary by default

In `courses/show.html`, each unit currently renders all its work items expanded inline.
For courses with multiple units this becomes very long. Instead, show each unit with its
title, description, and topic tags, and collapse its work item list behind an
Alpine.js-powered `x-show` toggle labeled "N work items · X complete." The drag-to-reorder
handle, the "Add Work Item" dropdown, and the individual work item rows all stay as they
are inside the expanded state — just hidden by default.

---

## Section 3 — Work Items

### 3.1 Restructure `work-items/show.html` to match the priority order

Eliminate the current 3-column meta card entirely and redistribute its components per the
priority order below. The target reading order of the page:

1. **Title + Due date** — due date as a styled chip next to the title (see item 3.2)
2. **Type-specific details** — the Reading details / Assignment description / Paper prompt
   / Practice Session details card, moved up immediately after the header
3. **Notes** — the renamed Attachments section (see items 3.3, 3.4, 3.5)
4. **Topic tags** — compact chip row, no section header
5. **Status** — slim "○ Mark Complete / ✓ Mark Incomplete" toggle row
6. **Log Session** — standalone CTA button (see item 3.6)
7. **Edit / Delete** — moved to the bottom of the page (see item 3.7)

---

### 3.2 Move the due date from the meta card into the page header

In `work-items/show.html`, render the due date as a styled chip directly next to or below
the title (e.g., "Due May 30"). If the due date is in the past and the item is incomplete,
apply a warning color class. Remove the "Schedule" row from the meta card. The estimated
duration field can either be dropped from the show page or appended as a muted detail
inside the type-specific details card.

---

### 3.3 Rename "Attachments" to "Notes" in `work-items/show.html`

Change the `<h2>Attachments</h2>` heading in the referenced-notes card to `<h2>Notes</h2>`.
This communicates that these notes are the content that completes the work item, not file
attachments.

---

### 3.4 Add a "New Note" button inside the Notes section of `work-items/show.html`

Add a primary button in the Notes section:

```html
<a th:href="@{/notes/new(parentType='WORK_ITEM',parentId=${item.id})}"
   class="btn btn-primary btn-sm">+ New Note</a>
```

The route already exists in `NoteController`. No server-side changes needed for creation.
However, this button alone is not enough — item 3.5 is required to make the created note
appear on this page.

---

### 3.5 Load "owned" work item notes in `WorkItemController.show()` and display them

**The problem:** `WorkItemController.show()` (around line 114) currently only adds
`item.notes` to the model — the `workItemRefs` many-to-many set, which contains notes
explicitly attached via the attach/detach UI. Notes created via
`/notes/new?parentType=WORK_ITEM&parentId={id}` have `primaryParentType=WORK_ITEM` but
are NOT in `item.notes`. They are invisible on the work item show page today.

**Fix:**

In `WorkItemController.show()`, add:

```java
List<Note> ownedNotes = noteService.findByParent(NoteParentType.WORK_ITEM, id);
model.addAttribute("ownedNotes", ownedNotes);
```

In `work-items/show.html`, in the Notes section, display both `ownedNotes` and
`item.notes` (workItemRefs) merged into one list, de-duplicated by note ID. Items 3.4
and 3.5 must be implemented together for "New Note" to work end-to-end.

---

### 3.6 Promote "Log Session" to a standalone CTA and collapse session history

In `work-items/show.html`:

- Pull the `+ Log Session` link out of the Study Sessions section header and place it as
  a standalone button after the Status toggle (priority position #6).
- Wrap the session history list in a `<details>` element labeled
  "Session History (N sessions logged)" so it is collapsed by default.

---

### 3.7 Move Edit and Delete buttons to the bottom of `work-items/show.html`

Currently Edit and Delete are the first interactive elements after the title — visually
making them look like primary actions. Per the priority order they are #7. Move them to
a small action row at the very bottom of the page content, or replace the top-right
button group with a compact `...` icon menu (a small Alpine.js dropdown) to keep them
accessible but out of the primary visual path.

---

## Cross-Cutting Notes

**Flyway is required for any schema change.** The app uses `ddl-auto=none` and Flyway
migrations. The next migration file must be
`src/main/resources/db/migration/V2__<description>.sql`. Any entity field additions
(e.g., `starred`) without a corresponding migration will cause startup errors.

**The two note-relationship models on work items.** A note can relate to a work item in
two distinct ways: (1) `primaryParentType=WORK_ITEM` — the note was created from/for this
work item, and (2) `workItemRefs` — a note created elsewhere that is cross-referenced to
this work item. Items 3.3–3.5 treat both as "notes for this work item" and display them
merged. No model refactoring is needed; just load both and de-duplicate by ID in the
template or controller.

**`marked.js` is already available globally** from the existing note editor
(`notes/show.html`). The topic preview pane (item 1.3) can use it client-side if the
`/notes/{id}/preview` endpoint returns raw markdown rather than rendered HTML. Confirm
it is loaded in `layout/base.html` before relying on it outside of the notes page.
