# Core Requirements: Exam Seating and Invigilation Planner

**Scope:** complete functional project, including accepted seating refinements and the remaining invigilation, conflict, reporting and notification modules

**Last updated:** 18 October 2026

## 1. Purpose

The system manages academic records, examination schedules and student seating. It lets administrators maintain academic data, import enrollments, schedule exams and generate or adjust seating. Students can view published schedules and look up published seating by roll number.

This document describes the accepted current scope. The earlier [S1–S4 scope](s1-s4-scope.md) records the first evaluation and should be read as historical context where it differs from this document.

## 2. Users

- **Administrator:** manages academic records, accounts, rooms, courses, enrollments, exams and seating.
- **Faculty:** signs in, records unavailable times, views their own duties, accepts or declines scheduled duties, reads notifications and downloads their duty list.
- **Student/public user:** views the published timetable and searches published seating by roll number and academic year. A student account is not required.

## 3. Functional requirements

### FR-1. Accounts and access

- Administrators and faculty sign in and out with username and password.
- Administrator pages and operations require an administrator session. Faculty access is restricted to faculty functions.
- Only a signed-in administrator can create administrator or faculty accounts or reset an account password.
- Account names, usernames, email addresses and faculty employee codes are validated for required format and uniqueness.

### FR-2. Academic years and student cohorts

- The system stores academic years separately from study years. An academic year is labelled, for example, `2026-27`; study years are displayed as **FY**, **SY**, **TY** and **BE**.
- Departments have divisions for a particular academic year and study year.
- Each student can have one division and semester placement per academic year. Promotion adds or updates a placement while retaining earlier placements.
- Dashboard counts show students by department and study year, then by division. Counts use annual student placements rather than course-enrollment rows. Departments and divisions with zero students remain visible.

### FR-3. Academic record management

- Administrators can create, view, update and delete academic years, departments, divisions, students, faculty, rooms, courses and course enrollments, subject to reference and history safeguards.
- Faculty records manage their associated staff login account.
- Courses belong to a department and study year/semester. Course offerings connect catalog courses to academic years.
- Student enrollments must match the student's annual placement, semester and course offering. Duplicate student/course/year enrollments are prevented.
- Referenced or historically used records cannot be deleted or structurally changed when doing so would invalidate existing exams, enrollments or seating.

### FR-4. Student CSV import

- Administrators can import student and course-enrollment rows from a CSV file.
- The current header is `roll_number,name,email,department_code,semester,course_code,academic_year,division,requires_accessible`.
- The import reports new students, additional enrollments, duplicates and invalid rows. A known student may be enrolled in another eligible course without creating a duplicate student.
- Imported rows must reference valid academic years, departments, divisions and course offerings.

### FR-5. Examination scheduling

- Administrators can create, view, update and delete exams and manage their status.
- An exam has an academic year, course offering, date, start and end times, one or more rooms, a status and an optional horizontal-spacing rule.
- The end time must be later than the start time. Overlapping non-cancelled exams cannot share a room or students enrolled in the affected courses. Exams that only touch at an endpoint do not overlap.
- Exam states follow `DRAFT → SCHEDULED` or `CANCELLED`, then `SCHEDULED → COMPLETED` or `CANCELLED`. Completed and cancelled exams cannot be reopened. Exam details become read-only after seating history is created.
- Only scheduled or completed exams are exposed on public timetable and seating lookup pages.

### FR-6. Seating generation

- Administrators can generate seating for draft or scheduled exams using that exam's annual course roster and selected rooms.
- Every student must receive exactly one unique seat in the exam. A seat cannot be assigned to two students in the same exam.
- Allocation is deterministic: students requiring accessible seating are allocated first. Others use regular seats first, with spare accessible seats as a capacity fallback. Roll numbers and physical room coordinates determine ordering within these groups.
- If horizontal spacing is selected, even-numbered seat columns remain empty; only odd-numbered columns can be assigned. Usable capacity is calculated from eligible seats, not nominal room capacity alone.
- Generation succeeds for the entire roster or leaves the existing seating unchanged if capacity or accessibility constraints cannot be met.

### FR-7. Accessible seating

- A student record includes an **Accessible seating required** field. The chart marks the student's requirement independently of the seat's accessibility.
- Rooms record whether their entry is accessible; seats can individually be marked accessible.
- To satisfy the accessible-seat requirement, both the room entry and the assigned seat must be marked accessible.
- No diagnosis or medical documentation is collected. The flag represents a general physical-access accommodation, not a full accommodation-management policy.
- A confirmed requirement change on the Seating page updates generated active examination plans together, preserving other students' valid manual assignments. Incompatible manual seats for the tagged student are released. Insufficient capacity leaves all plans and the requirement unchanged.

