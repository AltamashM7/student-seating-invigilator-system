const assert = require('assert');
const fs = require('fs');
const path = require('path');

const assignmentService = require('../src/modules/assignments/assignments.service');
const assignmentRoutes = require('../src/modules/assignments/assignments.routes');
const searchModel = require('../src/modules/search/search.model');
const searchService = require('../src/modules/search/search.service');
const searchRoutes = require('../src/modules/search/search.routes');

const root = path.resolve(__dirname, '..');

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

function routeSummary(router) {
  return router.stack
    .filter((layer) => layer.route)
    .map((layer) => ({
      path: layer.route.path,
      methods: Object.keys(layer.route.methods),
      middlewareCount: layer.route.stack.length,
    }));
}

function verifyAssignmentRouteHardening() {
  const routes = routeSummary(assignmentRoutes);

  const expectedRoutes = [
    ['GET', '/students', false],
    ['GET', '/students/:id', false],
    ['POST', '/students', true],
    ['PUT', '/students/:id', true],
    ['DELETE', '/students/:id', true],
    ['GET', '/supervisors', false],
    ['GET', '/supervisors/:id', false],
    ['POST', '/supervisors', true],
    ['PUT', '/supervisors/:id', true],
    ['DELETE', '/supervisors/:id', true],
  ];

  for (const [method, routePath, requiresAdmin] of expectedRoutes) {
    const route = routes.find(
      (item) =>
        item.path === routePath &&
        item.methods.includes(method.toLowerCase())
    );

    assert(route, `Missing assignment route: ${method} ${routePath}`);

    if (requiresAdmin) {
      assert(
        route.middlewareCount >= 2,
        `Admin middleware missing: ${method} ${routePath}`
      );
    }
  }
}

function verifyPublicSearchIsReadOnly() {
  const routes = routeSummary(searchRoutes);

  assert.strictEqual(
    routes.length,
    1,
    'Public search should expose exactly one route.'
  );

  assert.strictEqual(routes[0].path, '/');
  assert.deepStrictEqual(
    routes[0].methods,
    ['get'],
    'Public search must expose GET only.'
  );
}

async function verifySearchHardening() {
  const originals = {
    searchStudents: searchModel.searchStudents,
    searchSupervisors: searchModel.searchSupervisors,
  };

  try {
    searchModel.searchStudents = async () => [];
    searchModel.searchSupervisors = async () => [];

    const noResults = await searchService.search('does-not-exist');

    assert.deepStrictEqual(
      noResults,
      [],
      'No-result searches must return an empty array.'
    );

    searchModel.searchStudents = async () => [
      {
        person_id: 1,
        name: 'Same Name',
        student_id: 'STU001',
        roll_number: 'ROLL001',
        department: 'CSE',
        year: 2,
        division: 'A',
        assignment_id: 101,
        seat_number: 'A1',
        classroom_id: 5,
        room_number: '101',
        building: 'Main',
        floor: 1,
        exam_id: 7,
        exam_name: 'Midterm',
        subject: 'DBMS',
        exam_date: '2026-08-20',
        start_time: '10:00:00',
        end_time: '12:00:00',
      },
      {
        person_id: 2,
        name: 'Same Name',
        student_id: 'STU002',
        roll_number: 'ROLL002',
        department: 'IT',
        year: 2,
        division: 'B',
        assignment_id: 102,
        seat_number: 'A2',
        classroom_id: 5,
        room_number: '101',
        building: 'Main',
        floor: 1,
        exam_id: 7,
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
        name: 'Same Name',
        employee_id: 'EMP009',
        department: 'CSE',
        assignment_id: 201,
        assignment_role: 'Chief Invigilator',
        classroom_id: 6,
        room_number: '102',
        building: 'Main',
        floor: 1,
        exam_id: 7,
        exam_name: 'Midterm',
        subject: 'DBMS',
        exam_date: '2026-08-20',
        start_time: '10:00:00',
        end_time: '12:00:00',
      },
    ];

    const duplicates = await searchService.search('Same Name');

    assert.strictEqual(
      duplicates.length,
      3,
      'Duplicate-name search must preserve every matching person.'
    );

    assert.strictEqual(duplicates[0].student_id, 'STU001');
    assert.strictEqual(duplicates[1].student_id, 'STU002');
    assert.strictEqual(duplicates[2].employee_id, 'EMP009');

    await expectReject(
      searchService.search(''),
      400,
      'required'
    );

    await expectReject(
      searchService.search('x'.repeat(101)),
      400,
      '100 characters'
    );
  } finally {
    searchModel.searchStudents = originals.searchStudents;
    searchModel.searchSupervisors = originals.searchSupervisors;
  }
}

