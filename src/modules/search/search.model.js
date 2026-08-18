const pool = require('../../config/db');

async function searchStudents(query) {
  const likeQuery = `%${query}%`;

  const [rows] = await pool.query(
    `SELECT
      s.id AS person_id,
      s.name,
      s.student_id,
      s.roll_number,
      s.department,
      s.year,
      s.division,
      sa.id AS assignment_id,
      sa.seat_number,
      c.id AS classroom_id,
      c.room_number,
      c.building,
      c.floor,
      e.id AS exam_id,
      e.exam_name,
      e.subject,
      e.exam_date,
      e.start_time,
      e.end_time
    FROM students s
    JOIN student_assignments sa ON sa.student_id = s.id
    JOIN classrooms c ON c.id = sa.classroom_id
    JOIN exams e ON e.id = sa.exam_id
    WHERE s.name LIKE ?
      OR s.student_id = ?
      OR s.roll_number = ?
    ORDER BY e.exam_date ASC, e.start_time ASC, s.name ASC`,
    [likeQuery, query, query]
  );

  return rows;
}

async function searchSupervisors(query) {
  const likeQuery = `%${query}%`;

  const [rows] = await pool.query(
    `SELECT
      s.id AS person_id,
      s.name,
      s.employee_id,
      s.department,
      sa.id AS assignment_id,
      sa.role AS assignment_role,
      c.id AS classroom_id,
      c.room_number,
      c.building,
      c.floor,
      e.id AS exam_id,
      e.exam_name,
      e.subject,
      e.exam_date,
      e.start_time,
      e.end_time
    FROM supervisors s
    JOIN supervisor_assignments sa ON sa.supervisor_id = s.id
    JOIN classrooms c ON c.id = sa.classroom_id
    JOIN exams e ON e.id = sa.exam_id
    WHERE s.name LIKE ?
      OR s.employee_id = ?
    ORDER BY e.exam_date ASC, e.start_time ASC, s.name ASC`,
    [likeQuery, query]
  );

  return rows;
}

module.exports = { searchStudents, searchSupervisors };
