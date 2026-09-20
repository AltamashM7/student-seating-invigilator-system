# Data Flow Diagram - Level 0

```mermaid
flowchart LR
    A[Administrator] -->|login and management data| S[Exam Planner System]
    F[Faculty] -->|login / timetable request| S
    ST[Student] -->|timetable request| S
    S -->|results and validation messages| A
    S -->|timetable| F
    S -->|timetable| ST
    S <--> DB[(MySQL Database)]
```
