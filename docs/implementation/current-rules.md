# Current S1-S6 rules

## Academic structure

Academic year is a calendar label such as 2026-27. Study year is First/Second/Third/Final year. Semesters 1-2/3-4/5-6/7-8 map to study years 1/2/3/4. A reusable course catalog has study year and semester; `course_offerings` links a course to an academic year. Divisions belong to an academic year, department and study year, with an A/B-style code.

Student annual placement has one division and semester per academic year. Promotion creates/updates the new placement and retains the old one. Legacy student department/semester are profile mirrors; the annual table is the source for counts. Dashboard counts use placements, not course-enrollment totals. All departments appear for four years, including zeros, and division counts include empty divisions.

These decisions use established administration patterns described by [Frappe Education academic years](https://docs.frappe.io/education/academic-year) and [program enrollment](https://docs.frappe.io/education/program-enrollment). This application's model is tailored to its requirements.

## CRUD and enrollment

Admin screens provide CRUD for years, departments, divisions, students, faculty, rooms, courses and course enrollments. Faculty CRUD manages the associated login account. Courses have offer/remove-offering actions. Current year deletion is blocked; choose another current year first. Foreign keys and service guards protect referenced records. Students and exams with seating history are retained.

Course department/semester and division placement fields are protected while referenced. Names/codes can be edited without changing placement constraints. A room's layout and accessibility are protected once assigned to an exam. Creating a room generates seat coordinates; the first two seats are accessible by default when entry is accessible. Individual accessible seats can be configured before assigning the room. Both room entry and seat accessibility must be true to satisfy accommodation.

Annual course enrollment requires a matching annual placement/semester and course offering. Student/course/year is unique. Late enrollment cannot introduce overlapping exams. Enrollments are protected after generation/publication; clear draft seating before changing its roster. Published placement is protected, and accessibility cannot change while the student has active seating in any year.

CSV header: `roll_number,name,email,department_code,semester,course_code,academic_year,division,requires_accessible`. The six-column legacy header uses current year/division A. Empty lines retain physical error line numbering. Existing matching students can enroll in another course; duplicates are reported. Fields are plain unquoted CSV: commas/newlines inside quoted fields are unsupported. Each row commits independently. No medical diagnosis/document is stored.

## Exam rules

Input editing requires DRAFT with no seating history. Status transitions are DRAFT to SCHEDULED/CANCELLED, SCHEDULED to COMPLETED/CANCELLED. Same-state requests are harmless. Cancelled/completed exams cannot be reopened. Only DRAFT/CANCELLED exams without history can be deleted. Public timetable and seating search expose only SCHEDULED/COMPLETED exams.

Intervals overlap when `existing.start < new.end AND existing.end > new.start` on the same date; touching endpoints are allowed. Non-cancelled overlapping exams cannot share rooms or enrolled students, including across academic years. Creation, editing, publication and late enrollment validate conflicts. A global `planner_lock` row serializes administrative JDBC transactions; a tested pair of competing saves produces one winner.

## Seating and audit

Generate for DRAFT/SCHEDULED from its annual roster and selected rooms. Every student is assigned or no changes are saved. Exam/student and exam/seat uniqueness are enforced. Horizontal separation uses columns 1,3,5,..., reserving even columns and reducing usable capacity. It does not enforce vertical/diagonal spacing, mixed-subject adjacency or physical distances.

Students needing accessible seats are allocated first. Other students use non-accessible seats first; accessible seats are a fallback when ordinary capacity is insufficient. Room entry and the seat must both be accessible, and spacing constraints still apply. Reassign requires an empty target; swap requires an occupied target. Both students must satisfy accessibility/spacing, and the target must belong to the exam. Regeneration shows a preview and preserves valid manual seats by default. Full replacement requires an explicit selection, acknowledgement and server confirmation flag. A stale reviewed snapshot cannot be saved. Allocation is validated before replacing rows. Completed seating is read-only; cancelled seating can be cleared but cannot be allocated or overridden.

The Seating page can change the accommodation requirement after allocation. Preview and confirmation cover all generated active examinations for that student across academic years. They are reallocated atomically, retaining other students' manual pins and releasing an incompatible pin only for the tagged student. Insufficient capacity in any affected exam saves no tag or seating changes. Ungenerated, completed and cancelled plans are not regenerated. Direct general student editing remains protected while active seating exists; the dedicated accommodation workflow is used instead.

Generation, regeneration, swap, reassignment, accommodation changes and clear record actor, timestamp, reason and old/new room-seat labels. Clear retains history. The chart displays the last 200 events; the database retains all events. The supported accommodation is accessible seating. The requirement flag is current student data, while completed seat positions remain historical.

## API and compatibility

The application root is the public student portal; staff login is `login.html`. Public lookup still requires an exact roll number and exposes only published exams. Shared navigation/list helpers centralize presentation behavior without changing permissions. Editing forms and advanced controls are collapsed until needed. Search/pagination/filtering inspect records without mutating them. Searchable selectors retain their current selection while narrowing options.

Form-encoded POST values produce JSON responses. Admin routes require ADMIN sessions.

| Route | Purpose |
|---|---|
| `/api/admin/data` | GET year-scoped snapshot; POST resource/action CRUD, offerings and seat accessibility |
| `/api/admin/exams` | GET list; POST create/update/delete/status |
| `/api/admin/seating` | GET chart or regeneration/accommodation preview; POST generate/regenerate/swap/reassign/accommodation/clear; reviewed revisions and explicit replacement confirmation. Full contract: `docs/requirements/s7-scope.md` |
| `/api/admin/import/students` | Multipart CSV file |
| `/api/timetable` | Public published exams, optional academicYearId |
| `/api/seating/search` | Public exact rollNumber and academicYearId lookup |
| `/api/auth/reset-password` | Admin-assisted recovery matching username/email and password policy |

Legacy room/course POST routes delegate to AcademicDao. Original unused DAOs/models are retained; new management screens use the new annual model. Invalid inputs produce controlled 400/409 errors; unexpected SQL errors are logged. Baseline session and SHA-256 password storage are retained; public unauthenticated reset has been removed. No production security audit is claimed.
