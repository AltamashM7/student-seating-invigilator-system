# Student portal and usability refresh — 4 October 2026

The default route now opens the student portal, with staff login in the header corner. Exact-roll lookup returns student/course/room/seat cards for published exams. Upcoming exam search, the full timetable and print actions are available without login.

Staff management lists share search, pagination, sorting and academic-year selection. Cohort screens add department/study-year filters. Large selectors support option search and retain the current selection. Exams replaces room multi-select with searchable checkbox cards and capacity feedback. Seating provides student/target search, room filtering and a chart search that retains physical geometry.

Shared `app-ui.js` provides navigation, list, feedback, busy-state, date/time and selector helpers. Page controllers retain domain behavior. `refresh-ui-pages.py` generates consistent HTML shells. Collapsible editors, row actions and advanced panels reduce clutter while preserving CRUD, exam lifecycle, import, accessibility, overrides, regeneration and history. Versioned assets prevent stale scripts after deployment.

The responsive interface uses navy/slate with muted teal, visible focus, labels, skip links, current-page markers, reduced-motion styles and print layouts. Mobile navigation opens and closes explicitly; room charts scroll inside their container. Password visibility is a labelled button.

The regenerated data contains 240 fictional students and 12 faculty with realistic names, excluding the requested surname. The evaluator CSV has 12 new students across 24 workshop enrollments. It is available as a source file and page download; preview and result summaries explain the effect.

Actual browser observations are summarized in `docs/testing/evidence/ui-refresh-results.json`; student landing and import screenshots accompany them. These observations precede final small fixes to selector help, password label, selected-student preservation and versioned assets. The final WAR build succeeded, but browser security policy blocked reconnecting after resumption, so the final visual recheck was not completed. Source syntax/reference checks cover those edits. No backend behavior changed in this refresh.

## Official wording and alignment follow-up

Replaced promotional headings and personal greetings with functional headings such as Examination Seating and Staff Login. Study-year labels now use FY, SY, TY and BE throughout filters, course/division selectors, editors and count tables; stored values remain 1–4. Sorting by study year retains academic order. Toolbars align actions with field bottoms, checkbox fields align with adjacent inputs, and headings wrap on narrow screens. Generated pages use a new asset version. Source checks and focused label/render checks passed; the existing browser policy limitation still prevents claiming a new visual recheck.
