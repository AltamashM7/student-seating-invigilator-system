const assert = require('assert');
const fs = require('fs');
const path = require('path');

const assignmentModel = require('../src/modules/assignments/assignments.model');
const assignmentService = require('../src/modules/assignments/assignments.service');
const searchModel = require('../src/modules/search/search.model');
const searchService = require('../src/modules/search/search.service');
const searchRoutes = require('../src/modules/search/search.routes');

async function expectReject(promise, statusCode, messagePart) {
  let caught = null;

  try {
    await promise;
  } catch (err) {
    caught = err;
  }

  assert(caught, 'Expected promise to reject.');
  assert.strictEqual(caught.statusCode, statusCode);

  if (messagePart) {
    assert(
      caught.message.includes(messagePart),
      `Expected "${caught.message}" to include "${messagePart}".`
    );
  }
}

async function verifyAssignmentSafety() {
  const originals = { ...assignmentModel };

  try {
    assignmentModel.studentExists = async () => true;
    assignmentModel.supervisorExists = async () => true;
    assignmentModel.examExists = async () => true;
    assignmentModel.classroomExists = async () => true;
    assignmentModel.findStudentSeatConflict = async () => null;
    assignmentModel.findStudentExamConflict = async () => null;
    assignmentModel.getClassroomCapacity = async () => 2;
    assignmentModel.countStudentAssignmentsForClassroomExam = async () => 1;
    assignmentModel.findSupervisorTimeConflict = async () => null;

    assignmentModel.createStudentAssignment = async (payload) => ({
      id: 1,
      ...payload,
    });

    assignmentModel.createSupervisorAssignment = async (payload) => ({
      id: 2,
      ...payload,
    });

    const createdStudent = await assignmentService.createStudentAssignment({
      student_id: 1,
      exam_id: 1,
      classroom_id: 1,
      seat_number: 'A1',
    });

    assert.strictEqual(createdStudent.seat_number, 'A1');

    assignmentModel.findStudentSeatConflict = async () => ({ id: 99 });

    await expectReject(
      assignmentService.createStudentAssignment({
        student_id: 1,
        exam_id: 1,
        classroom_id: 1,
        seat_number: 'A1',
      }),
      409,
      'Seat is already assigned'
    );

    assignmentModel.findStudentSeatConflict = async () => null;
    assignmentModel.findStudentExamConflict = async () => ({ id: 98 });

    await expectReject(
      assignmentService.createStudentAssignment({
        student_id: 1,
        exam_id: 1,
        classroom_id: 1,
        seat_number: 'A2',
      }),
      409,
      'already has an assignment'
    );

    assignmentModel.findStudentExamConflict = async () => null;
    assignmentModel.countStudentAssignmentsForClassroomExam = async () => 2;

    await expectReject(
      assignmentService.createStudentAssignment({
        student_id: 1,
        exam_id: 1,
        classroom_id: 1,
        seat_number: 'A2',
      }),
      409,
      'capacity'
    );

    assignmentModel.countStudentAssignmentsForClassroomExam = async () => 1;

    const createdSupervisor = await assignmentService.createSupervisorAssignment({
      supervisor_id: 1,
      exam_id: 1,
      classroom_id: 1,
      role: 'Invigilator',
    });

    assert.strictEqual(createdSupervisor.role, 'Invigilator');

    assignmentModel.findSupervisorTimeConflict = async () => ({ id: 77 });

    await expectReject(
      assignmentService.createSupervisorAssignment({
        supervisor_id: 1,
        exam_id: 2,
        classroom_id: 2,
        role: 'Invigilator',
      }),
      409,
      'during this exam time'
    );

    assignmentModel.findSupervisorTimeConflict = async () => null;
    assignmentModel.createStudentAssignment = async () => {
      const err = new Error('duplicate');
      err.code = 'ER_DUP_ENTRY';
      throw err;
    };

    await expectReject(
      assignmentService.createStudentAssignment({
        student_id: 1,
        exam_id: 1,
        classroom_id: 1,
        seat_number: 'A3',
      }),
      409,
      'conflicts'
    );
  } finally {
    Object.assign(assignmentModel, originals);
  }
}

