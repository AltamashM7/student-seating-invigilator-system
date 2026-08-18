const service = require('./assignments.service');
const { success, error } = require('../../utils/response');

function handleServiceError(res, err) {
  if (err.statusCode) {
    return error(res, err.message, err.statusCode);
  }

  console.error(err);
  return error(res, 'Internal server error.', 500);
}

async function getStudentAssignments(req, res) {
  try {
    const assignments = await service.listStudentAssignments();
    return success(res, assignments);
  } catch (err) {
    return handleServiceError(res, err);
  }
}

async function createStudentAssignment(req, res) {
  try {
    const assignment = await service.createStudentAssignment(
      req.body || {}
    );

    return success(res, assignment, 201);
  } catch (err) {
    return handleServiceError(res, err);
  }
}

module.exports = {
  getStudentAssignments,
  createStudentAssignment,
};
