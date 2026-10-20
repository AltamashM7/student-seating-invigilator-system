# Exam Seating and Invigilation Planner: implementation report

18 October 2026

## Purpose

The project brings academic administration, examination scheduling, student seating and faculty invigilation into a single college web application. Students use the public portal; administrators and faculty sign in for their role-specific work.

## Architecture

The browser uses HTML, CSS and vanilla JavaScript. Java Servlets handle requests, service classes implement allocation/validation and DAO classes store data through JDBC in MySQL. Tomcat serves the WAR built by Maven. The browser training model is a separate local-data copy.

## Implemented modules

| Area | Delivered functionality |
|---|---|
| Academic records | Academic years, departments, divisions, annual student placement, courses/offerings, enrollments, rooms and staff accounts |
| Bulk enrollment | CSV preview, row classification, duplicates and additional enrollments |
| Examinations | Scheduling, room/student conflict checks, publication, completion/cancellation and timetable |
| Seating | Deterministic allocation, accessible-seat priority, spacing, moves/swaps, reviewed regeneration and history |
| Invigilation | Eligibility, unavailable times, course conflicts, rest gaps, daily/yearly limits and balanced assignment |
| Duty workflow | Preview, accepted/manual preservation, reassignment, faculty accept/decline and history |
| Readiness | System-wide seating, timetable and invigilation issue overview with action links |
| Reports | Room seating, student seating, faculty duty list and exam schedule in PDF/CSV |
| Notifications | Staff inbox, student publication email notices, persistent SMTP outbox and explicit retries |
| Integration | Role navigation, dashboard statistics, database upgrades, setup instructions and security safeguards |

## Algorithms and data

Seating uses deterministic greedy ordering with priority for students who require accessible seats. Invigilation creates slots from actual occupied rooms and tries the lowest-loaded eligible faculty first, backtracking if a choice blocks a later slot. The system does not claim globally optimal scheduling.

Normalized records separate academic-year placements, course offerings, seating assignments, faculty unavailability, course conflicts, duty settings, duty assignments and history. Unique constraints and locked transactions prevent partial allocation changes. A new migration adds the remaining modules without resetting the existing academic records.

## Demonstration walkthrough

1. Use the student portal and a known roll number to display published seating.
2. Sign in as administrator and inspect academic-year student counts and examination states.
3. Open a seating plan and demonstrate a reviewed move, swap or regeneration.
4. Configure faculty availability, limits and course conflicts.
5. Review and save an invigilation plan.
6. Sign in as an assigned faculty member and accept or decline a scheduled duty.
7. Return as administrator to review notifications and reassign a declined duty.
8. Open Conflicts & Readiness and inspect any remaining issue.
9. Download one of the four reports as PDF or CSV.
10. If SMTP is configured, explicitly send a queued message and inspect its delivery status.

## Limits and evaluation

Accessible seating is the supported accommodation; medical documentation is not collected. Horizontal spacing reserves even columns. One exam uses one annual course roster. PDF uses standard Western fonts; CSV preserves Unicode. The email status records SMTP acceptance and cannot guarantee recipient inbox delivery.

The user will carry out manual testing, acceptance and KPI verification. No new functional test, load test, KPI pass, report-rendering check, live email delivery or production deployment is claimed here. Hosting and SMTP credentials remain deployment configuration.

See the complete user guide and deployment notes for the exact local commands and controls.
