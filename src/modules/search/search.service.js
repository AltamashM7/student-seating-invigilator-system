const searchModel = require('./search.model');

class ValidationError extends Error {
  constructor(message) {
    super(message);
    this.name = 'ValidationError';
    this.statusCode = 400;
  }
}

function validateQuery(value) {
  if (value === undefined || value === null) {
    throw new ValidationError('Search query q is required.');
  }

  const query = String(value).trim();

  if (!query) {
    throw new ValidationError('Search query q is required.');
  }

  if (query.length > 100) {
    throw new ValidationError('Search query q must be 100 characters or fewer.');
  }

  return query;
}

function mapStudentResult(row) {
  return {
    role: 'student',
    person_id: row.person_id,
    name: row.name,
    student_id: row.student_id,
    roll_number: row.roll_number,
    department: row.department,
    year: row.year,
    division: row.division,
    assignment: {
      id: row.assignment_id,
      seat_number: row.seat_number,
      classroom: {
        id: row.classroom_id,
        room_number: row.room_number,
        building: row.building,
        floor: row.floor,
      },
      exam: {
        id: row.exam_id,
        exam_name: row.exam_name,
        subject: row.subject,
        exam_date: row.exam_date,
        start_time: row.start_time,
        end_time: row.end_time,
      },
    },
  };
}

function mapSupervisorResult(row) {
  return {
    role: 'supervisor',
    person_id: row.person_id,
    name: row.name,
    employee_id: row.employee_id,
    department: row.department,
    assignment: {
      id: row.assignment_id,
      role: row.assignment_role,
      classroom: {
        id: row.classroom_id,
        room_number: row.room_number,
        building: row.building,
        floor: row.floor,
      },
      exam: {
        id: row.exam_id,
        exam_name: row.exam_name,
        subject: row.subject,
        exam_date: row.exam_date,
        start_time: row.start_time,
        end_time: row.end_time,
      },
    },
  };
}

async function search(queryValue) {
  const query = validateQuery(queryValue);

  const [studentRows, supervisorRows] = await Promise.all([
    searchModel.searchStudents(query),
    searchModel.searchSupervisors(query),
  ]);

  return [
    ...studentRows.map(mapStudentResult),
    ...supervisorRows.map(mapSupervisorResult),
  ];
}

module.exports = { search, ValidationError };