async function verifyInvalidAssignmentIds() {
  await expectReject(
    assignmentService.getStudentAssignment('abc'),
    400,
    'positive integer'
  );

  await expectReject(
    assignmentService.getStudentAssignment(0),
    400,
    'positive integer'
  );

  await expectReject(
    assignmentService.getSupervisorAssignment('-1'),
    400,
    'positive integer'
  );
}

function verifyParameterizedSql() {
  const assignmentModel = fs.readFileSync(
    path.join(
      root,
      'src/modules/assignments/assignments.model.js'
    ),
    'utf8'
  );

  const searchModelText = fs.readFileSync(
    path.join(root, 'src/modules/search/search.model.js'),
    'utf8'
  );

  const requiredAssignmentFragments = [
    'WHERE id = ?',
    'VALUES (?, ?, ?, ?)',
    'WHERE student_id = ?',
    'WHERE sa.supervisor_id = ?',
    'WHERE exam_id = ?',
    'WHERE id = ? LIMIT 1',
  ];

  for (const fragment of requiredAssignmentFragments) {
    assert(
      assignmentModel.includes(fragment),
      `Expected parameterized assignment SQL fragment: ${fragment}`
    );
  }

  const requiredSearchFragments = [
    's.name LIKE ?',
    's.student_id = ?',
    's.roll_number = ?',
    's.employee_id = ?',
    '[likeQuery, query, query]',
    '[likeQuery, query]',
  ];

  for (const fragment of requiredSearchFragments) {
    assert(
      searchModelText.includes(fragment),
      `Expected parameterized search SQL fragment: ${fragment}`
    );
  }
}

function verifyIndexesAndConstraints() {
  const studentMigration = fs.readFileSync(
    path.join(
      root,
      'database/migrations/006_create_student_assignments.sql'
    ),
    'utf8'
  );

  const supervisorMigration = fs.readFileSync(
    path.join(
      root,
      'database/migrations/007_create_supervisor_assignments.sql'
    ),
    'utf8'
  );

  const studentRequirements = [
    'UNIQUE (exam_id, classroom_id, seat_number)',
    'UNIQUE (student_id, exam_id)',
    'idx_student_assignments_student',
    'idx_student_assignments_exam',
    'idx_student_assignments_classroom',
    'idx_student_assignments_exam_classroom',
  ];

  for (const fragment of studentRequirements) {
    assert(
      studentMigration.includes(fragment),
      `Student migration hardening missing: ${fragment}`
    );
  }

  const supervisorRequirements = [
    'UNIQUE (supervisor_id, exam_id)',
    'idx_supervisor_assignments_supervisor',
    'idx_supervisor_assignments_exam',
    'idx_supervisor_assignments_classroom',
    'idx_supervisor_assignments_exam_classroom',
  ];

  for (const fragment of supervisorRequirements) {
    assert(
      supervisorMigration.includes(fragment),
      `Supervisor migration hardening missing: ${fragment}`
    );
  }
}

function verifyServerAndContract() {
  const serverText = fs.readFileSync(
    path.join(root, 'server.js'),
    'utf8'
  );

  assert(
    serverText.includes(
      "app.use('/api/admin/assignments', assignmentsRoutes)"
    ),
    'Assignment routes are not registered on the server.'
  );

  assert(
    serverText.includes("app.use('/api/search', searchRoutes)"),
    'Search routes are not registered on the server.'
  );

  const contractPath = path.join(
    root,
    'docs/PLAYER3_API_CONTRACT.md'
  );

  assert(
    fs.existsSync(contractPath),
    'Player 3 API contract document is missing.'
  );

  const contract = fs.readFileSync(contractPath, 'utf8');
  const normalizedContract = contract.replace(/\s+/g, ' ');

  const requiredContractFragments = [
    'GET /api/search?q=',
    'POST /api/admin/assignments/students',
    'POST /api/admin/assignments/supervisors',
    'Final integration still required',
  ];

  for (const fragment of requiredContractFragments) {
    assert(
      normalizedContract.includes(fragment),
      `API contract is missing: ${fragment}`
    );
  }
}

async function main() {
  verifyAssignmentRouteHardening();
  verifyPublicSearchIsReadOnly();
  await verifySearchHardening();
  await verifyInvalidAssignmentIds();
  verifyParameterizedSql();
  verifyIndexesAndConstraints();
  verifyServerAndContract();

  require('../server');

  console.log('Player 3 Batch E hardening verification PASSED');
}

main().catch((err) => {
  console.error('Player 3 Batch E hardening verification FAILED');
  console.error(err);
  process.exit(1);
});
