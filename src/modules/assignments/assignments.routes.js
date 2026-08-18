const express = require('express');
const controller = require('./assignments.controller');
const requireAdmin = require('../../middleware/requireAdmin');

const router = express.Router();

router.get('/students', controller.getStudentAssignments);
router.get('/students/:id', controller.getStudentAssignment);
router.post('/students', requireAdmin, controller.createStudentAssignment);
router.put('/students/:id', requireAdmin, controller.updateStudentAssignment);
router.delete(
  '/students/:id',
  requireAdmin,
  controller.deleteStudentAssignment
);

router.get('/supervisors', controller.getSupervisorAssignments);
router.get('/supervisors/:id', controller.getSupervisorAssignment);
router.post(
  '/supervisors',
  requireAdmin,
  controller.createSupervisorAssignment
);
router.put(
  '/supervisors/:id',
  requireAdmin,
  controller.updateSupervisorAssignment
);
router.delete(
  '/supervisors/:id',
  requireAdmin,
  controller.deleteSupervisorAssignment
);

module.exports = router;