async function verifySearch() {
  const originals = { ...searchModel };

  try {
    searchModel.searchStudents = async () => [
      {
        person_id: 1,
        name: 'Alex',
        student_id: 'STU001',
        roll_number: 'R001',
        department: 'CSE',
        year: 2,
        division: 'A',
        assignment_id: 10,
        seat_number: 'A1',
        classroom_id: 5,
        room_number: '101',
        building: 'Main',
        floor: 1,
        exam_id: 3,
        exam_name: 'Midterm',
        subject: 'DBMS',
        exam_date: '2026-08-20',
        start_time: '10:00:00',
        end_time: '12:00:00',
      },
      {
        person_id: 2,
        name: 'Alex',
        student_id: 'STU002',
        roll_number: 'R002',
        department: 'IT',
        year: 2,
        division: 'B',
        assignment_id: 11,
        seat_number: 'A2',
        classroom_id: 5,
        room_number: '101',
        building: 'Main',
        floor: 1,
        exam_id: 3,
        exam_name: 'Midterm',
        subject: 'DBMS',
        exam_date: '2026-08-20',
        start_time: '10:00:00',
        end_time: '12:00:00',
      },
    ];

    searchModel.searchSupervisors = async () => [
      {
        person_id: 9,
        name: 'Alex',
        employee_id: 'EMP009',
        department: 'CSE',
        assignment_id: 20,
        assignment_role: 'Chief Invigilator',
        classroom_id: 5,
        room_number: '101',
        building: 'Main',
        floor: 1,
        exam_id: 3,
        exam_name: 'Midterm',
        subject: 'DBMS',
        exam_date: '2026-08-20',
        start_time: '10:00:00',
        end_time: '12:00:00',
      },
    ];

    const results = await searchService.search(' Alex ');

    assert.strictEqual(results.length, 3);
    assert.strictEqual(results[0].role, 'student');
    assert.strictEqual(results[0].student_id, 'STU001');
    assert.strictEqual(results[1].student_id, 'STU002');
    assert.strictEqual(results[2].role, 'supervisor');
    assert.strictEqual(results[2].employee_id, 'EMP009');
    assert.strictEqual(results[0].assignment.seat_number, 'A1');
    assert.strictEqual(results[2].assignment.role, 'Chief Invigilator');

    await expectReject(searchService.search('   '), 400, 'required');

    const route = searchRoutes.stack.find(
      (layer) => layer.route && layer.route.path === '/' && layer.route.methods.get
    );

    assert(route, 'Missing GET / search route.');
  } finally {
    Object.assign(searchModel, originals);
  }
}

function verifyMigrationsAndServer() {
  const root = path.resolve(__dirname, '..');

  const studentMigration = fs.readFileSync(
    path.join(root, 'database/migrations/006_create_student_assignments.sql'),
    'utf8'
  );

  const supervisorMigration = fs.readFileSync(
    path.join(root, 'database/migrations/007_create_supervisor_assignments.sql'),
    'utf8'
  );

  assert(
    studentMigration.includes('UNIQUE (exam_id, classroom_id, seat_number)'),
    'Student seat unique constraint is missing.'
  );

  assert(
    studentMigration.includes('UNIQUE (student_id, exam_id)'),
    'Student/exam unique constraint is missing.'
  );

  assert(
    supervisorMigration.includes('UNIQUE (supervisor_id, exam_id)'),
    'Supervisor/exam unique constraint is missing.'
  );

  const serverText = fs.readFileSync(path.join(root, 'server.js'), 'utf8');

  assert(
    serverText.includes("app.use('/api/search', searchRoutes)"),
    'Server does not mount /api/search.'
  );
}

async function main() {
  await verifyAssignmentSafety();
  await verifySearch();
  verifyMigrationsAndServer();
  require('../server');
  console.log('Player 3 Batch C verification PASSED');
}

main().catch((err) => {
  console.error('Player 3 Batch C verification FAILED');
  console.error(err);
  process.exit(1);
});
