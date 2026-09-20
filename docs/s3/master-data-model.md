# S3 Room and Course Data Model

## Room
- room number: unique text value
- capacity: positive integer
- seats: generated automatically as S001, S002, ... up to capacity

## Course
- course code: unique text value
- course name
- department
- semester: 1 to 8

## Student import
CSV columns:

`roll_number,name,email,department_code,semester,course_code`

Rules:
- Header row is required.
- Commas inside field values are not supported in this simple CSV format.
- Duplicate roll numbers are counted and skipped.
- Unknown departments/courses and invalid semesters are reported by line number.
- A successful row creates the student and the course enrollment together.
