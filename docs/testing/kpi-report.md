# KPI verification

## Remaining project implementation - 18 October 2026

Invigilation, reassignment/response, conflict overview, reports, notifications and final integration were added after the verification results below. Website testing and KPI verification for these changes are intentionally left to the user. Do not treat the historical S1-S7 results as verification of the new modules.


## Sprint 7 reviewed implementation — 13 October 2026

Scope: the user's quoted manual-override/accommodation/regeneration issue, documented in `docs/requirements/s7-scope.md`. The earlier master plan assigns this work a different sprint number; invigilator allocation is not part of this issue.

| Requirement | Executed verification | Result |
|---|---|---|
| Accommodation pre-reservation | Priority, regular-seat preference, accessible fallback, manual pin conflicts, missing capacity and partial-row spacing | 23 allocator checks passed |
| Manual override validation | Pure move/swap rules including both affected students, occupancy, enrollment, room membership, spacing and accessibility | 17 manual-rule checks passed |
| Persisted workflow | Valid/invalid moves/swaps, audit, stale snapshots, preservation/replacement, and completed/cancelled states | Included in 32 passing API checks |
| Accommodation tagging | Before allocation; atomic updates of generated draft/scheduled exams; selective release of incompatible pins; stale preview in another affected exam; failure in one exam leaves all plans and flag unchanged | API cases passed; normal demo database not used for test mutations |
| Regeneration confirmation | Read-only proposed mapping, server confirmation, preserved pins, explicit full replacement and stale preview rejection | API and browser cases passed |
| Browser interactions | Source/target chart selection, requirement badge, manual badge, tag review/save, regeneration review/save, acknowledgement, cancellation and selector refresh | Seven workflow checks passed; no captured console errors |
| Build | Fresh Java 17 compilation of 36 production sources and both test runners, then WAR packaging using cached Maven dependencies | Clean build passed, with no compilation skipped |
| Static UI check | JavaScript/Python syntax and HTML asset references | Passed |

The standalone unit runner executed **40 checks (23 allocation + 17 manual rules)**. Maven compiles the main-method test runners but does not discover/run them as JUnit tests; `scripts/test-algorithm.ps1` executes them explicitly.

The API checks ran on a separate application at port 33090 with database `exam_seating_s7_check` on the workspace's isolated MySQL instance. The normal demo's students, enrollments, seating and history were not changed by this verification. Temporary test service/database cleanup is part of the test launcher.

Evidence:
- `docs/testing/evidence/algorithm-results.txt`
- `docs/testing/evidence/sprint7-api-results.json`
- `docs/testing/evidence/sprint7-browser-results.json`
- `docs/testing/evidence/sprint7-regeneration-preview.png`
- `docs/testing/evidence/sprint7-build-results.txt`

Reproduce the focused checks after building and starting the isolated local demo database:
```powershell
.\scripts\test-algorithm.ps1
.\scripts\test-sprint7.ps1 -ConfirmDisposable
python scripts/check-ui-source.py
```

These results cover the changed seating workflows. They do not claim a new full security/load audit, a general medical-accommodation system or a mixed-subject adjacency engine.

## Historical S1-S6 verification — 4 October 2026

Verified on the isolated local instance: MySQL 8, Tomcat 10.1.60, Java 25 compiling with release 17. The original repository and host MySQL database were not modified. Baseline: `973df5d7005961e57b31583b6b7808d411b12e6e`.

Authoritative scope inputs were the supplied orchestrator handoff, the evaluator feedback/user decisions, and the original Downloads PDFs: `exam_seating_sprint_overview_updated.xlsx - Sprint Overview.pdf`, `Sprint_KPI_1_Exam.pdf`, and `Mini Project_ Exam Seating and Invigilation Planner.pdf`. The KPI report covers accepted S1-S6 features, not every future sprint or production condition.

