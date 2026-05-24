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

    // ── Lucide icons ───────────────────────────────────────────────────────
    if (typeof lucide !== 'undefined') lucide.createIcons();

    // Re-enable sidebar transitions after first paint (prevents flash on page load)
    requestAnimationFrame(() => requestAnimationFrame(() => {
        document.documentElement.classList.remove('sidebar-no-transition');
    }));

    // ── Dark mode toggle ───────────────────────────────────────────────────
    const themeToggle = document.getElementById('theme-toggle');
    themeToggle?.addEventListener('click', () => {
        const html = document.documentElement;
        const isDark = html.getAttribute('data-theme') === 'dark';
        if (isDark) {
            html.removeAttribute('data-theme');
            localStorage.setItem('theme', 'light');
        } else {
            html.setAttribute('data-theme', 'dark');
            localStorage.setItem('theme', 'dark');
        }
    });

    // ── Mobile sidebar toggle ──────────────────────────────────────────────
    const sidebar  = document.getElementById('sidebar');
    const overlay  = document.getElementById('sidebar-overlay');
    const openBtn  = document.getElementById('sidebar-open');
    const closeBtn = document.getElementById('sidebar-close');

    function openSidebar() {
        sidebar?.classList.add('open');
        overlay?.classList.add('open');
        document.body.style.overflow = 'hidden';
    }

    function closeSidebar() {
        sidebar?.classList.remove('open');
        overlay?.classList.remove('open');
        document.body.style.overflow = '';
    }

    openBtn?.addEventListener('click', openSidebar);
    closeBtn?.addEventListener('click', closeSidebar);
    overlay?.addEventListener('click', closeSidebar);

    // ── Sidebar collapse (desktop only) ───────────────────────────────────
    const collapseBtn = document.getElementById('sidebar-collapse');

    collapseBtn?.addEventListener('click', () => {
        const collapsed = document.documentElement.classList.toggle('sidebar-collapsed');
        localStorage.setItem('sidebarCollapsed', collapsed ? 'true' : 'false');
    });

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
        if (typeof lucide !== 'undefined') lucide.createIcons();
        initSortable();
        initMarkdownPreview();
    });

    // ── Markdown preview ───────────────────────────────────────────────────
    initMarkdownPreview();

    // ── Scripture reference validation ────────────────────────────────────
    initScriptureValidation();

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

// ── Scripture reference validation ────────────────────────────────────────

const SCRIPTURE_BOOKS = new Set([
    'Gen','Exo','Lev','Num','Deu','Jos','Jdg','Rut','1Sa','2Sa',
    '1Ki','2Ki','1Ch','2Ch','Ezr','Neh','Est','Job','Psa','Pro',
    'Ecc','Son','Isa','Jer','Lam','Eze','Dan','Hos','Joe','Amo',
    'Oba','Jon','Mic','Nah','Hab','Zep','Hag','Zec','Mal',
    'Mat','Mrk','Luk','Jhn','Act','Rom','1Co','2Co','Gal','Eph',
    'Phi','Col','1Th','2Th','1Ti','2Ti','Tit','Phm','Heb','Jam',
    '1Pe','2Pe','1Jo','2Jo','3Jo','Jud','Rev'
]);

const SCRIPTURE_REF_RE = /^([1-9]?[A-Z][a-z]{1,2}) (\d+)(?::(\d+)(?:[–\-](\d+))?)?$/;

/**
 * Validates a scripture reference input and applies visual feedback.
 * @param {HTMLInputElement} input
 * @returns {boolean}
 */
function validateScriptureRef(input) {
    const val = input.value.trim();
    const icon = input.parentElement?.querySelector('.scripture-validation-icon');
    if (!val) {
        input.classList.remove('input-valid', 'input-invalid');
        if (icon) icon.textContent = '';
        return true;
    }
    const m = SCRIPTURE_REF_RE.exec(val);
    const valid = m !== null && SCRIPTURE_BOOKS.has(m[1]);
    input.classList.toggle('input-valid', valid);
    input.classList.toggle('input-invalid', !valid);
    if (icon) icon.textContent = valid ? '✓' : '✕';
    if (icon) icon.style.color = valid ? 'var(--color-success, #16a34a)' : 'var(--color-danger, #dc2626)';
    return valid;
}

/** Adds a new scripture tag input row to the container. */
function addScriptureTagRow() {
    const container = document.getElementById('scripture-tags-container');
    if (!container) return;
    const row = document.createElement('div');
    row.className = 'scripture-tag-row d-flex gap-2 align-center';
    row.style.marginBottom = '0.5rem';
    row.innerHTML = `
        <input type="text" name="scriptureTags"
               class="form-control scripture-ref-input"
               placeholder="e.g. Gen 1:1"
               autocomplete="off"/>
        <span class="scripture-validation-icon" aria-hidden="true"></span>
        <button type="button" class="btn btn-secondary btn-sm"
                onclick="removeScriptureTag(this)" title="Remove">✕</button>
    `;
    container.appendChild(row);
    const input = row.querySelector('input');
    input.addEventListener('blur', () => validateScriptureRef(input));
    input.addEventListener('input', () => validateScriptureRef(input));
    input.focus();
}

/** Removes the scripture tag row containing the given button. */
function removeScriptureTag(btn) {
    const container = document.getElementById('scripture-tags-container');
    const row = btn.closest('.scripture-tag-row');
    if (!row) return;
    // Keep at least one empty row
    const rows = container.querySelectorAll('.scripture-tag-row');
    if (rows.length === 1) {
        const input = row.querySelector('input');
        if (input) { input.value = ''; validateScriptureRef(input); }
        return;
    }
    row.remove();
}

/** Wire validation onto all existing scripture ref inputs on the page. */
function initScriptureValidation() {
    document.querySelectorAll('.scripture-ref-input:not([data-scripture-wired])').forEach(input => {
        input.dataset.scriptureWired = 'true';
        input.addEventListener('blur', () => validateScriptureRef(input));
        input.addEventListener('input', () => validateScriptureRef(input));
        if (input.value.trim()) validateScriptureRef(input);
    });
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
