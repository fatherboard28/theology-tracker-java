/**
 * Theology Study Tracker — main.js
 *
 * Responsibilities:
 *  - Configure HTMX defaults (history, error handling)
 *  - Initialize Sortable.js on drag-and-drop lists after HTMX swaps
 *  - Set up live markdown preview for note editor
 *  - Auto-dismiss flash messages
 *  - Confirm dialog helper for destructive actions
 */

document.addEventListener('DOMContentLoaded', () => {

    // ── HTMX configuration ─────────────────────────────────────────────────
    if (typeof htmx !== 'undefined') {
        htmx.config.defaultSwapStyle = 'outerHTML';
        htmx.config.historyCacheSize = 10;
        htmx.config.scrollBehavior = 'smooth';

        // Global error handling — show a flash if a server error occurs
        document.body.addEventListener('htmx:responseError', (evt) => {
            console.error('HTMX request failed:', evt.detail);
            showFlash('An error occurred. Please try again.', 'error');
        });
    }

    // ── Sortable.js — unit drag-and-drop reorder ───────────────────────────
    initSortable();

    // Re-initialize after any HTMX swap that may have added new sortable lists
    document.body.addEventListener('htmx:afterSwap', () => {
        initSortable();
        initMarkdownPreview();
    });

    // ── Markdown preview ───────────────────────────────────────────────────
    initMarkdownPreview();

    // ── Auto-dismiss flash messages after 5 s ─────────────────────────────
    document.querySelectorAll('.flash').forEach((el) => {
        setTimeout(() => {
            el.style.transition = 'opacity 500ms ease';
            el.style.opacity = '0';
            setTimeout(() => el.remove(), 500);
        }, 5000);
    });

    // ── Confirm delete helpers ─────────────────────────────────────────────
    // Elements with data-confirm="message" prompt before form submit/link follow
    document.addEventListener('click', (evt) => {
        const el = evt.target.closest('[data-confirm]');
        if (!el) return;
        const msg = el.dataset.confirm || 'Are you sure?';
        if (!window.confirm(msg)) {
            evt.preventDefault();
            evt.stopPropagation();
        }
    });
});

/**
 * Initialize Sortable.js on all elements with class `sortable-list`.
 * After a drag, posts the new order to the URL in data-sort-url.
 */
function initSortable() {
    if (typeof Sortable === 'undefined') return;

    document.querySelectorAll('.sortable-list:not([data-sortable-init])').forEach((list) => {
        list.dataset.sortableInit = 'true';
        Sortable.create(list, {
            handle: '.drag-handle',
            animation: 150,
            ghostClass: 'sortable-ghost',
            onEnd(evt) {
                const sortUrl = list.dataset.sortUrl;
                if (!sortUrl) return;

                // Collect ordered IDs
                const ids = [...list.querySelectorAll('[data-id]')].map(el => el.dataset.id);

                // POST new order via HTMX fetch
                htmx.ajax('POST', sortUrl, {
                    target: list,
                    swap: 'none',
                    values: { ids: ids.join(',') }
                });
            }
        });
    });
}

/**
 * Wire up markdown live preview for note editors.
 * Expects:
 *   <textarea id="note-body-editor" ...>
 *   <div id="note-body-preview" class="markdown-preview">
 */
function initMarkdownPreview() {
    if (typeof marked === 'undefined') return;

    const editor  = document.getElementById('note-body-editor');
    const preview = document.getElementById('note-body-preview');
    if (!editor || !preview) return;
    if (editor._previewWired) return; // already wired
    editor._previewWired = true;

    // Render on load
    renderMarkdown(editor, preview);

    // Render on input
    editor.addEventListener('input', () => renderMarkdown(editor, preview));

    // Tab insertion in editor
    editor.addEventListener('keydown', (evt) => {
        if (evt.key === 'Tab') {
            evt.preventDefault();
            const start = editor.selectionStart;
            const end   = editor.selectionEnd;
            editor.value = editor.value.substring(0, start) + '    ' + editor.value.substring(end);
            editor.selectionStart = editor.selectionEnd = start + 4;
            renderMarkdown(editor, preview);
        }
    });
}

function renderMarkdown(editor, preview) {
    try {
        preview.innerHTML = marked.parse(editor.value || '');
    } catch (e) {
        console.warn('Markdown render error:', e);
    }
}

/**
 * Display a flash message programmatically.
 * @param {string} message
 * @param {'success'|'error'} type
 */
function showFlash(message, type = 'success') {
    let container = document.querySelector('.flash-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'flash-container';
        document.querySelector('.main-content')?.prepend(container);
    }

    const el = document.createElement('div');
    el.className = `flash flash-${type}`;
    el.textContent = message;
    container.appendChild(el);

    setTimeout(() => {
        el.style.transition = 'opacity 500ms ease';
        el.style.opacity = '0';
        setTimeout(() => el.remove(), 500);
    }, 5000);
}
