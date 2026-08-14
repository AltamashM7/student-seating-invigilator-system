# Seating Arrangement System — Backend

## Player 2: Examination Infrastructure

Owns `classrooms` and `exams`.

### Setup
```
npm install
cp .env.example .env   # fill in DB credentials
node server.js
```

### Endpoints

**Classrooms**
- `GET /api/admin/classrooms`
- `GET /api/admin/classrooms/:id`
- `POST /api/admin/classrooms` (admin) — `{ room_number, building?, floor?, capacity }`
- `PUT /api/admin/classrooms/:id` (admin)
- `DELETE /api/admin/classrooms/:id` (admin)

**Exams**
- `GET /api/admin/exams`
- `GET /api/admin/exams/:id`
- `POST /api/admin/exams` (admin) — `{ exam_name, subject?, exam_date, start_time, end_time }`
- `PUT /api/admin/exams/:id` (admin)
- `DELETE /api/admin/exams/:id` (admin)

Admin-only routes expect `req.session.admin` to already be set by Player 1's
auth flow (`requireAdmin` middleware is a thin guard, not the login logic).

### Validation
- `room_number` required, unique, `capacity > 0`, `floor` optional integer.
- `exam_date`, `start_time`, `end_time` required; `end_time` must be after `start_time`.
- All queries are parameterized; responses use a shared `{ success, data }` /
  `{ success: false, error }` envelope (`src/utils/response.js`).
- Deleting a classroom/exam checks for references in `student_assignments`
  (Player 3's table) first and refuses the delete instead of cascading
  silently.
