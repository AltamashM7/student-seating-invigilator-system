# Remaining project implementation

18 October 2026. This delivery implements the unfinished invigilation work and the S8-S10 functional work from the master sprint plan. The previously accepted Sprint 7 seating changes remain intact.

## Invigilation

- Faculty are available by default. Administrators maintain eligibility and daily/yearly limits; administrators or the faculty member record unavailable time intervals.
- Teaching/conflict-of-interest course links exclude a faculty member from those courses. Excluding an entire department is an optional global rule.
- The defaults are a 30-minute rest gap, one invigilator per 30 seated students in each occupied room, two duties per day and twenty per academic year. These are configurable project decisions, not university regulations.
- Seating must be complete before duties can be generated. Empty rooms require no invigilators. Each faculty member supervises at most one room per exam.
- Allocation prefers the eligible faculty member with the lowest assigned minutes, then duty count, employee code and ID. Bounded backtracking tries alternatives if a greedy choice blocks a later slot. It either proposes a complete plan or saves nothing.
- Preview and save share a revision. Manual assignments and accepted duties are preserved unless an administrator explicitly confirms replacement. A declined faculty member is not automatically assigned to the same exam again.

## Reassignment and confirmation

- Administrators review and confirm a replacement, supplying a reason. All assignment constraints are checked again when saving.
- Faculty see only their own duties and may accept or decline a scheduled duty. Declining requires a reason and alerts administrators; the room remains uncovered until reassigned.
- All assignment, replacement, confirmation and clearing actions retain history. Completed/cancelled examinations cannot be reassigned. Cancellation alerts assigned faculty and stops the exam counting towards active workload.
- Availability changes cannot invalidate an active assigned duty; decline/reassign that duty first.

## Conflicts and readiness

The dashboard reports missing rosters/rooms/seating, capacity, duplicate or invalid seats, spacing/accessibility violations, room/student overlaps, uncovered duty slots, pending/declined duties, faculty overlaps, rest gaps, availability, course conflicts and workload violations. Cross-year clashes are considered. Readiness requires complete valid seating and accepted coverage, with no outstanding conflicts.

## Reports and notifications

- Administrators download room-wise seating, student-wise seating, faculty duties and exam schedules as PDF or CSV. Faculty can download their own duties.
- In-app notifications work independently of email. Duty changes, responses, exam publication/cancellation and explicit published-exam notices create notifications.
- Email uses SMTP configuration and a persistent outbox. Administrators explicitly send/retry queued messages. Missing SMTP configuration and failed deliveries remain visible; no delivery is claimed without a successful SMTP response.
- Dashboard totals come from stored records and current conflict results. They are operational counts, not KPI verification results.

## Final integration

Includes a non-destructive S1-S7 upgrade, fresh-schema support, integrated navigation, faculty views, report downloads, email setup, deployment notes and a manual walkthrough. Password storage supports salted PBKDF2 and upgrades legacy hashes on login. Session/cross-origin safeguards and private-response headers are included.

The user will perform manual testing and KPI verification. No new website tests, UAT, performance measurements or KPI pass claims are part of this implementation. A build with tests skipped checks compilation/package creation only. Production hosting credentials, SMTP credentials and a production domain are supplied by the deployer; this local delivery does not publish to GitHub or hosting services.
