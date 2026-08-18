const assignmentsModel = require('./assignments.model');

class ValidationError extends Error {
  constructor(message) {
    super(message);
    this.name = 'ValidationError';
    this.statusCode = 400;
  }
}

class NotFoundError extends Error {
  constructor(message) {
    super(message);
    this.name = 'NotFoundError';
    this.statusCode = 404;
  }
}

class ConflictError extends Error {
  constructor(message) {
    super(message);
    this.name = 'ConflictError';
    this.statusCode = 409;
  }
}

function validatePositiveInteger(value, fieldName) {
  const numericValue = Number(value);
  if (!Number.isInteger(numericValue) || numericValue <= 0) {
    throw new ValidationError(`${fieldName} must be a positive integer.`);
  }
  return numericValue;
}

function validateSeatNumber(value) {
  if (value === undefined || value === null || !String(value).trim()) {
    throw new ValidationError('seat_number is required.');
  }
  const seatNumber = String(value).trim();
  if (seatNumber.length > 20) {
    throw new ValidationError('seat_number must be 20 characters or fewer.');
  }
  return seatNumber;
}

function validateRole(value) {
  if (value === undefined || value === null || typeof value !== 'string' || !value.trim()) {
    throw new ValidationError('role is required and must be a non-empty string.');
  }
  const role = value.trim();
  if (role.length > 100) {
    throw new ValidationError('role must be 100 characters or fewer.');
  }
  return role;
}

function validateStudentAssignmentPayload(payload) {
  return {
    student_id: validatePositiveInteger(payload.student_id, 'student_id'),
    exam_id: validatePositiveInteger(payload.exam_id, 'exam_id'),
    classroom_id: validatePositiveInteger(payload.classroom_id, 'classroom_id'),
    seat_number: validateSeatNumber(payload.seat_number),
  };
}

function validateSupervisorAssignmentPayload(payload) {
  return {
    supervisor_id: validatePositiveInteger(payload.supervisor_id, 'supervisor_id'),
    exam_id: validatePositiveInteger(payload.exam_id, 'exam_id'),
    classroom_id: validatePositiveInteger(payload.classroom_id, 'classroom_id'),
    role: validateRole(payload.role),
  };
}

async function ensureStudentReferences({ student_id, exam_id, classroom_id }) {
  const [studentExists, examExists, classroomExists] = await Promise.all([
    assignmentsModel.studentExists(student_id),
    assignmentsModel.examExists(exam_id),
    assignmentsModel.classroomExists(classroom_id),
  ]);
  if (!studentExists) throw new NotFoundError('Student not found.');
  if (!examExists) throw new NotFoundError('Exam not found.');
  if (!classroomExists) throw new NotFoundError('Classroom not found.');
}

async function ensureSupervisorReferences({ supervisor_id, exam_id, classroom_id }) {
  const [supervisorExists, examExists, classroomExists] = await Promise.all([
    assignmentsModel.supervisorExists(supervisor_id),
    assignmentsModel.examExists(exam_id),
    assignmentsModel.classroomExists(classroom_id),
  ]);
  if (!supervisorExists) throw new NotFoundError('Supervisor not found.');
  if (!examExists) throw new NotFoundError('Exam not found.');
  if (!classroomExists) throw new NotFoundError('Classroom not found.');
}

async function ensureStudentSafety(assignment, excludeAssignmentId = null) {
  const [seatConflict, studentExamConflict, classroomCapacity, assignedCount] = await Promise.all([
    assignmentsModel.findStudentSeatConflict(
      assignment.exam_id,
      assignment.classroom_id,
      assignment.seat_number,
      excludeAssignmentId
    ),
    assignmentsModel.findStudentExamConflict(
      assignment.student_id,
      assignment.exam_id,
      excludeAssignmentId
    ),
    assignmentsModel.getClassroomCapacity(assignment.classroom_id),
    assignmentsModel.countStudentAssignmentsForClassroomExam(
      assignment.exam_id,
      assignment.classroom_id,
      excludeAssignmentId
    ),
  ]);

  if (seatConflict) {
    throw new ConflictError('Seat is already assigned for this exam and classroom.');
  }
  if (studentExamConflict) {
    throw new ConflictError('Student already has an assignment for this exam.');
  }
  if (classroomCapacity === null) {
    throw new NotFoundError('Classroom not found.');
  }
  if (assignedCount >= classroomCapacity) {
    throw new ConflictError('Classroom capacity has been reached for this exam.');
  }
}

async function ensureSupervisorSafety(assignment, excludeAssignmentId = null) {
  const conflict = await assignmentsModel.findSupervisorTimeConflict(
    assignment.supervisor_id,
    assignment.exam_id,
    excludeAssignmentId
  );
  if (conflict) {
    throw new ConflictError('Supervisor already has an assignment during this exam time.');
  }
}

