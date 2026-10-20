# Complete project guide

## Start the application on this laptop

Open PowerShell in the project folder and run:

```powershell
.\scripts\start-local-demo.ps1 -Offline
```

Open `http://127.0.0.1:33080/exam-seating/`. The startup script applies the remaining-project database upgrade when needed and retains the current records. It packages the application with tests skipped.

Admin: `admin` / `Admin@123`. Faculty example: `cse.faculty1` / `Faculty@123`, unless these accounts were changed locally.

For another installation, load `database/schema.sql`, then `database/seed.sql`; optionally load `database/demo-data.sql`. An existing S1-S7 database needs only `database/migrations/002_complete_project.sql`, not a schema reload. See [deployment](deployment.md).

## Administrator workflow

1. Maintain academic years, departments, divisions, students, courses and enrollments as before. Create rooms and configure accessible entry/seats before assigning rooms to exams.
2. Create an exam, select rooms and generate seating on the Seating page. Manual seat changes, accommodation tagging and regeneration remain available.
3. Open **Faculty Availability**. Select a faculty member and record unavailable periods. Set eligibility, daily/yearly limits and their teaching/course conflicts. Assignment rules provide the rest gap and the seated-students-per-invigilator ratio.
4. Open **Invigilation** and choose the academic year and an exam, or all active exams in that year. **Review duty allocation** shows the full proposed plan. Enter a reason and confirm. Complete seating is required first.
5. Accepted and manual duties remain in place during normal regeneration. The advanced replacement option requires a separate acknowledgement and makes replacement duties pending again.
6. A **Reassign** row action lets you select a replacement. Review the proposed change, supply a reason and confirm. The replacement must satisfy the same availability, conflict, rest and workload rules.
7. Publish the exam from **Exams** when appropriate. Faculty can respond to scheduled duties. Draft duties remain private to administrators.
8. Open **Conflicts & Readiness**. Follow each **Review** link to address missing/invalid seating, timetable clashes, unfilled duties, pending confirmations or other constraints. After a seating change alters room occupancy, review and regenerate duties if necessary. Refresh the overview to recalculate readiness.
9. Open **Reports** to choose a report, academic year, optional exam/room and PDF or CSV. The four reports are room-wise seating, student-wise seating, faculty duties and the exam schedule.
10. Open **Notifications** to read updates. Publication/cancellation creates student email notices and assigned-faculty notifications. Duty changes and responses create staff notifications. With SMTP configured, send queued emails in batches or retry a failed message. Do not infer inbox delivery solely from SMTP acceptance.

**Clear duties** is available for a selected draft/cancelled exam and retains history. Clear duties before clearing its seating. Completed exams remain read-only. Cancelled exams retain records but no longer count towards active faculty workload.

## Faculty workflow

1. Use Staff login with a faculty account. The default faculty page shows the duty schedule.
2. Select an academic year and search the list. Each row includes the date/time, course, room, duty slot, exam status and response status.
3. **Accept** confirms a scheduled duty. **Decline** requires a reason and notifies administrators. A declined duty needs administrator reassignment and cannot simply be accepted again.
4. Use **Availability** to record unavailable intervals or change eligibility. An existing active duty must be declined/reassigned before a conflicting availability change can be saved.
5. Use **Notifications** for account updates. Download your own duty list as PDF or CSV from the duty page.

## Student workflow

The student portal remains the default page and does not require login. Select an academic year and search by roll number, for example `CSE26Y1A001`. Only scheduled/completed exams appear. Students can view the timetable and print published seating details.

## Browser training copy

```powershell
python -m http.server 33081 --bind 127.0.0.1
```

Open `http://127.0.0.1:33081/demo/`. The local copy includes planning, duty responses, local notifications and report downloads. Data changes stay in that browser. Actual email sending uses the Java application and SMTP configuration. The hosted site is unchanged until a separate deployment.

## Evaluation status

The remaining features have been implemented for manual evaluation. No website tests, new KPI checks, report-rendering checks, live email delivery checks or production deployment were performed for this delivery. Historical S1-S7 evidence remains historical.
