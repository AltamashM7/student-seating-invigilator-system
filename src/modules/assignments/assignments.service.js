const assignmentsModel = require('./assignments.model');

class ValidationError extends Error {
  constructor(message) {
    super(message);
    this.name = 'ValidationError';
    this.statusCode = 400;
  }
}

function validatePositiveInteger(value, fieldName) {
  const numericValue = Number(value);

  if (!Number.isInteger(numericValue) || numericValue <= 0) {
    throw new ValidationError(`${fieldName} must be a positive integer.`);
  }

  return numericValue;
}

function validateStudentAssignmentPayload(payload) {
  const studentId = validatePositiveInteger(
    payload.student_id,
    'student_id'
  );

  const examId = validatePositiveInteger(
    payload.exam_id,
    'exam_id'
  );

  const classroomId = validatePositiveInteger(
    payload.classroom_id,
    'classroom_id'
  );

  if (
    payload.seat_number === undefined ||
    payload.seat_number === null ||
    !String(payload.seat_number).trim()
  ) {
    throw new ValidationError('seat_number is required.');
  }

  return {
    student_id: studentId,
    exam_id: examId,
    classroom_id: classroomId,
    seat_number: String(payload.seat_number).trim(),
  };
}

async function listStudentAssignments() {
  return assignmentsModel.findAllStudentAssignments();
}

async function createStudentAssignment(payload) {
  const assignment = validateStudentAssignmentPayload(payload);

  return assignmentsModel.createStudentAssignment(assignment);
}

module.exports = {
  listStudentAssignments,
  createStudentAssignment,
  ValidationError,
};
