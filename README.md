# Exam Seating and Invigilation Planner

A college examination-management application built with Java Servlets, JDBC, MySQL and plain HTML/CSS/JavaScript. It includes the accepted seating refinements and the remaining functional work from the ten-sprint master plan.

## Features

- Academic years, departments, FY/SY/TY/BE divisions, student placements, courses, enrollments, faculty and rooms.
- CSV import with previews, duplicate detection and row errors.
- Exam scheduling, state changes, room/student clash prevention and a public timetable.
- Student seating lookup, deterministic allocation, accessible-seat priority, spacing, manual moves/swaps, accommodation updates and regeneration previews.
- Faculty availability, teaching/course exclusions, rest gaps, workload limits and invigilator allocation with least-loaded selection and bounded backtracking.
- Duty previews, manual reassignment, faculty accept/decline, retained history and readiness/conflict views.
- Room-wise seating, student-wise seating, faculty duties and exam schedules in PDF or CSV.
- In-app notifications, a configurable SMTP email outbox and operational dashboard totals.
- Salted PBKDF2 passwords with legacy login upgrades, role restrictions and same-origin write safeguards.

## Stack

Java 17+, Core Java Servlets, JDBC, MySQL 8, HTML5/CSS/vanilla JavaScript, Maven and Apache Tomcat 10.1+. Algorithms and the simple PDF writer use the JDK; no scheduling framework or external email API is used.

## Run on this Windows laptop

Open PowerShell in the project folder:

```powershell
.\scripts\start-local-demo.ps1 -Offline
```

The script starts the isolated local services, applies the remaining database migration when needed, retains existing records and packages with tests skipped. Remove `-Offline` on a machine that needs Maven dependency downloads. Pass `-MysqlHome`, `-TomcatHome` or `-JavaHome` if the installed locations differ.

Open **http://127.0.0.1:33080/exam-seating/**.

- Administrator: `admin` / `Admin@123`
- Faculty example: `cse.faculty1` / `Faculty@123`

These are fictional demonstration accounts; locally changed credentials remain changed. The student portal is the default page, with **Staff login** in the corner. Try roll number `CSE26Y1A001` in academic year `2026-27` for published seating.

The disposable MySQL server uses loopback port 33307. Existing host services on ports 3306/8080 are not replaced. Data/logs remain in `.local-runtime/`.

## Set up another installation

Fresh database:

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed.sql
# Optional fictional demonstration data:
mysql -u root -p < database/demo-data.sql
```

Existing S1-S7 database: back it up, stop the old application and apply **only**:

```bash
mysql -u root -p < database/migrations/002_complete_project.sql
```

On Windows PowerShell, use `Get-Content -Raw database/migrations/002_complete_project.sql | mysql -u root -p`. The isolated startup script applies this upgrade automatically. For an original S1-S4 database, apply migration 001 before 002. Do not reload the fresh schema over existing records.

Configure `DB_URL`, `DB_USER` and `DB_PASSWORD` for the Tomcat process. The default database is `exam_seating` on local MySQL. Use a dedicated account and password for deployment.

```bash
mvn -Dmaven.test.skip=true clean package
```

Deploy `target/exam-seating.war` to Tomcat. The normal installation URL is `http://localhost:8080/exam-seating/`.

## Invigilation workflow

1. Create the exam and generate complete seating.
2. In **Faculty Availability**, configure eligibility, unavailable periods, workload limits and teaching/course conflicts.
3. In **Invigilation**, select an exam/year, review the proposed allocation, enter a reason and confirm.
4. Publish the exam. Faculty log in to see assigned rooms and accept/decline scheduled duties.
5. Reassign declined duties and use **Conflicts & Readiness** to review outstanding issues.
6. Download reports or read notifications from the navigation.

Default rules: 30 minutes between duties, one invigilator per 30 seated students per occupied room, two duties per faculty per day and twenty per academic year. These are configurable project decisions. Accepted/manual duties remain in place unless replacement is explicitly acknowledged. A faculty member who declined an exam is excluded from its automatic regeneration.

## Email

In-app notifications work without an email server. Actual email needs `SMTP_HOST`, `SMTP_FROM`, and the provider's `SMTP_PORT`, `SMTP_MODE`, `SMTP_USERNAME` and `SMTP_PASSWORD`. STARTTLS and implicit TLS are supported. Restart Tomcat after configuration.

Administrators send queued messages from **Notifications**. Disabled/failed entries can be queued again. A Sent status means SMTP accepted the message, not that inbox delivery was verified. The browser training copy never sends real email. See [deployment notes](docs/implementation/deployment.md).

## Fictional data and CSV

The prepared dataset contains 240 students, 26 courses, 24 divisions, 12 faculty, 8 rooms, 480 enrollments and 24 exams. Fresh demo data includes example faculty/course conflict links. Names are fictional and email addresses use `example.test`.

Use **CSV Import → Download evaluator sample**, or `database/evaluator-import.csv`. The 24 rows enroll twelve new students in CSE103 and CSE104. The page previews the file before import. `database/student_import_template.csv` is the format example.

To prepare a newly loaded disposable dataset's seating/publication examples, run `python scripts/prepare-demo.py --confirm-demo`. This generates seating and changes exam publication states. Do not use reset/preparation scripts on a production database.

## Browser training copy

```bash
python -m http.server 33081 --bind 127.0.0.1
```

Open `http://127.0.0.1:33081/demo/`. This separate JavaScript model stores practice changes in each browser and includes the new planning, duty response, notification and report screens. It does not replace Java/MySQL. **Reset sample data** restores the fictional records.

Regenerate its UI after source changes with `python scripts/build-pages-demo.py`. The complete source and updated browser copy are published in this repository. The browser version is available at **https://exam-seating-planner.pages.dev/**. Source changes require a separate deployment to update the hosted copy.

## Documentation

- [Complete project guide](docs/implementation/complete-project-guide.md)
- [Deployment and SMTP](docs/implementation/deployment.md)
- [Core requirements](docs/requirements/core-requirements.md)
- [Remaining project scope and rules](docs/requirements/full-project-scope.md)
- [Invigilator algorithm](docs/algorithms/invigilator-allocation.md)
- [ER diagram](docs/diagrams/er-diagram.md)
- [Project report](docs/evaluation/final-project-report.md)

## Evaluation status

The remaining features were implemented on 18 October 2026 for user-led manual testing. Website tests, KPI verification, PDF rendering verification, live email delivery checks and production deployment have not been performed for these changes. Packaging with tests skipped checks compilation/package creation only.

[Historical KPI evidence](docs/testing/kpi-report.md) covers earlier S1-S7 work and must not be presented as verification of the new modules. Earlier member ZIPs and setup handouts are historical deliveries and have not been regenerated.
