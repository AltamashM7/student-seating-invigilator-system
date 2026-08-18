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

async function getStudentAssignment(req, res) {
  try {
    const assignment = await service.getStudentAssignment(req.params.id);
    return success(res, assignment);
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

async function updateStudentAssignment(req, res) {
  try {
    const assignment = await service.updateStudentAssignment(
      req.params.id,
      req.body || {}
    );

    return success(res, assignment);
  } catch (err) {
    return handleServiceError(res, err);
  }
}

async function deleteStudentAssignment(req, res) {
  try {
    await service.deleteStudentAssignment(req.params.id);

    return success(res, {
      id: req.params.id,
      deleted: true,
    });
  } catch (err) {
    return handleServiceError(res, err);
  }
}

async function getSupervisorAssignments(req, res) {
  try {
    const assignments = await service.listSupervisorAssignments();
    return success(res, assignments);
  } catch (err) {
    return handleServiceError(res, err);
  }
}

async function getSupervisorAssignment(req, res) {
  try {
    const assignment = await service.getSupervisorAssignment(
      req.params.id
    );

    return success(res, assignment);
  } catch (err) {
    return handleServiceError(res, err);
  }
}

async function createSupervisorAssignment(req, res) {
  try {
    const assignment = await service.createSupervisorAssignment(
      req.body || {}
    );

    return success(res, assignment, 201);
  } catch (err) {
    return handleServiceError(res, err);
  }
}

async function updateSupervisorAssignment(req, res) {
  try {
    const assignment = await service.updateSupervisorAssignment(
      req.params.id,
      req.body || {}
    );

    return success(res, assignment);
  } catch (err) {
    return handleServiceError(res, err);
  }
}

async function deleteSupervisorAssignment(req, res) {
  try {
    await service.deleteSupervisorAssignment(req.params.id);

    return success(res, {
      id: req.params.id,
      deleted: true,
    });
  } catch (err) {
    return handleServiceError(res, err);
  }
}

module.exports = {
  getStudentAssignments,
  getStudentAssignment,
  createStudentAssignment,
  updateStudentAssignment,
  deleteStudentAssignment,
  getSupervisorAssignments,
  getSupervisorAssignment,
  createSupervisorAssignment,
  updateSupervisorAssignment,
  deleteSupervisorAssignment,
};
