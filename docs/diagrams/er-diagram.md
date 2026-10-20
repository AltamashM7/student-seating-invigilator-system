# Complete project ER Diagram

```mermaid
erDiagram
    USERS ||--o| ADMINS : owns
    USERS ||--o| FACULTY : owns
    DEPARTMENTS ||--o{ FACULTY : contains
    DEPARTMENTS ||--o{ STUDENTS : contains
    DEPARTMENTS ||--o{ COURSES : offers
    ACADEMIC_YEARS ||--o{ DIVISIONS : scopes
    DEPARTMENTS ||--o{ DIVISIONS : groups
    STUDENTS ||--o{ STUDENT_ACADEMIC_ENROLLMENTS : placed_annually
    ACADEMIC_YEARS ||--o{ STUDENT_ACADEMIC_ENROLLMENTS : scopes
    DIVISIONS ||--o{ STUDENT_ACADEMIC_ENROLLMENTS : contains
    COURSES ||--o{ COURSE_OFFERINGS : offered
    ACADEMIC_YEARS ||--o{ COURSE_OFFERINGS : scopes
    COURSE_OFFERINGS ||--o{ COURSE_ENROLLMENTS : roster
    STUDENT_ACADEMIC_ENROLLMENTS ||--o{ COURSE_ENROLLMENTS : annual_placement
    ROOMS ||--o{ SEATS : contains
    STUDENTS ||--o{ COURSE_ENROLLMENTS : has
    COURSES ||--o{ COURSE_ENROLLMENTS : has
    COURSES ||--o{ EXAMINATIONS : scheduled_for
    EXAMINATIONS ||--o{ EXAMINATION_ROOMS : uses
    ROOMS ||--o{ EXAMINATION_ROOMS : assigned_to
    ACADEMIC_YEARS ||--o{ EXAMINATIONS : scopes
    EXAMINATIONS ||--o{ SEATING_ASSIGNMENTS : allocates
    STUDENTS ||--o{ SEATING_ASSIGNMENTS : receives
    SEATS ||--o{ SEATING_ASSIGNMENTS : occupied_per_exam
    EXAMINATIONS ||--o{ SEATING_HISTORY : audited
    STUDENTS ||--o{ SEATING_HISTORY : affected
    USERS ||--o{ SEATING_HISTORY : actor
    FACULTY ||--o| FACULTY_DUTY_SETTINGS : limits
    FACULTY ||--o{ FACULTY_UNAVAILABILITY : declares
    FACULTY ||--o{ FACULTY_COURSE_CONFLICTS : excludes
    COURSES ||--o{ FACULTY_COURSE_CONFLICTS : teaches
    EXAMINATION_ROOMS ||--o{ INVIGILATION_ASSIGNMENTS : staffed
    FACULTY ||--o{ INVIGILATION_ASSIGNMENTS : assigned
    EXAMINATIONS ||--o{ INVIGILATION_HISTORY : audited
    USERS ||--o{ INVIGILATION_HISTORY : actor
    USERS |o--o{ NOTIFICATIONS : receives
    EXAMINATIONS |o--o{ NOTIFICATIONS : concerns
```

Annual placement has composite key (student_id,academic_year_id); offering has (course_id,academic_year_id). Course enrollment is unique per student/course/year. Seating is independently unique per exam/student and exam/seat. Room/seat store geometry/accessibility; assignment stores is_manual. Audit stores labels to preserve cleared assignment explanations. The single planner_lock row synchronizes writes. Invigilation is unique per exam/room/slot and per exam/faculty. Duty history retains previous/new faculty and actor/reason. Notification recipients may be staff accounts or student email addresses. Global assignment rules and per-faculty limits are stored separately from duties.
