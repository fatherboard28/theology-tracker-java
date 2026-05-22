# Apple Design Redesign — Task List
## Foundation
Task 1 — Replace the CSS design-token system in main.css
Replace the current :root block (Inter font, indigo/violet brand, gray-* scale, sidebar-*, legacy alias variables) with the Apple design system token set: --bg-primary, --bg-secondary, --text-primary, --text-secondary, --accent (#0071E3), --border (#D2D2D7), and --error/--success (Apple red/green). Add the [data-theme="dark"] counterpart block immediately after. Update --font-sans to the system font stack (-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif) and remove the Google Fonts @import for Inter. Update all downstream CSS that references the old token names so nothing breaks.

Task 2 — Enforce the 8pt spacing scale and update the typography scale in main.css
Replace all hardcoded px padding/margin/gap values in main.css with values that are strict multiples of 8 (4, 8, 16, 24, 40, 80px). Update the --text-* size variables to match the Apple type scale: body 17px, secondary/caption 13–15px, label 11–12px, section title 28–40px. Set line-height: 1.5 on body copy rules and line-height: 1.1 on all heading rules. Add letter-spacing: -0.02em to any heading selector at 28px or larger.

## Navigation & Shell
Task 3 — Redesign the sidebar in main.css and base.html
Change the sidebar background from #0f1117 (dark) to var(--bg-secondary) (#F5F5F7 light / #1C1C1E dark). Remove the indigo gradient on the brand logo area. Update sidebar nav links to plain text with no background pill on hover — hover changes text color to var(--accent) only. Active link: text color var(--accent), font-weight 600, no background box. Set sidebar border-right to 1px solid var(--border). Update all sidebar-specific token references in main.css to use the new design tokens.

Task 4 — Apply frosted glass effect to the topbar in main.css and base.html
Replace the topbar's solid background with backdrop-filter: blur(20px) saturate(180%) and background: rgba(255,255,255,0.72) (dark mode: rgba(28,28,30,0.72)). Set position: sticky; top: 0; z-index: 100. Replace the bottom border with border-bottom: 1px solid rgba(0,0,0,0.08). Remove any box-shadow on the topbar. Ensure the topbar uses var(--text-primary) for all text.

## Components
Task 5 — Redesign all buttons in main.css
Update .btn-primary to: background: var(--accent), color: #fff, border-radius: 980px, padding: 12px 24px, font-size: 17px, font-weight: 400, border: none, transition: background 0.2s ease, transform 0.1s ease, hover background: #0077ED, active transform: scale(0.97). No box-shadow. Update .btn-secondary to: background: transparent, color: var(--accent), border: 1.5px solid var(--accent), border-radius: 980px, padding: 11px 24px. Remove any other button variants that don't fit these two patterns or convert them to text links (.btn-link).

Task 6 — Redesign card and surface styles in main.css
Set all .card variants to: background: var(--bg-secondary), border-radius: 18px, padding: 32px, border: none, box-shadow: 0 2px 8px rgba(0,0,0,0.08). Remove any heavy drop shadows, gradient backgrounds, or colored borders on cards (including the dashboard stat cards which currently use indigo/teal/rose gradients — replace with the flat var(--bg-secondary) surface and show the stat number in var(--text-primary)). Hero/large cards use border-radius: 28px, small inline chips use border-radius: 12px.

Task 7 — Redesign form inputs in main.css and all form templates
Update all input, select, and textarea elements to: border: 1px solid var(--border), border-radius: 12px, padding: 12px 16px, font-size: 17px, background: var(--bg-primary), color: var(--text-primary), outline: none, transition: border-color 0.2s ease, box-shadow 0.2s ease. Focus state: border-color: var(--accent), box-shadow: 0 0 0 3px rgba(0,113,227,0.25). In every Thymeleaf form template (courses/form.html, topics/form.html, notes/form.html, and all others), ensure every th:errors output renders as inline text directly below its field in var(--error) (#FF3B30) at 13px — not inside an alert box. Disabled fields: opacity: 0.4, cursor: not-allowed.

Task 8 — Redesign tables in main.css
Replace the current table styles with: border-collapse: collapse, width: 100%. th: font-size: 13px, font-weight: 600, color: var(--text-secondary), text-transform: uppercase, letter-spacing: 0.06em, padding: 8px 16px, border-bottom: 1px solid var(--border), text-align: left. td: padding: 14px 16px, border-bottom: 1px solid var(--border), font-size: 15px, color: var(--text-primary). Remove zebra striping. Last row td: border-bottom: none. tbody tr:hover: background: var(--bg-secondary).

Task 9 — Replace flash message / alert styling in main.css and base.html
Remove the current Bootstrap-style filled alert boxes from both the CSS and the alert markup in base.html. Replace with a minimal banner: background: transparent, border-left: 4px solid var(--success) for success (Apple green #34C759) or border-left: 4px solid var(--error) for error (Apple red #FF3B30), padding: 12px 16px, font-size: 15px, color: var(--text-primary). No bold text, no icons. The auto-dismiss JS in main.js can stay as-is.

## Icons
Task 10 — Replace existing icons with Lucide Icons in base.html and all templates
Add the Lucide Icons CDN script tag to base.html (the npm-free ESM CDN version). Audit every template for icon usage (Font Awesome classes, inline SVGs, emoji icons, or Unicode symbols used as icons) and replace each one with the equivalent Lucide icon using their web component syntax (<i data-lucide="icon-name"></i> and call lucide.createIcons() once in main.js after page load). Icon size: 20×20px when inline with text, 24×24px standalone. Stroke color should inherit from text color (currentColor). Remove any icon that is purely decorative with no semantic meaning.

## Motion & Accessibility
Task 11 — Add CSS transitions, reveal animations, and prefers-reduced-motion handling to main.css
Add a global default transition rule for all interactive elements: transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1). Add the @keyframes fadeUp animation (from: opacity 0, translateY 16px → to: opacity 1, translateY 0) and a .reveal utility class that applies it with 0.4s duration and the same easing. Apply .reveal to the main content grid wrappers in the dashboard, courses, topics, and sessions templates so content fades up on load. At the bottom of main.css, add the @media (prefers-reduced-motion: reduce) block that sets * { transition: none !important; animation: none !important; }.

## Dark Mode
Task 12 — Add a dark mode toggle to base.html and main.js
Add a toggle button in the topbar in base.html (use the Lucide sun/moon icon, no label). In main.js, add an event listener on that button that toggles [data-theme="dark"] on the <html> element and persists the preference in localStorage (key: "theme"). On page load, read localStorage and apply the attribute before first paint (add a small <script> in <head> in base.html to avoid flash). The CSS custom properties from Task 1 handle all color changes automatically.

## Page-Level Updates
Task 13 — Redesign the dashboard page (dashboard/index.html)
Update the 4-stat card grid: remove gradient backgrounds, use flat var(--bg-secondary) cards (border-radius: 18px), display the stat number at 40px weight-700 in var(--text-primary), and the label at 13px in var(--text-secondary). Update the activity heatmap to use opacity steps of var(--accent) for intensity levels instead of the current hardcoded green shades. Apply class="reveal" to the stat grid, the active-courses section, and the recent-work section so they animate in. Ensure all spacing between sections is a multiple of 8px.

Task 14 — Redesign the error page (error/error.html)
Replace the current error layout with a centered, minimal design: the HTTP status code displayed at 80px, font-weight: 700, color: var(--text-secondary) (large, subtle). Below it, a short human-readable sentence at 17px in var(--text-primary). A single pill-shaped .btn-primary linking back to /dashboard. No stack trace visible to users. The page should use the same base.html layout so the nav and tokens apply automatically.

Task 15 — Audit every remaining template for token and component compliance
Go through each of the following templates and update any hardcoded colors, non-8pt spacings, old button classes, bordered cards, or un-styled th:errors outputs to use the new design system: calendar/index.html, search/index.html, settings/index.html, courses/index.html, notes/index.html, notes/form.html, sessions/index.html, scripture/index.html, methods/ templates, work-items/ templates, topics/tag-select-fragment.html, and scripture/tag-input-fragment.html. Confirm the Apple design checklist passes on each: CSS custom properties only, 8pt grid, accent color on interactive elements only, no card borders, nothing below 13px, visible focus states, and styled th:errors outputs.
