const examsModel = require('./exams.model');

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

const TIME_RE = /^([01]\d|2[0-3]):([0-5]\d)(:([0-5]\d))?$/;
const DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

function toMinutes(time) {
  const [h, m] = time.split(':').map(Number);
  return h * 60 + m;
}

function validatePayload(payload, { partial = false } = {}) {
  const errors = [];
  const { exam_name, subject, exam_date, start_time, end_time } = payload;

  if (!partial || exam_name !== undefined) {
    if (!exam_name || typeof exam_name !== 'string' || !exam_name.trim()) {
      errors.push('exam_name is required.');
    }
  }

  if (!partial || exam_date !== undefined) {
    if (!exam_date || !DATE_RE.test(exam_date) || Number.isNaN(Date.parse(exam_date))) {
      errors.push('exam_date is required and must be a valid date (YYYY-MM-DD).');
    }
  }

  if (!partial || start_time !== undefined) {
    if (!start_time || !TIME_RE.test(start_time)) {
      errors.push('start_time is required and must be a valid time (HH:MM).');
    }
  }

  if (!partial || end_time !== undefined) {
    if (!end_time || !TIME_RE.test(end_time)) {
      errors.push('end_time is required and must be a valid time (HH:MM).');
    }
  }

  if (
    start_time &&
    end_time &&
    TIME_RE.test(start_time) &&
    TIME_RE.test(end_time) &&
    toMinutes(end_time) <= toMinutes(start_time)
  ) {
    errors.push('end_time must be after start_time.');
  }

  if (subject !== undefined && subject !== null && typeof subject !== 'string') {
    errors.push('subject must be a string.');
  }

  if (errors.length) {
    throw new ValidationError(errors.join(' '));
  }
}

async function listExams() {
  return examsModel.findAll();
}

async function getExam(id) {
  const exam = await examsModel.findById(id);
  if (!exam) throw new NotFoundError('Exam not found.');
  return exam;
}

async function createExam(payload) {
  validatePayload(payload);
  return examsModel.create({
    exam_name: payload.exam_name.trim(),
    subject: payload.subject ?? null,
    exam_date: payload.exam_date,
    start_time: payload.start_time,
    end_time: payload.end_time,
  });
}

async function updateExam(id, payload) {
  const existing = await examsModel.findById(id);
  if (!existing) throw new NotFoundError('Exam not found.');

  validatePayload(payload, { partial: true });

  const merged = {
    exam_name: payload.exam_name !== undefined ? payload.exam_name.trim() : existing.exam_name,
    subject: payload.subject !== undefined ? payload.subject : existing.subject,
    exam_date: payload.exam_date !== undefined ? payload.exam_date : existing.exam_date,
    start_time: payload.start_time !== undefined ? payload.start_time : existing.start_time,
    end_time: payload.end_time !== undefined ? payload.end_time : existing.end_time,
  };

  // Re-validate the merged record so a partial update can't produce an
  // invalid end_time/start_time combination.
  validatePayload(merged);

  return examsModel.update(id, merged);
}

async function deleteExam(id) {
  const existing = await examsModel.findById(id);
  if (!existing) throw new NotFoundError('Exam not found.');

  const referenced = await examsModel.isReferencedByAssignment(id);
  if (referenced) {
    throw new ConflictError('Exam is referenced by an existing assignment and cannot be deleted.');
  }

  await examsModel.remove(id);
}

module.exports = {
  listExams,
  getExam,
  createExam,
  updateExam,
  deleteExam,
  ValidationError,
  NotFoundError,
  ConflictError,
};
