class AllocationError extends Error {
  constructor(message, statusCode = 400) {
    super(message);
    this.name = 'AllocationError';
    this.statusCode = statusCode;
  }
}

function normalizePositiveInteger(value, fieldName) {
  const numericValue = Number(value);

  if (!Number.isInteger(numericValue) || numericValue <= 0) {
    throw new AllocationError(`${fieldName} must be a positive integer.`);
  }

  return numericValue;
}

function normalizeStudents(students) {
  if (!Array.isArray(students) || students.length === 0) {
    throw new AllocationError('At least one student is required.');
  }

  const ids = students.map((student) =>
    normalizePositiveInteger(
      typeof student === 'object' ? student.id : student,
      'student id'
    )
  );

  if (new Set(ids).size !== ids.length) {
    throw new AllocationError('Student list contains duplicate IDs.');
  }

  return ids;
}

function normalizeClassrooms(classrooms) {
  if (!Array.isArray(classrooms) || classrooms.length === 0) {
    throw new AllocationError('At least one classroom is required.');
  }

  const normalized = classrooms.map((classroom) => ({
    id: normalizePositiveInteger(classroom.id, 'classroom id'),
    capacity: normalizePositiveInteger(
      classroom.capacity,
      'classroom capacity'
    ),
  }));

  const ids = normalized.map((classroom) => classroom.id);

  if (new Set(ids).size !== ids.length) {
    throw new AllocationError('Classroom list contains duplicate IDs.');
  }

  return normalized;
}

function generateSeatNumber(position, prefix = '') {
  const safePrefix = prefix === undefined || prefix === null
    ? ''
    : String(prefix).trim();

  return `${safePrefix}${position}`;
}

function generateSeatingPlan({
  students,
  exam_id,
  classrooms,
  seat_prefix = '',
}) {
  const examId = normalizePositiveInteger(exam_id, 'exam_id');
  const studentIds = normalizeStudents(students);
  const rooms = normalizeClassrooms(classrooms);
  const totalCapacity = rooms.reduce(
    (sum, classroom) => sum + classroom.capacity,
    0
  );

  if (studentIds.length > totalCapacity) {
    throw new AllocationError(
      `Insufficient classroom capacity: ${studentIds.length} students for ${totalCapacity} seats.`,
      409
    );
  }

  const assignments = [];
  let studentIndex = 0;

  for (const classroom of rooms) {
    for (
      let seatPosition = 1;
      seatPosition <= classroom.capacity &&
      studentIndex < studentIds.length;
      seatPosition += 1
    ) {
      assignments.push({
        student_id: studentIds[studentIndex],
        exam_id: examId,
        classroom_id: classroom.id,
        seat_number: generateSeatNumber(
          seatPosition,
          seat_prefix
        ),
      });

      studentIndex += 1;
    }

    if (studentIndex >= studentIds.length) {
      break;
    }
  }

  return {
    exam_id: examId,
    student_count: studentIds.length,
    total_capacity: totalCapacity,
    assignments,
  };
}

module.exports = {
  generateSeatingPlan,
  AllocationError,
};
