# S4 Test Cases

| ID | Scenario | Expected result |
|---|---|---|
| EX-01 | Create non-overlapping exam | Exam is saved |
| EX-02 | End time before start time | Validation error |
| EX-03 | Same room, same date, overlapping time | Room conflict returned |
| EX-04 | Same room, touching boundary only | No overlap when one ends exactly as the next starts |
| EX-05 | Shared student in two overlapping courses | Student conflict returned |
| EX-06 | Different students, overlapping time | No student conflict |
| EX-07 | DRAFT to SCHEDULED | Transition succeeds |
| EX-08 | SCHEDULED to COMPLETED | Transition succeeds |
| EX-09 | COMPLETED to DRAFT | Transition is rejected |
| EX-10 | Cancelled exam overlaps another | Cancelled exam does not block it |