| Sprint / target | Verification performed | Outcome |
|---|---|---|
| S1 requirements/structure/design | Accepted scope, current rules, academic model, ER diagram, existing three role wireframes, build structure | Documented/reviewed within agreed scope |
| S2 authentication and access | Admin/faculty login, invalid credentials, unauthorized routes/mutations, registration policy/duplicates, admin recovery, old/new password behavior, logout | All exercised assertions pass; unauthorized requests rejected |
| S3 CRUD/import | Academic-year, department, division, course/offering, room capacity, faculty/account, student/placement and enrollment CRUD; invalid/duplicate/reference guards | All exercised assertions pass |
| S3 bulk import quality | 120 valid rows, 120 duplicates, 120 existing students gaining a second subject; header/unknown-department/physical-line errors | 120/120 valid imports; duplicate/error classification correct |
| S4 workflow/conflicts | DRAFT/SCHEDULED/COMPLETED/CANCELLED rules; create/edit/delete; room/shared-student overlap, touching endpoints; concurrent competing saves | All exercised assertions pass; one concurrent winner |
| S5 valid allocation/capacity | 17 pure Java checks; API atomic capacity failure and uniqueness; full demo charts | 17/17 pass; 480/480 assignments; zero duplicate seats/students per exam |
| S6 accommodation enforcement | Accessible priority, insufficient accessible capacity, invalid movement and swap affecting the other student, cross-year edit protection | All exercised assertions pass; 48/48 demo accommodation assignments valid |
| S6 overrides/regeneration/audit | Valid move/swap, preservation of manual pins, explicit replacement, retained history after clear; UI reassignment and regeneration | Constraints hold; actor/reason/time recorded; manual assignment preserved |
| Evaluator years/counts/data | All departments × 4 study years, divisions, independent counts, empty department, promotion retaining both annual placements | 240 students; 24 divisions; correct totals; zeros included |
| Browser workflows | Eight management screens, third-year course creation -> semester 5, dashboard counts, manual override/regeneration, student published-only lookup, 390-pixel responsive chart | Verified; mobile overflow fixed; no captured console errors in final checked browser flows |
| Upgrade/runtime | Original-schema fixture with populated relations; guarded isolated reset, graceful stop/restart, offline package build | Upgrade preserves fixture; runtime verified; WAR build succeeds |

The original S1-S6 API integration result was **98 passed, 0 failed**. Its original standalone allocator result was **17 passed**. A 2,000-student allocation passed in **1,365 ms** in the saved run. The latest 120-row import completed in **5.34 seconds** on this machine. Neither timing is a service-level guarantee. The refreshed CSV sample was verified through both API and browser: 12 new students, 12 additional enrollments, zero invalid rows; repeat import gives 24 duplicates.

## Evidence and reproduction

- `evidence/integration-results.json`: names/results of all 98 actual assertions.
- `evidence/algorithm-results.txt`: 17 assertions and measured load result.
- `evidence/migration-results.json`: repeatable original-schema preservation check; the earlier tabular run is also retained.
- `evidence/demo-results.json`: 240-student/480-assignment clean demo invariants before browser override demonstrations.
- `evidence/browser-results.json` and JPEGs: actual UI observations, mobile geometry and screenshots.
- `evidence/ui-refresh-results.json`, `student-home-desktop.jpg`, `evaluator-import-ui.jpg`: refreshed student portal, search/filter/navigation, mobile exam and CSV observations. Earlier JPEGs document the previous UI.
- `evidence/build-results.txt`: successful WAR build and local runtime deployment.

Run `test-algorithm.ps1` explicitly; Maven packaging does not run that main-method suite. Reset disposable data, then run `python scripts/test-integration.py --confirm-disposable`. Repeat migration with `python scripts/test-migration.py --confirm-disposable`. Restore presentation data with the guarded reset and `prepare-demo.py --confirm-demo`. Browser edits add retained audit history; they do not alter the number of valid assignments.

The fresh demo has 23 published exams and one draft, CSE102, for override demonstrations. This deliberately verifies draft exclusion from public views. Spacing means horizontal odd columns only. Accessible seating is the supported accommodation. Priority uses sorting, not a literal PriorityQueue. The baseline password hashing remains SHA-256. This is bounded local functional verification, not a production load, security or comprehensive accommodation-policy audit. Later invigilator/reporting KPIs are not claimed as complete.

The UI refresh was exercised in a real browser before the usage interruption. Final small presentation fixes were deployed successfully after resumption and checked in source. Browser security policy blocked reconnecting for the final visual recheck; no new browser pass is claimed for those last edits. The clean demo was restored after test fixtures and CSV imports.
