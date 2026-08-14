const pool = require('../../config/db');

async function findAll() {
  const [rows] = await pool.query(
    'SELECT id, room_number, building, floor, capacity, created_at, updated_at FROM classrooms ORDER BY id ASC'
  );
  return rows;
}

async function findById(id) {
  const [rows] = await pool.query(
    'SELECT id, room_number, building, floor, capacity, created_at, updated_at FROM classrooms WHERE id = ?',
    [id]
  );
  return rows[0] || null;
}

async function findByRoomNumber(roomNumber, excludeId = null) {
  const params = [roomNumber];
  let sql = 'SELECT id FROM classrooms WHERE room_number = ?';
  if (excludeId) {
    sql += ' AND id != ?';
    params.push(excludeId);
  }
  const [rows] = await pool.query(sql, params);
  return rows[0] || null;
}

async function create({ room_number, building, floor, capacity }) {
  const [result] = await pool.query(
    'INSERT INTO classrooms (room_number, building, floor, capacity) VALUES (?, ?, ?, ?)',
    [room_number, building ?? null, floor ?? null, capacity]
  );
  return findById(result.insertId);
}

async function update(id, { room_number, building, floor, capacity }) {
  await pool.query(
    'UPDATE classrooms SET room_number = ?, building = ?, floor = ?, capacity = ? WHERE id = ?',
    [room_number, building ?? null, floor ?? null, capacity, id]
  );
  return findById(id);
}

async function remove(id) {
  const [result] = await pool.query('DELETE FROM classrooms WHERE id = ?', [id]);
  return result.affectedRows > 0;
}

async function isReferencedByAssignment(id) {
  // Assignment table is owned by Player 3. This check is defensive so we
  // never silently delete a classroom that active assignments depend on.
  // It only runs if the table exists; otherwise it's a no-op.
  try {
    const [rows] = await pool.query(
      "SELECT COUNT(*) as cnt FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'student_assignments'"
    );
    if (!rows[0] || rows[0].cnt === 0) return false;
    const [refs] = await pool.query(
      'SELECT COUNT(*) as cnt FROM student_assignments WHERE classroom_id = ?',
      [id]
    );
    return refs[0].cnt > 0;
  } catch (err) {
    // If the referencing table/column doesn't exist yet, treat as not referenced.
    return false;
  }
}

module.exports = {
  findAll,
  findById,
  findByRoomNumber,
  create,
  update,
  remove,
  isReferencedByAssignment,
};
