# Exam Seating and Invigilation Planner

First-evaluation build covering Sprint S1 to S4.

## Stack
- Java 17
- Core Java Servlets
- JDBC
- MySQL
- HTML, CSS and vanilla JavaScript
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

## Scope
This branch set intentionally stops after S4: authentication, master data, CSV import, exam scheduling, timetable views, and student/room conflict detection. Seating allocation and invigilator assignment are later-sprint work.
