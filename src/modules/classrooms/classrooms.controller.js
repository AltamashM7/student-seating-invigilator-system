const service = require('./classrooms.service');
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
    const classrooms = await service.listClassrooms();
    return success(res, classrooms);
  } catch (err) {
    return handleServiceError(res, err);
  }
}

async function getById(req, res) {
  try {
    const classroom = await service.getClassroom(req.params.id);
    return success(res, classroom);
  } catch (err) {
    return handleServiceError(res, err);
  }
}

async function create(req, res) {
  try {
    const classroom = await service.createClassroom(req.body || {});
    return success(res, classroom, 201);
  } catch (err) {
    return handleServiceError(res, err);
  }
}

async function update(req, res) {
  try {
    const classroom = await service.updateClassroom(req.params.id, req.body || {});
    return success(res, classroom);
  } catch (err) {
    return handleServiceError(res, err);
  }
}

async function remove(req, res) {
  try {
    await service.deleteClassroom(req.params.id);
    return success(res, { id: req.params.id, deleted: true });
  } catch (err) {
    return handleServiceError(res, err);
  }
}

module.exports = { getAll, getById, create, update, remove };
