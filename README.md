# Exam Seating and Invigilation Planner

Evaluation release covering S1-S6 plus the user's Sprint 7 seating refinement issue: academic administration, CSV enrollment, exam scheduling, accessible-seat priority, accommodation tagging, reviewed manual overrides and regeneration.

Sprint 7 adds accommodation changes across active generated exam plans, a seat-change preview, explicit manual-replacement acknowledgement and rejection of stale reviewed operations. Scope and workflow are documented in `docs/requirements/s7-scope.md`. The earlier master plan labels the same seating work S6; this release follows the user's quoted S7 issue.

## Stack
- Java 17
- Core Java Servlets
- JDBC
- MySQL
- HTML5, CSS and vanilla JavaScript
- Apache Tomcat 10.1+

## Database setup
```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed.sql
```

The default admin account is:
- username: `admin`
- password: `Admin@123`

Change it after the first login.

## Database connection
The application reads these environment variables when present:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

Defaults are suitable for a local MySQL installation using database `exam_seating`, user `root`, and an empty password.

## Build
```bash
mvn clean package
```

Deploy `target/exam-seating.war` to Tomcat and open:

`http://localhost:8080/exam-seating/`

## Isolated Windows demo

Install Java 17+, Maven, MySQL 8 and Tomcat 10.1. Override the installed paths as necessary:

```powershell
.\scripts\start-local-demo.ps1 -MysqlHome 'C:\Program Files\MySQL\MySQL Server 8.0' -TomcatHome 'C:\Tools\apache-tomcat-10.1.60' -JavaHome 'C:\Program Files\Java\jdk-25'
python scripts/prepare-demo.py --confirm-demo
```

Open **http://127.0.0.1:33080/exam-seating/**. Admin: `admin` / `Admin@123`. Faculty example: `cse.faculty1` / `Faculty@123`. These are fictional demonstration accounts. MySQL binds to loopback port 33307, with an empty root password, for the disposable demo only. Existing host services on port 3306/8080 are unchanged.

The student portal is the default landing page. **Staff login** in the corner opens `login.html`. Try `CSE26Y1A001` (Atharva Kulkarni) to find published seating. Students can search upcoming exams and the timetable, and print seating results. Staff pages provide grouped responsive navigation, searchable/paged lists, department/year filters, clear save feedback and collapsible editing/advanced controls. The palette uses navy, slate and muted teal.

Data/logs are stored in `.local-runtime/`. The script refuses an unrelated server on its ports. Maven uses the cache under `.local-runtime/m2`; `-Offline` avoids dependency downloads when this cache is ready. Other machines populate the cache through normal Maven downloads. Maven targets Java 17 even with a newer JDK.

```powershell
.\scripts\reset-local-demo.ps1 -ConfirmDisposable
python scripts/prepare-demo.py --confirm-demo
.\scripts\stop-local-demo.ps1
```

Reset replaces only this workspace's disposable database. Prepare generates 480 assignments and publishes 23 exams, leaving CSE102 as a draft demonstration. Stop gracefully stops only these local services and retains data.

## Evaluator CSV demonstration

The fictional dataset has 240 students, 26 courses, 24 divisions, 12 faculty, 8 rooms, 480 enrollments and 24 exams. Student/faculty names exclude the requested surname; addresses use example.test.

Use **CSV Import → Download evaluator sample**, or `database/evaluator-import.csv`. Its 24 rows add 12 fictional students, each enrolled in CSE103 and CSE104, workshops without published seating. The preview displays the header and first six rows. On the clean demo, import reports **12 new students, 12 additional enrollments, 0 duplicates, 0 invalid rows**. Re-import reports 24 duplicates. `database/student_import_template.csv` is the separate format example. Restore the clean demo after presenting using reset and prepare above.

## Upgrade an existing database

Fresh database: schema, seed, optional demo-data SQL in that order. In Windows PowerShell use `Get-Content -Raw database/schema.sql | mysql --user=YOUR_USER --password`, repeating for the next files. SQL uses database name `exam_seating`.

For the original S1-S4 database, back it up, stop the old application and run **only** `database/migrations/001_s1_s6_upgrade.sql` once. Do not run the full schema or fictional data over existing records. The upgrade preserves records and assigns 2026-27/division A, deriving study year from semester. Review those placements/accessibility flags. DDL is not a rollback transaction; restore the backup on failure. The migration targets the original schema, not an already upgraded database.

## Team browser demo

The interactive training copy is available at **https://altamashm7.github.io/student-seating-invigilator-system/**. It uses the current interface with a separate JavaScript simulator under `demo/`. Sample changes are stored in each browser; the demo login is simulated. The real Java/Tomcat/MySQL application above remains the project implementation.

Demo accounts: `admin` / `Admin@123`, or `cse.faculty1` / `Faculty@123`. Open **Demo guide** for practice steps. **Reset sample data** restores the original fictional records and accounts. Use fictional data and demo passwords.

To preview locally, run `python -m http.server 33081 --bind 127.0.0.1` from the repository root and open `http://127.0.0.1:33081/demo/`. After changing the webapp UI, run `python scripts/build-pages-demo.py`. Run `node scripts/test-pages-demo.mjs` to check the simulator. Publish only the contents of `demo/` to the `gh-pages` branch, configured as the Pages source at `/`.

## Verification

For the focused Sprint 7 checks, build first, start the isolated local demo database, then run `scripts/test-algorithm.ps1` and `scripts/test-sprint7.ps1 -ConfirmDisposable`. The second command creates a separate test application/database, saves API evidence and removes its test database afterwards. It does not reset the normal demo. `docs/testing/kpi-report.md` contains the executed results.

Existing ZIPs in `handoff` are the earlier S1-S6 delivery. They have not been refreshed for Sprint 7; the current release is the local repository source.

```powershell
mvn clean package
.\scripts\test-algorithm.ps1
# Only against freshly reset disposable data; this test changes/adds records:
python scripts/test-integration.py --confirm-disposable
# Restore presentation data afterwards:
.\scripts\reset-local-demo.ps1 -ConfirmDisposable
python scripts/prepare-demo.py --confirm-demo
```

Maven packaging alone does not invoke the pure Java algorithm runner. See [KPI evidence](docs/testing/kpi-report.md), [current rules](docs/implementation/current-rules.md), [user walkthrough](docs/implementation/user-guide.md).

Recorded verification: 98 core API assertions, 32 focused Sprint 7 API assertions and 40 allocation/manual-rule checks passed. See [KPI evidence](docs/testing/kpi-report.md). The separate Pages simulator has its own functional checks; its results do not replace testing of the Java application.

Original baseline: `973df5d7005961e57b31583b6b7808d411b12e6e`. The current S1-S7 source is published with the repository's existing member commits retained. The earlier twelve-member ZIP distribution was for S1-S6 and has not been regenerated for this release. Historical S1-S4 documents describe the first evaluation; current rules describe this release.

Invigilator duty allocation, workload balancing, notifications and report exports belong to later sprints. Faculty administration exists. Password hashing retains the baseline SHA-256 approach and does not claim production security hardening.
