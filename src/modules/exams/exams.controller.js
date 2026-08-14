const service = require('./exams.service');
const { success, error } = require('../../utils/response');

function handleServiceError(res, err) {
  if (err.statusCode) {
    return error(res, err.message, err.statusCode);
  }
  console.error(err);
  return error(res, 'Internal server error.', 500);
}

async function getAll(req, res) {
  try {
    const exams = await service.listExams();
    return success(res, exams);
  } catch (err) {
    return handleServiceError(res, err);
  }
}

async function getById(req, res) {
  try {
    const exam = await service.getExam(req.params.id);
    return success(res, exam);
  } catch (err) {
    return handleServiceError(res, err);
  }
}

async function create(req, res) {
  try {
    const exam = await service.createExam(req.body || {});
    return success(res, exam, 201);
  } catch (err) {
    return handleServiceError(res, err);
  }
}

async function update(req, res) {
  try {
    const exam = await service.updateExam(req.params.id, req.body || {});
    return success(res, exam);
  } catch (err) {
    return handleServiceError(res, err);
  }
}

async function remove(req, res) {
  try {
    await service.deleteExam(req.params.id);
    return success(res, { id: req.params.id, deleted: true });
  } catch (err) {
    return handleServiceError(res, err);
  }
}

module.exports = { getAll, getById, create, update, remove };
