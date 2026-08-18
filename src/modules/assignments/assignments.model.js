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

async function updateStudentAssignment(
  id,
  { student_id, exam_id, classroom_id, seat_number }
) {
  await pool.query(
    `UPDATE student_assignments
    SET student_id = ?, exam_id = ?, classroom_id = ?, seat_number = ?
    WHERE id = ?`,
    [student_id, exam_id, classroom_id, seat_number, id]
  );

  return findStudentAssignmentById(id);
}

async function deleteStudentAssignment(id) {
  const [result] = await pool.query(
    'DELETE FROM student_assignments WHERE id = ?',
    [id]
  );

  return result.affectedRows > 0;
}

async function findAllSupervisorAssignments() {
  const [rows] = await pool.query(
    `SELECT
      id,
      supervisor_id,
      exam_id,
      classroom_id,
      role,
      created_at,
      updated_at
    FROM supervisor_assignments
    ORDER BY id ASC`
  );

  return rows;
}

async function findSupervisorAssignmentById(id) {
  const [rows] = await pool.query(
    `SELECT
      id,
      supervisor_id,
      exam_id,
      classroom_id,
      role,
      created_at,
      updated_at
    FROM supervisor_assignments
    WHERE id = ?`,
    [id]
  );

  return rows[0] || null;
}

async function createSupervisorAssignment({
  supervisor_id,
  exam_id,
  classroom_id,
  role,
}) {
  const [result] = await pool.query(
    `INSERT INTO supervisor_assignments (
      supervisor_id,
      exam_id,
      classroom_id,
      role
    )
    VALUES (?, ?, ?, ?)`,
    [supervisor_id, exam_id, classroom_id, role]
  );

  return findSupervisorAssignmentById(result.insertId);
}

async function updateSupervisorAssignment(
  id,
  { supervisor_id, exam_id, classroom_id, role }
) {
  await pool.query(
    `UPDATE supervisor_assignments
    SET supervisor_id = ?, exam_id = ?, classroom_id = ?, role = ?
    WHERE id = ?`,
    [supervisor_id, exam_id, classroom_id, role, id]
  );

  return findSupervisorAssignmentById(id);
}

async function deleteSupervisorAssignment(id) {
  const [result] = await pool.query(
    'DELETE FROM supervisor_assignments WHERE id = ?',
    [id]
  );

  return result.affectedRows > 0;
}

async function studentExists(id) {
  const [rows] = await pool.query(
    'SELECT id FROM students WHERE id = ? LIMIT 1',
    [id]
  );

  return Boolean(rows[0]);
}

async function supervisorExists(id) {
  const [rows] = await pool.query(
    'SELECT id FROM supervisors WHERE id = ? LIMIT 1',
    [id]
  );

  return Boolean(rows[0]);
}

async function examExists(id) {
  const [rows] = await pool.query(
    'SELECT id FROM exams WHERE id = ? LIMIT 1',
    [id]
  );

  return Boolean(rows[0]);
}

async function classroomExists(id) {
  const [rows] = await pool.query(
    'SELECT id FROM classrooms WHERE id = ? LIMIT 1',
    [id]
  );

  return Boolean(rows[0]);
}

module.exports = {
  findAllStudentAssignments,
  findStudentAssignmentById,
  createStudentAssignment,
  updateStudentAssignment,
  deleteStudentAssignment,
  findAllSupervisorAssignments,
  findSupervisorAssignmentById,
  createSupervisorAssignment,
  updateSupervisorAssignment,
  deleteSupervisorAssignment,
  studentExists,
  supervisorExists,
  examExists,
  classroomExists,
};
