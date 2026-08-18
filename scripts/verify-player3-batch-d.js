const assert = require('assert');

const {
  generateSeatingPlan,
} = require('../src/modules/assignments/seating-generator.service');

const {
  generateInvigilatorPlan,
} = require('../src/modules/assignments/invigilator-generator.service');

function expectThrow(fn, statusCode, messagePart) {
  let caught = null;

  try {
    fn();
  } catch (err) {
    caught = err;
  }

  assert(caught, 'Expected function to throw.');
  assert.strictEqual(caught.statusCode, statusCode);

  if (messagePart) {
    assert(
      caught.message.includes(messagePart),
      `Expected "${caught.message}" to include "${messagePart}".`
    );
  }
}

function verifySeatingGenerator() {
  const plan = generateSeatingPlan({
    students: [{ id: 3 }, { id: 1 }, { id: 2 }],
    exam_id: 7,
    classrooms: [
      { id: 10, capacity: 2 },
      { id: 11, capacity: 2 },
    ],
  });

  assert.strictEqual(plan.assignments.length, 3);
  assert.deepStrictEqual(plan.assignments, [
    {
      student_id: 3,
      exam_id: 7,
      classroom_id: 10,
      seat_number: '1',
    },
    {
      student_id: 1,
      exam_id: 7,
      classroom_id: 10,
      seat_number: '2',
    },
    {
      student_id: 2,
      exam_id: 7,
      classroom_id: 11,
      seat_number: '1',
    },
  ]);

  const prefixed = generateSeatingPlan({
    students: [1],
    exam_id: 7,
    classrooms: [{ id: 10, capacity: 1 }],
    seat_prefix: 'S-',
  });

  assert.strictEqual(
    prefixed.assignments[0].seat_number,
    'S-1'
  );

  expectThrow(
    () =>
      generateSeatingPlan({
        students: [1, 2, 3],
        exam_id: 7,
        classrooms: [{ id: 10, capacity: 2 }],
      }),
    409,
    'Insufficient classroom capacity'
  );

  expectThrow(
    () =>
      generateSeatingPlan({
        students: [1, 1],
        exam_id: 7,
        classrooms: [{ id: 10, capacity: 2 }],
      }),
    400,
    'duplicate IDs'
  );
}

function verifyInvigilatorGenerator() {
  const plan = generateInvigilatorPlan({
    supervisors: [8, 9, 10, 11],
    exam_id: 7,
    classrooms: [20, 21],
    supervisors_per_room: 2,
    role: 'Exam Invigilator',
  });

  assert.deepStrictEqual(plan.assignments, [
    {
      supervisor_id: 8,
      exam_id: 7,
      classroom_id: 20,
      role: 'Exam Invigilator',
    },
    {
      supervisor_id: 9,
      exam_id: 7,
      classroom_id: 20,
      role: 'Exam Invigilator',
    },
    {
      supervisor_id: 10,
      exam_id: 7,
      classroom_id: 21,
      role: 'Exam Invigilator',
    },
    {
      supervisor_id: 11,
      exam_id: 7,
      classroom_id: 21,
      role: 'Exam Invigilator',
    },
  ]);

  const defaultRole = generateInvigilatorPlan({
    supervisors: [8],
    exam_id: 7,
    classrooms: [20],
  });

  assert.strictEqual(
    defaultRole.assignments[0].role,
    'Invigilator'
  );

  expectThrow(
    () =>
      generateInvigilatorPlan({
        supervisors: [8],
        exam_id: 7,
        classrooms: [20, 21],
      }),
    409,
    'Insufficient supervisors'
  );

  expectThrow(
    () =>
      generateInvigilatorPlan({
        supervisors: [8, 8],
        exam_id: 7,
        classrooms: [20],
      }),
    400,
    'duplicate IDs'
  );
}

function main() {
  verifySeatingGenerator();
  verifyInvigilatorGenerator();

  console.log('Player 3 Batch D verification PASSED');
}

try {
  main();
} catch (err) {
  console.error('Player 3 Batch D verification FAILED');
  console.error(err);
  process.exit(1);
}
