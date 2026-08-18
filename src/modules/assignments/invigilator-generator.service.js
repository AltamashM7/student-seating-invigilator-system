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

function normalizeIdList(items, label) {
  if (!Array.isArray(items) || items.length === 0) {
    throw new AllocationError(`At least one ${label} is required.`);
  }

  const ids = items.map((item) =>
    normalizePositiveInteger(
      typeof item === 'object' ? item.id : item,
      `${label} id`
    )
  );

  if (new Set(ids).size !== ids.length) {
    throw new AllocationError(`${label} list contains duplicate IDs.`);
  }

  return ids;
}

function normalizeRole(role) {
  if (role === undefined || role === null || !String(role).trim()) {
    return 'Invigilator';
  }

  const normalized = String(role).trim();

  if (normalized.length > 100) {
    throw new AllocationError(
      'role must be 100 characters or fewer.'
    );
  }

  return normalized;
}

function generateInvigilatorPlan({
  supervisors,
  exam_id,
  classrooms,
  supervisors_per_room = 1,
  role = 'Invigilator',
}) {
  const examId = normalizePositiveInteger(exam_id, 'exam_id');
  const supervisorIds = normalizeIdList(
    supervisors,
    'supervisor'
  );
  const classroomIds = normalizeIdList(
    classrooms,
    'classroom'
  );
  const perRoom = normalizePositiveInteger(
    supervisors_per_room,
    'supervisors_per_room'
  );
  const normalizedRole = normalizeRole(role);
  const requiredSupervisors = classroomIds.length * perRoom;

  if (supervisorIds.length < requiredSupervisors) {
    throw new AllocationError(
      `Insufficient supervisors: ${requiredSupervisors} required but ${supervisorIds.length} available.`,
      409
    );
  }

  const assignments = [];
  let supervisorIndex = 0;

  for (const classroomId of classroomIds) {
    for (let slot = 0; slot < perRoom; slot += 1) {
      assignments.push({
        supervisor_id: supervisorIds[supervisorIndex],
        exam_id: examId,
        classroom_id: classroomId,
        role: normalizedRole,
      });

      supervisorIndex += 1;
    }
  }

  return {
    exam_id: examId,
    classroom_count: classroomIds.length,
    supervisors_per_room: perRoom,
    assignments,
  };
}

module.exports = {
  generateInvigilatorPlan,
  AllocationError,
};