function translateDatabaseConflict(err) {
  if (err && err.code === 'ER_DUP_ENTRY') {
    throw new ConflictError('Assignment conflicts with an existing assignment.');
  }
  throw err;
}

async function listStudentAssignments() {
  return assignmentsModel.findAllStudentAssignments();
}

async function getStudentAssignment(id) {
  const assignmentId = validatePositiveInteger(id, 'assignment id');
  const assignment = await assignmentsModel.findStudentAssignmentById(assignmentId);
  if (!assignment) throw new NotFoundError('Student assignment not found.');
  return assignment;
}

async function createStudentAssignment(payload) {
  const assignment = validateStudentAssignmentPayload(payload);
  await ensureStudentReferences(assignment);
  await ensureStudentSafety(assignment);
  try {
    return await assignmentsModel.createStudentAssignment(assignment);
  } catch (err) {
    return translateDatabaseConflict(err);
  }
}

async function updateStudentAssignment(id, payload) {
  const assignmentId = validatePositiveInteger(id, 'assignment id');
  const existing = await assignmentsModel.findStudentAssignmentById(assignmentId);
  if (!existing) throw new NotFoundError('Student assignment not found.');

  const merged = validateStudentAssignmentPayload({
    student_id: payload.student_id !== undefined ? payload.student_id : existing.student_id,
    exam_id: payload.exam_id !== undefined ? payload.exam_id : existing.exam_id,
    classroom_id: payload.classroom_id !== undefined ? payload.classroom_id : existing.classroom_id,
    seat_number: payload.seat_number !== undefined ? payload.seat_number : existing.seat_number,
  });

  await ensureStudentReferences(merged);
  await ensureStudentSafety(merged, assignmentId);

  try {
    return await assignmentsModel.updateStudentAssignment(assignmentId, merged);
  } catch (err) {
    return translateDatabaseConflict(err);
  }
}

async function deleteStudentAssignment(id) {
  const assignmentId = validatePositiveInteger(id, 'assignment id');
  const existing = await assignmentsModel.findStudentAssignmentById(assignmentId);
  if (!existing) throw new NotFoundError('Student assignment not found.');
  await assignmentsModel.deleteStudentAssignment(assignmentId);
}

async function listSupervisorAssignments() {
  return assignmentsModel.findAllSupervisorAssignments();
}

async function getSupervisorAssignment(id) {
  const assignmentId = validatePositiveInteger(id, 'assignment id');
  const assignment = await assignmentsModel.findSupervisorAssignmentById(assignmentId);
  if (!assignment) throw new NotFoundError('Supervisor assignment not found.');
  return assignment;
}

async function createSupervisorAssignment(payload) {
  const assignment = validateSupervisorAssignmentPayload(payload);
  await ensureSupervisorReferences(assignment);
  await ensureSupervisorSafety(assignment);
  try {
    return await assignmentsModel.createSupervisorAssignment(assignment);
  } catch (err) {
    return translateDatabaseConflict(err);
  }
}

async function updateSupervisorAssignment(id, payload) {
  const assignmentId = validatePositiveInteger(id, 'assignment id');
  const existing = await assignmentsModel.findSupervisorAssignmentById(assignmentId);
  if (!existing) throw new NotFoundError('Supervisor assignment not found.');

  const merged = validateSupervisorAssignmentPayload({
    supervisor_id: payload.supervisor_id !== undefined ? payload.supervisor_id : existing.supervisor_id,
    exam_id: payload.exam_id !== undefined ? payload.exam_id : existing.exam_id,
    classroom_id: payload.classroom_id !== undefined ? payload.classroom_id : existing.classroom_id,
    role: payload.role !== undefined ? payload.role : existing.role,
  });

  await ensureSupervisorReferences(merged);
  await ensureSupervisorSafety(merged, assignmentId);

  try {
    return await assignmentsModel.updateSupervisorAssignment(assignmentId, merged);
  } catch (err) {
    return translateDatabaseConflict(err);
  }
}

async function deleteSupervisorAssignment(id) {
  const assignmentId = validatePositiveInteger(id, 'assignment id');
  const existing = await assignmentsModel.findSupervisorAssignmentById(assignmentId);
  if (!existing) throw new NotFoundError('Supervisor assignment not found.');
  await assignmentsModel.deleteSupervisorAssignment(assignmentId);
}

module.exports = {
  listStudentAssignments,
  getStudentAssignment,
  createStudentAssignment,
  updateStudentAssignment,
  deleteStudentAssignment,
  listSupervisorAssignments,
  getSupervisorAssignment,
  createSupervisorAssignment,
  updateSupervisorAssignment,
  deleteSupervisorAssignment,
  ValidationError,
  NotFoundError,
  ConflictError,
};
