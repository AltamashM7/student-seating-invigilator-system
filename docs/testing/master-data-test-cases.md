# S3 Test Cases

| ID | Scenario | Expected result |
|---|---|---|
| MD-01 | Create room R101 capacity 30 | Room and 30 seats are created |
| MD-02 | Create duplicate room R101 | Duplicate error |
| MD-03 | Increase room capacity 30 to 35 | S031-S035 are added |
| MD-04 | Decrease room capacity 35 to 25 | Seats above S025 are removed |
| MD-05 | Create valid course | Course appears in list |
| MD-06 | Create duplicate course code | Duplicate error |
| CSV-01 | Import valid rows | Students and enrollments are created |
| CSV-02 | Duplicate roll number | Row is counted as duplicate and skipped |
| CSV-03 | Unknown course | Row appears in error list |
| CSV-04 | Invalid semester | Row appears in error list |
| CSV-05 | 100+ valid rows | Import finishes without database inconsistency |
