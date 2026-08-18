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

function validatePositiveInteger(value, fieldName) {
  const numericValue = Number(value);

  if (!Number.isInteger(numericValue) || numericValue <= 0) {
    throw new ValidationError(`${fieldName} must be a positive integer.`);
  }

  return numericValue;
}

function validateSeatNumber(value) {
  if (
    value === undefined ||
    value === null ||
    !String(value).trim()
  ) {
    throw new ValidationError('seat_number is required.');
  }

  return String(value).trim();
}

function validateRole(value) {
  if (
    value === undefined ||
    value === null ||
    typeof value !== 'string' ||
    !value.trim()
  ) {
    throw new ValidationError('role is required and must be a non-empty string.');
  }

  return value.trim();
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
    supervisor_id: validatePositiveInteger(
      payload.supervisor_id,
      'supervisor_id'
    ),
    exam_id: validatePositiveInteger(payload.exam_id, 'exam_id'),
    classroom_id: validatePositiveInteger(payload.classroom_id, 'classroom_id'),
    role: validateRole(payload.role),
  };
}

async function ensureStudentReferences({
  student_id,
  exam_id,
  classroom_id,
}) {
  const [studentExists, examExists, classroomExists] = await Promise.all([
    assignmentsModel.studentExists(student_id),
    assignmentsModel.examExists(exam_id),
    assignmentsModel.classroomExists(classroom_id),
  ]);

  if (!studentExists) {
    throw new NotFoundError('Student not found.');
  }

  if (!examExists) {
    throw new NotFoundError('Exam not found.');
  }

  if (!classroomExists) {
    throw new NotFoundError('Classroom not found.');
  }
}

async function ensureSupervisorReferences({
  supervisor_id,
  exam_id,
  classroom_id,
}) {
  const [supervisorExists, examExists, classroomExists] = await Promise.all([
    assignmentsModel.supervisorExists(supervisor_id),
    assignmentsModel.examExists(exam_id),
    assignmentsModel.classroomExists(classroom_id),
  ]);

  if (!supervisorExists) {
    throw new NotFoundError('Supervisor not found.');
  }

  if (!examExists) {
    throw new NotFoundError('Exam not found.');
  }

  if (!classroomExists) {
    throw new NotFoundError('Classroom not found.');
  }
}

async function listStudentAssignments() {
  return assignmentsModel.findAllStudentAssignments();
}

async function getStudentAssignment(id) {
  const assignmentId = validatePositiveInteger(id, 'assignment id');
  const assignment = await assignmentsModel.findStudentAssignmentById(
    assignmentId
  );

  if (!assignment) {
    throw new NotFoundError('Student assignment not found.');
  }

  return assignment;
}

async function createStudentAssignment(payload) {
  const assignment = validateStudentAssignmentPayload(payload);
  await ensureStudentReferences(assignment);

  return assignmentsModel.createStudentAssignment(assignment);
}

async function updateStudentAssignment(id, payload) {
  const assignmentId = validatePositiveInteger(id, 'assignment id');
  const existing = await assignmentsModel.findStudentAssignmentById(
    assignmentId
  );

  if (!existing) {
    throw new NotFoundError('Student assignment not found.');
  }

  const merged = validateStudentAssignmentPayload({
    student_id:
      payload.student_id !== undefined
        ? payload.student_id
        : existing.student_id,
    exam_id:
      payload.exam_id !== undefined
        ? payload.exam_id
        : existing.exam_id,
    classroom_id:
      payload.classroom_id !== undefined
        ? payload.classroom_id
        : existing.classroom_id,
    seat_number:
      payload.seat_number !== undefined
        ? payload.seat_number
        : existing.seat_number,
  });

  await ensureStudentReferences(merged);

  return assignmentsModel.updateStudentAssignment(
    assignmentId,
    merged
  );
}

async function deleteStudentAssignment(id) {
  const assignmentId = validatePositiveInteger(id, 'assignment id');
  const existing = await assignmentsModel.findStudentAssignmentById(
    assignmentId
  );

  if (!existing) {
    throw new NotFoundError('Student assignment not found.');
  }

  await assignmentsModel.deleteStudentAssignment(assignmentId);
}

async function listSupervisorAssignments() {
  return assignmentsModel.findAllSupervisorAssignments();
}

async function getSupervisorAssignment(id) {
  const assignmentId = validatePositiveInteger(id, 'assignment id');
  const assignment = await assignmentsModel.findSupervisorAssignmentById(
    assignmentId
  );

  if (!assignment) {
    throw new NotFoundError('Supervisor assignment not found.');
  }

  return assignment;
}

async function createSupervisorAssignment(payload) {
  const assignment = validateSupervisorAssignmentPayload(payload);
  await ensureSupervisorReferences(assignment);

  return assignmentsModel.createSupervisorAssignment(assignment);
}

async function updateSupervisorAssignment(id, payload) {
  const assignmentId = validatePositiveInteger(id, 'assignment id');
  const existing = await assignmentsModel.findSupervisorAssignmentById(
    assignmentId
  );

  if (!existing) {
    throw new NotFoundError('Supervisor assignment not found.');
  }

  const merged = validateSupervisorAssignmentPayload({
    supervisor_id:
      payload.supervisor_id !== undefined
        ? payload.supervisor_id
        : existing.supervisor_id,
    exam_id:
      payload.exam_id !== undefined
        ? payload.exam_id
        : existing.exam_id,
    classroom_id:
      payload.classroom_id !== undefined
        ? payload.classroom_id
        : existing.classroom_id,
    role:
      payload.role !== undefined
        ? payload.role
        : existing.role,
  });

  await ensureSupervisorReferences(merged);

  return assignmentsModel.updateSupervisorAssignment(
    assignmentId,
    merged
  );
}

async function deleteSupervisorAssignment(id) {
  const assignmentId = validatePositiveInteger(id, 'assignment id');
  const existing = await assignmentsModel.findSupervisorAssignmentById(
    assignmentId
  );

  if (!existing) {
    throw new NotFoundError('Supervisor assignment not found.');
  }

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
};
