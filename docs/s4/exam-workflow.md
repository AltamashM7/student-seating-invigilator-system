# S4 Exam Session and State Workflow

States:

`DRAFT -> SCHEDULED -> COMPLETED`

A draft or scheduled examination may also become `CANCELLED`.
Completed and cancelled examinations do not move back to earlier states.

Rules:
- Exam end time must be later than start time.
- At least one room must be selected.
- Only draft exams can have their course/date/time/rooms edited.
- Draft or cancelled exams can be deleted.
- Cancelled exams are ignored by conflict checks.
