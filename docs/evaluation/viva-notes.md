# Viva Notes - S1 to S4

## Why Servlets + JDBC?
Servlets provide the basic Java web backend required by the project. JDBC is the direct connection layer between Java and MySQL. No backend framework or ORM is used.

## How does login work?
The login servlet reads the username/password, finds the user through JDBC, hashes the entered password and compares it with the stored hash. On success an HTTP session stores the user id and role.

## How is role access enforced?
Frontend guarding only improves navigation. Real access control happens on the backend through `AuthFilter`, which checks the session and required role before protected admin/faculty APIs run.

## How are room seats generated?
When a room is created, its capacity controls a loop that inserts seat numbers such as S001, S002 and so on. Updating capacity adds or removes the corresponding generated seats.

## How does CSV import work?
The servlet reads the uploaded file line by line. Each row is split into the fixed six-column template, validated, checked for duplicate roll number, then inserted into `students` and `course_enrollments` using JDBC.

## How is room conflict detected?
For the same date, an existing exam conflicts when it uses one of the same rooms and its time interval overlaps the new exam interval.

## How is student conflict detected?
The database checks whether a student belongs to both the new exam course and another overlapping exam course. If at least one common student exists, the schedule is rejected.

## Why are seating allocation and invigilator assignment missing?
They belong to later master-plan sprints. The first evaluation implementation is intentionally bounded to S1-S4.
