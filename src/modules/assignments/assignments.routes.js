const express = require('express');
const controller = require('./assignments.controller');

const router = express.Router();

router.get('/students', controller.getStudentAssignments);
router.post('/students', controller.createStudentAssignment);

module.exports = router;
