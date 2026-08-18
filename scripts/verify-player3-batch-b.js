const assert = require('assert');

const model = require('../src/modules/assignments/assignments.model');
const service = require('../src/modules/assignments/assignments.service');
const routes = require('../src/modules/assignments/assignments.routes');

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

async function main() {
  const originals = { ...model };

  try {
    // Reference checks introduced in Batch B.
    model.studentExists = async () => true;
    model.supervisorExists = async () => true;
    model.examExists = async () => true;
    model.classroomExists = async () => true;

    // Safety checks introduced later in Batch C.
    // Batch B is an isolated CRUD verifier, so these must be mocked too;
    // otherwise the newer service implementation falls through to real MySQL.
    model.findStudentSeatConflict = async () => null;
    model.findStudentExamConflict = async () => null;
    model.getClassroomCapacity = async () => 100;
    model.countStudentAssignmentsForClassroomExam = async () => 0;
    model.findSupervisorTimeConflict = async () => null;

    model.findAllStudentAssignments = async () => [
      {
        id: 1,
        student_id: 1,
        exam_id: 1,
        classroom_id: 1,
        seat_number: 'A1',
      },
    ];

    model.findStudentAssignmentById = async (id) => ({
      id: Number(id),
      student_id: 1,
      exam_id: 1,
      classroom_id: 1,
      seat_number: 'A1',
    });

    model.createStudentAssignment = async (payload) => ({
      id: 10,
      ...payload,
    });

    model.updateStudentAssignment = async (id, payload) => ({
      id: Number(id),
      ...payload,
    });

    model.deleteStudentAssignment = async () => true;

    model.findAllSupervisorAssignments = async () => [
      {
        id: 2,
        supervisor_id: 1,
        exam_id: 1,
        classroom_id: 1,
        role: 'Invigilator',
      },
    ];

    model.findSupervisorAssignmentById = async (id) => ({
      id: Number(id),
      supervisor_id: 1,
      exam_id: 1,
      classroom_id: 1,
      role: 'Invigilator',
    });

    model.createSupervisorAssignment = async (payload) => ({
      id: 20,
      ...payload,
    });

    model.updateSupervisorAssignment = async (id, payload) => ({
      id: Number(id),
      ...payload,
    });

    model.deleteSupervisorAssignment = async () => true;

    assert.strictEqual(
      (await service.listStudentAssignments()).length,
      1
    );

    assert.strictEqual(
      (await service.getStudentAssignment(1)).id,
      1
    );

    const createdStudent = await service.createStudentAssignment({
      student_id: '1',
      exam_id: 1,
      classroom_id: 1,
      seat_number: '  A2  ',
    });

    assert.strictEqual(createdStudent.student_id, 1);
    assert.strictEqual(createdStudent.seat_number, 'A2');

    const updatedStudent = await service.updateStudentAssignment(
      1,
      { seat_number: 'A3' }
    );

    assert.strictEqual(updatedStudent.seat_number, 'A3');

    await service.deleteStudentAssignment(1);

    await expectReject(
      service.createStudentAssignment({
        student_id: 0,
        exam_id: 1,
        classroom_id: 1,
        seat_number: 'A1',
      }),
      400,
      'student_id'
    );

    model.studentExists = async () => false;

    await expectReject(
      service.createStudentAssignment({
        student_id: 999,
        exam_id: 1,
        classroom_id: 1,
        seat_number: 'A1',
      }),
      404,
      'Student not found'
    );

    model.studentExists = async () => true;

    assert.strictEqual(
      (await service.listSupervisorAssignments()).length,
      1
    );

    assert.strictEqual(
      (await service.getSupervisorAssignment(2)).id,
      2
    );

    const createdSupervisor =
      await service.createSupervisorAssignment({
        supervisor_id: 1,
        exam_id: 1,
        classroom_id: 1,
        role: '  Chief Invigilator  ',
      });

    assert.strictEqual(
      createdSupervisor.role,
      'Chief Invigilator'
    );

    const updatedSupervisor =
      await service.updateSupervisorAssignment(2, {
        role: 'Assistant Invigilator',
      });

    assert.strictEqual(
      updatedSupervisor.role,
      'Assistant Invigilator'
    );

    await service.deleteSupervisorAssignment(2);

    await expectReject(
      service.createSupervisorAssignment({
        supervisor_id: 1,
        exam_id: 1,
        classroom_id: 1,
        role: '   ',
      }),
      400,
      'role'
    );

    model.supervisorExists = async () => false;

    await expectReject(
      service.createSupervisorAssignment({
        supervisor_id: 999,
        exam_id: 1,
        classroom_id: 1,
        role: 'Invigilator',
      }),
      404,
      'Supervisor not found'
    );

    const routeSummary = routes.stack
      .filter((layer) => layer.route)
      .map((layer) => ({
        path: layer.route.path,
        methods: Object.keys(layer.route.methods),
        middlewareCount: layer.route.stack.length,
      }));

    const expectedRoutes = [
      ['GET', '/students'],
      ['GET', '/students/:id'],
      ['POST', '/students'],
      ['PUT', '/students/:id'],
      ['DELETE', '/students/:id'],
      ['GET', '/supervisors'],
      ['GET', '/supervisors/:id'],
      ['POST', '/supervisors'],
      ['PUT', '/supervisors/:id'],
      ['DELETE', '/supervisors/:id'],
    ];

    for (const [method, routePath] of expectedRoutes) {
      const route = routeSummary.find(
        (item) =>
          item.path === routePath &&
          item.methods.includes(method.toLowerCase())
      );

      assert(route, `Missing route: ${method} ${routePath}`);

      if (['POST', 'PUT', 'DELETE'].includes(method)) {
        assert(
          route.middlewareCount >= 2,
          `Expected admin middleware on ${method} ${routePath}`
        );
      }
    }

    console.log('Player 3 Batch B verification PASSED');
  } finally {
    Object.assign(model, originals);
  }
}

main().catch((err) => {
  console.error('Player 3 Batch B verification FAILED');
  console.error(err);
  process.exit(1);
});
