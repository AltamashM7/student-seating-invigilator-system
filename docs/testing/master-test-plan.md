# Master Test Plan - S1 to S4

## Areas
1. Authentication and role access
2. Account validation
3. Room creation and automatic seat generation
4. Course CRUD
5. CSV student import and duplicate handling
6. Exam CRUD and state transitions
7. Student-level timetable conflicts
8. Room-level timetable conflicts
9. Frontend validation and error reporting

## Test types
- Functional tests
- Negative/input-validation tests
- Access-control tests
- Database consistency checks
- Small bulk-import performance check

Runtime results should be recorded only after the integrated build is run against MySQL and Tomcat.
