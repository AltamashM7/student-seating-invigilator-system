const pool = require('../../config/db');

async function findAllStudentAssignments() {
  const [rows] = await pool.query(
    `SELECT
      id,
      student_id,
      exam_id,
      classroom_id,
      seat_number,
      created_at,
      updated_at
    FROM student_assignments
    ORDER BY id ASC`
  );

  return rows;
}

async function findStudentAssignmentById(id) {
  const [rows] = await pool.query(
    `SELECT
      id,
      student_id,
      exam_id,
      classroom_id,
      seat_number,
      created_at,
      updated_at
    FROM student_assignments
    WHERE id = ?`,
    [id]
  );

  return rows[0] || null;
}

async function createStudentAssignment({
  student_id,
  exam_id,
  classroom_id,
  seat_number,
}) {
  const [result] = await pool.query(
    `INSERT INTO student_assignments (
      student_id,
      exam_id,
      classroom_id,
      seat_number
    )
    VALUES (?, ?, ?, ?)`,
    [student_id, exam_id, classroom_id, seat_number]
  );

  return findStudentAssignmentById(result.insertId);
}

module.exports = {
  findAllStudentAssignments,
  findStudentAssignmentById,
  createStudentAssignment,
};
