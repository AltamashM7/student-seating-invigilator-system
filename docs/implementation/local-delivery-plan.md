# Local S1-S6 delivery plan

Baseline: GitHub main 973df5d7005961e57b31583b6b7808d411b12e6e. Work only in this workspace. Do not push.

1. Academic structure: reusable course catalog; academic-year course offerings; division defined by academic year, department and study year; student placement retained per academic year. Course year follows semesters 1-2/3-4/5-6/7-8. Counts use distinct enrolled students, including empty divisions.
2. Administration: CRUD for years, departments, divisions, students, faculty, rooms and courses; manage annual enrollments and course offerings. Prevent deleting referenced data. Preserve existing user authentication.
3. Existing fixes: safe rendering, CSV header/physical-line validation, enroll existing students into additional courses, persistent room IDs, useful server logs and controlled invalid input; atomic exam mutations and lifecycle rules.
4. Seating: deterministic roll-number ordering and room-number/seat-number ordering; real seat row/column coordinates; standard full-capacity layout or optional horizontal separation; all-or-nothing allocation on insufficient usable capacity; one student/seat per exam.
5. Accommodation: students needing accessible seats are placed first; accessible-room and seat flags. Overrides must satisfy the same constraints; actor/timestamp/history recorded. Regeneration preserves manual assignments by default, with explicit confirmation to replace them.
6. Sample data: 240 fictional students across 3 departments, 4 years, 2 divisions (10 per division), multiple course enrollments, faculty, rooms, exams, accommodation cases. Never modify the existing host database.
7. Verification: pure Java worked-example/edge/load algorithm tests, disposable MySQL/Tomcat integration checks, browser checks, KPI-to-test evidence with actual results. No unsupported claim of passing.
8. Delivery: complete local project, documented upgrade SQL, runtime/setup guide, 12 member ZIPs grouped into four teams based on non-overlapping file ownership, ordered integration steps and integrity manifests. Members review/test and commit their owned changes.