### FR-8. Manual changes, regeneration and audit

- Administrators can move a student to an eligible empty seat or swap two students in occupied seats.
- Moves and swaps must preserve room, spacing and accessibility constraints for every affected student.
- Regeneration preserves manual assignments by default. Replacing manual assignments requires an explicit selection and confirmation.
- A preview shows proposed changes and preservation/replacement counts. Stale reviewed operations are rejected before saving.
- Generation, regeneration, moves, swaps and clearing seating record the acting user, time, reason and relevant seat changes. Clearing seating does not erase its history.
- Completed seating is read-only. Cancelled seating cannot be regenerated or overridden, but may be cleared while retaining its audit history.

### FR-9. Student portal and usability

- The application opens on the student portal. A staff-login link opens the staff sign-in page.
- Students can search published seating by exact roll number and academic year; unpublished draft seating is hidden.
- Students can search and view the public examination timetable and print their seating details.
- Administrative screens provide navigation, search, filters or pagination where appropriate, and clear feedback for successful or rejected actions.

### FR-10. Invigilation allocation

- Administrators manage faculty eligibility, unavailable intervals, teaching/course conflicts and daily/academic-year workload limits.
- Automatic allocation covers each occupied room according to the configurable students-per-invigilator ratio, respecting availability, course conflicts, rest gaps and workload limits.
- Allocation favors lower assigned minutes and then lower duty counts. It previews a complete plan before saving and preserves accepted/manual duties by default.
- Each faculty member can supervise only one room in an exam. Invalid or stale plans save nothing.

### FR-11. Duty reassignment and response

- Administrators preview and confirm a replacement with a reason. Reassignment resets the duty to pending.
- Faculty see only their own published duties and can accept or decline scheduled duties. A decline requires a reason and alerts administrators.
- Duty changes and responses retain history. Completed and cancelled plans remain read-only; draft/cancelled duties can be cleared while retaining history.

### FR-12. System-wide conflicts and readiness

- Administrators can review room/student/faculty overlaps, missing or invalid seating, capacity, spacing, accessibility, rest gaps, workload, unavailable faculty, teaching conflicts and missing/pending/declined duties.
- Cross-year timetable conflicts are considered. A scheduled exam is ready only when no errors or confirmation warnings remain.
- Dashboard operational totals include exam states, seating assignments, readiness, faculty duty counts/minutes and unread notifications.

### FR-13. Reports

- Administrators can export room-wise seating, student-wise seating, faculty duties and examination schedules as paginated PDF or UTF-8 CSV.
- Reports support academic-year and exam filters, with room filters for seating and schedules. Faculty exports contain only their own duties.
- PDF uses standard Western fonts. CSV retains the complete original Unicode text.

### FR-14. Notifications

- Staff have an in-app notification center with read/unread controls. Duty changes/responses and exam publication/cancellation create notices.
- Student publication/cancellation notices can be queued to their recorded email addresses without requiring student accounts.
- A persistent SMTP outbox exposes queued, disabled, sent and failed states. Delivery/retries are explicit administrator actions and require SMTP configuration.

## 4. Data and operational requirements

- The application uses Java Servlets, JDBC and MySQL, with HTML, CSS and JavaScript in the browser.
- Admin changes that must be atomic either complete together or leave the prior data intact.
- Invalid and conflicting requests return controlled errors. Server-side validation is authoritative.
- Demonstration data is fictional. The evaluation dataset includes at least 50 students; the prepared demo contains 240 students across three departments, with academic-year placements, divisions, enrollments, rooms, exams and accommodation cases.
- The separate team browser demo uses simulated accounts and browser-local data. It includes the functional planning screens and local report downloads, while actual SMTP email belongs to the Java application.
- New features are implemented for user-led manual evaluation. Existing KPI evidence is historical; this delivery does not claim new test or KPI results.

## 5. Related specifications and evidence

- [Remaining project scope and rules](full-project-scope.md)
- [Complete project user guide](../implementation/complete-project-guide.md)

- [Accepted S5–S6 scope and evaluator acceptance](s5-s6-scope.md)
- [Sprint 7 manual overrides, accommodation and regeneration](s7-scope.md)
- [S2 account rules](account-rules.md)
- [Current academic, exam and seating rules](../implementation/current-rules.md)
- [Seating allocation algorithm](../algorithms/seating-allocation.md)
- [KPI verification and test evidence](../testing/kpi-report.md)
