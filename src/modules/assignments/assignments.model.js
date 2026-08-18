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

async function createStudentAssignment({ student_id, exam_id, classroom_id, seat_number }) {
  const [result] = await pool.query(
    `INSERT INTO student_assignments (student_id, exam_id, classroom_id, seat_number)
    VALUES (?, ?, ?, ?)`,
    [student_id, exam_id, classroom_id, seat_number]
  );

  return findStudentAssignmentById(result.insertId);
}

async function updateStudentAssignment(id, { student_id, exam_id, classroom_id, seat_number }) {
  await pool.query(
    `UPDATE student_assignments
    SET student_id = ?, exam_id = ?, classroom_id = ?, seat_number = ?
    WHERE id = ?`,
    [student_id, exam_id, classroom_id, seat_number, id]
  );

  return findStudentAssignmentById(id);
}

async function deleteStudentAssignment(id) {
  const [result] = await pool.query('DELETE FROM student_assignments WHERE id = ?', [id]);
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

async function createSupervisorAssignment({ supervisor_id, exam_id, classroom_id, role }) {
  const [result] = await pool.query(
    `INSERT INTO supervisor_assignments (supervisor_id, exam_id, classroom_id, role)
    VALUES (?, ?, ?, ?)`,
    [supervisor_id, exam_id, classroom_id, role]
  );

  return findSupervisorAssignmentById(result.insertId);
}

async function updateSupervisorAssignment(id, { supervisor_id, exam_id, classroom_id, role }) {
  await pool.query(
    `UPDATE supervisor_assignments
    SET supervisor_id = ?, exam_id = ?, classroom_id = ?, role = ?
    WHERE id = ?`,
    [supervisor_id, exam_id, classroom_id, role, id]
  );

  return findSupervisorAssignmentById(id);
}

async function deleteSupervisorAssignment(id) {
  const [result] = await pool.query('DELETE FROM supervisor_assignments WHERE id = ?', [id]);
  return result.affectedRows > 0;
}

async function studentExists(id) {
  const [rows] = await pool.query('SELECT id FROM students WHERE id = ? LIMIT 1', [id]);
  return Boolean(rows[0]);
}

async function supervisorExists(id) {
  const [rows] = await pool.query('SELECT id FROM supervisors WHERE id = ? LIMIT 1', [id]);
  return Boolean(rows[0]);
}

async function examExists(id) {
  const [rows] = await pool.query('SELECT id FROM exams WHERE id = ? LIMIT 1', [id]);
  return Boolean(rows[0]);
}

async function classroomExists(id) {
  const [rows] = await pool.query('SELECT id FROM classrooms WHERE id = ? LIMIT 1', [id]);
  return Boolean(rows[0]);
}

async function findStudentSeatConflict(examId, classroomId, seatNumber, excludeAssignmentId = null) {
  const params = [examId, classroomId, seatNumber];
  let sql = `SELECT id FROM student_assignments
    WHERE exam_id = ? AND classroom_id = ? AND seat_number = ?`;

  if (excludeAssignmentId !== null) {
    sql += ' AND id <> ?';
    params.push(excludeAssignmentId);
  }

  sql += ' LIMIT 1';
  const [rows] = await pool.query(sql, params);
  return rows[0] || null;
}

async function findStudentExamConflict(studentId, examId, excludeAssignmentId = null) {
  const params = [studentId, examId];
  let sql = `SELECT id FROM student_assignments
    WHERE student_id = ? AND exam_id = ?`;

  if (excludeAssignmentId !== null) {
    sql += ' AND id <> ?';
    params.push(excludeAssignmentId);
  }

  sql += ' LIMIT 1';
  const [rows] = await pool.query(sql, params);
  return rows[0] || null;
}

async function getClassroomCapacity(classroomId) {
  const [rows] = await pool.query('SELECT capacity FROM classrooms WHERE id = ?', [classroomId]);
  return rows[0] ? Number(rows[0].capacity) : null;
}

async function countStudentAssignmentsForClassroomExam(examId, classroomId, excludeAssignmentId = null) {
  const params = [examId, classroomId];
  let sql = `SELECT COUNT(*) AS count FROM student_assignments
    WHERE exam_id = ? AND classroom_id = ?`;

  if (excludeAssignmentId !== null) {
    sql += ' AND id <> ?';
    params.push(excludeAssignmentId);
  }

  const [rows] = await pool.query(sql, params);
  return Number(rows[0].count);
}

async function findSupervisorTimeConflict(supervisorId, examId, excludeAssignmentId = null) {
  const params = [examId, supervisorId];
  let sql = `SELECT sa.id
    FROM supervisor_assignments sa
    JOIN exams existing_exam ON existing_exam.id = sa.exam_id
    JOIN exams target_exam ON target_exam.id = ?
    WHERE sa.supervisor_id = ?
      AND existing_exam.exam_date = target_exam.exam_date
      AND existing_exam.start_time < target_exam.end_time
      AND existing_exam.end_time > target_exam.start_time`;

  if (excludeAssignmentId !== null) {
    sql += ' AND sa.id <> ?';
    params.push(excludeAssignmentId);
  }

  sql += ' LIMIT 1';
  const [rows] = await pool.query(sql, params);
  return rows[0] || null;
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
  findStudentSeatConflict,
  findStudentExamConflict,
  getClassroomCapacity,
  countStudentAssignmentsForClassroomExam,
  findSupervisorTimeConflict,
};
