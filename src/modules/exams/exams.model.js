const pool = require('../../config/db');

async function findAll() {
  const [rows] = await pool.query(
    'SELECT id, exam_name, subject, exam_date, start_time, end_time, created_at, updated_at FROM exams ORDER BY exam_date ASC, start_time ASC'
  );
  return rows;
}

async function findById(id) {
  const [rows] = await pool.query(
    'SELECT id, exam_name, subject, exam_date, start_time, end_time, created_at, updated_at FROM exams WHERE id = ?',
    [id]
  );
  return rows[0] || null;
}

async function create({ exam_name, subject, exam_date, start_time, end_time }) {
  const [result] = await pool.query(
    'INSERT INTO exams (exam_name, subject, exam_date, start_time, end_time) VALUES (?, ?, ?, ?, ?)',
    [exam_name, subject ?? null, exam_date, start_time, end_time]
  );
  return findById(result.insertId);
}

async function update(id, { exam_name, subject, exam_date, start_time, end_time }) {
  await pool.query(
    'UPDATE exams SET exam_name = ?, subject = ?, exam_date = ?, start_time = ?, end_time = ? WHERE id = ?',
    [exam_name, subject ?? null, exam_date, start_time, end_time, id]
  );
  return findById(id);
}

async function remove(id) {
  const [result] = await pool.query('DELETE FROM exams WHERE id = ?', [id]);
  return result.affectedRows > 0;
}

async function isReferencedByAssignment(id) {
  // Assignment table is owned by Player 3; defensive existence check so we
  // never silently delete exam metadata that live assignments depend on.
  try {
    const [rows] = await pool.query(
      "SELECT COUNT(*) as cnt FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'student_assignments'"
    );
    if (!rows[0] || rows[0].cnt === 0) return false;
    const [refs] = await pool.query(
      'SELECT COUNT(*) as cnt FROM student_assignments WHERE exam_id = ?',
      [id]
    );
    return refs[0].cnt > 0;
  } catch (err) {
    return false;
  }
}

module.exports = { findAll, findById, create, update, remove, isReferencedByAssignment };
