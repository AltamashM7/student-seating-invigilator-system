# Player 3 API Contract

## Status

Player 3 implementation is independently complete through Sprints 1-6 and the
independent portion of Sprint 7 hardening.

The following Player 3-owned areas are implemented:

- Student assignment CRUD.
- Supervisor assignment CRUD.
- Public assignment search.
- Assignment conflict validation.
- Student seating generator foundation.
- Invigilator generator foundation.
- Independent regression verification.

**Final integration still required** after Player 1 supplies the real student,
supervisor, and admin-authentication implementation.

## Response Envelope

Successful responses use:

```json
{
  "success": true,
  "data": {}
}
```

Errors use:

```json
{
  "success": false,
  "error": {
    "message": "Human-readable message"
  }
}
```

Common status codes:

- `200` - successful read/update/delete.
- `201` - successful creation.
- `400` - invalid input.
- `401` - admin authentication required.
- `404` - assignment or referenced record not found.
- `409` - assignment conflict.
- `500` - unexpected server error.

## Student Assignment API

Base path:

```text
/api/admin/assignments/students
```

Endpoints:

```http
GET    /api/admin/assignments/students
GET    /api/admin/assignments/students/:id
POST   /api/admin/assignments/students
PUT    /api/admin/assignments/students/:id
DELETE /api/admin/assignments/students/:id
```

`POST`, `PUT`, and `DELETE` require the project's shared `requireAdmin`
middleware.

Create body:

```json
{
  "student_id": 1,
  "exam_id": 1,
  "classroom_id": 1,
  "seat_number": "A1"
}
```

`PUT` supports partial updates. Omitted fields retain their existing values.

Student assignment safety rules:

- Student, exam, and classroom references must exist.
- Seat number is required.
- Seat number is limited to 20 characters.
- A seat cannot be assigned twice within the same exam and classroom.
- A student cannot receive two assignments for the same exam.
- Assignments cannot exceed classroom capacity.
- Database uniqueness constraints backstop application conflict checks.

## Supervisor Assignment API

Base path:

```text
/api/admin/assignments/supervisors
```

Endpoints:

```http
GET    /api/admin/assignments/supervisors
GET    /api/admin/assignments/supervisors/:id
POST   /api/admin/assignments/supervisors
PUT    /api/admin/assignments/supervisors/:id
DELETE /api/admin/assignments/supervisors/:id
```

`POST`, `PUT`, and `DELETE` require the project's shared `requireAdmin`
middleware.

Create body:

```json
{
  "supervisor_id": 1,
  "exam_id": 1,
  "classroom_id": 1,
  "role": "Invigilator"
}
```

Supervisor assignment safety rules:

- Supervisor, exam, and classroom references must exist.
- Role is required and is limited to 100 characters.
- A supervisor cannot be allocated to overlapping exam times.
- Database uniqueness prevents duplicate supervisor/exam rows.

## Public Search API

Endpoint:

```http
GET /api/search?q=
```

This route is public and read-only. No public `POST`, `PUT`, `PATCH`, or
`DELETE` search endpoint exists.

Supported lookup fields are implemented against the agreed Player 1 contract:

Students:

```text
name
student_id
roll_number
```

Supervisors:

```text
name
employee_id
```

A no-result query returns:

```json
{
  "success": true,
  "data": []
}
```

Duplicate names are not collapsed. Each matching result contains identifying
fields so the frontend can distinguish people.

Example student-shaped item:

```json
{
  "role": "student",
  "person_id": 1,
  "name": "Rahul Patil",
  "student_id": "ST001",
  "roll_number": "R001",
  "department": "CSE",
  "year": 2,
  "division": "A",
  "assignment": {
    "id": 10,
    "seat_number": "A1",
    "classroom": {
      "id": 3,
      "room_number": "A-201",
      "building": "Main",
      "floor": 2
    },
    "exam": {
      "id": 4,
      "exam_name": "Data Structures",
      "subject": "DS",
      "exam_date": "2026-08-20",
      "start_time": "10:00:00",
      "end_time": "12:00:00"
    }
  }
}
```

Example supervisor-shaped item:

```json
{
  "role": "supervisor",
  "person_id": 9,
  "name": "Prof. Sharma",
  "employee_id": "EMP009",
  "department": "CSE",
  "assignment": {
    "id": 20,
    "role": "Chief Invigilator",
    "classroom": {
      "id": 3,
      "room_number": "A-201",
      "building": "Main",
      "floor": 2
    },
    "exam": {
      "id": 4,
      "exam_name": "Data Structures",
      "subject": "DS",
      "exam_date": "2026-08-20",
      "start_time": "10:00:00",
      "end_time": "12:00:00"
    }
  }
}
```

## Automatic Allocation Foundation

The generator services currently produce deterministic allocation plans; they
do not expose HTTP endpoints and do not write directly to the database.

Files:

```text
src/modules/assignments/seating-generator.service.js
src/modules/assignments/invigilator-generator.service.js
```

This keeps Sprint 6 independent while Player 1's final data implementation is
still unavailable.

A future production allocator can use these generators as the planning layer
and then persist the plan inside a database transaction.

## Player 1 Integration Contract

Player 3 currently expects the agreed Player 1 fields used by assignment
validation and public search.

Student-facing fields:

```text
id
student_id
name
roll_number
department
year
division
```

Supervisor-facing fields:

```text
id
employee_id
name
department
```

Admin authentication must ultimately establish the session state expected by
the shared `requireAdmin` middleware.

If Player 1's final schema differs, adapt the Player 3 integration queries at
the integration boundary rather than creating duplicate Student/Supervisor
tables.

## Player 2 Integration Contract

Player 3 consumes the existing Player 2 records and fields needed by
assignments/search:

Exams:

```text
id
exam_name
subject
exam_date
start_time
end_time
```

Classrooms:

```text
id
room_number
building
floor
capacity
```

## Verification

Run the complete Player 3 independent verification suite with:

```powershell
npm run verify:player3
```

This performs:

- Syntax checks for Player 3 JavaScript.
- Sprint 2-3 CRUD validation.
- Sprint 4-5 search and conflict validation.
- Sprint 6 generator validation.
- Sprint 7A hardening checks.
- Public-search read-only verification.
- Mutation authentication-middleware verification.
- No-result and duplicate-name search checks.
- Invalid assignment ID checks.
- Parameterized-SQL checks.
- Player 3 database constraint/index checks.

## Deferred Final Integration Checks

These checks are intentionally not claimed as complete yet:

- Execute migrations `006` and `007` against the final combined database.
- Verify real foreign keys against Player 1's `students` and `supervisors`.
- Verify a real admin login creates the session expected by `requireAdmin`.
- Exercise all APIs over HTTP against real seeded database records.
- Confirm real student and supervisor public searches.
- Confirm real database duplicate-seat/capacity/conflict behavior.
- Reconcile any final Player 1 schema-name differences.
- Perform the final frontend integration handoff.

Those checks belong to Sprint 7B after Player 1's implementation is available.
