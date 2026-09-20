# S4 Timetable Conflict Rules

## Room conflict
Two active examinations conflict when:
- they are on the same date,
- at least one selected room is the same,
- the time intervals overlap.

Overlap condition:

`existing.start < new.end AND existing.end > new.start`

## Student conflict
Two active examinations conflict when:
- they are on the same date,
- their time intervals overlap,
- at least one student is enrolled in both course A and course B.

Both checks run before a new exam or edited draft is saved.
