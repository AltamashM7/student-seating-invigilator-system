# Timetable Conflict Detection Flow

```text
Read course, date, start time, end time and selected rooms
  |
Validate end time > start time and at least one room
  |
Search active exams on same date whose time overlaps
  |
Selected room already used? -- yes --> room conflict
  |
Find students enrolled in new course and overlapping exam courses
  |
Any common student? -- yes --> student conflict
  |
No conflict --> save exam
```
