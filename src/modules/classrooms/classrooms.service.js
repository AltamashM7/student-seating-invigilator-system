const classroomsModel = require('./classrooms.model');

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

function validatePayload(payload, { partial = false } = {}) {
  const errors = [];
  const { room_number, building, floor, capacity } = payload;

  if (!partial || room_number !== undefined) {
    if (!room_number || typeof room_number !== 'string' || !room_number.trim()) {
      errors.push('room_number is required.');
    }
  }

  if (!partial || capacity !== undefined) {
    if (capacity === undefined || capacity === null || Number.isNaN(Number(capacity))) {
      errors.push('capacity is required and must be a number.');
    } else if (Number(capacity) <= 0) {
      errors.push('capacity must be greater than zero.');
    }
  }

  if (floor !== undefined && floor !== null && floor !== '') {
    if (Number.isNaN(Number(floor)) || !Number.isInteger(Number(floor))) {
      errors.push('floor must be a valid integer.');
    }
  }

  if (building !== undefined && building !== null && typeof building !== 'string') {
    errors.push('building must be a string.');
  }

  if (errors.length) {
    throw new ValidationError(errors.join(' '));
  }
}

async function listClassrooms() {
  return classroomsModel.findAll();
}

async function getClassroom(id) {
  const classroom = await classroomsModel.findById(id);
  if (!classroom) throw new NotFoundError('Classroom not found.');
  return classroom;
}

async function createClassroom(payload) {
  validatePayload(payload);
  const existing = await classroomsModel.findByRoomNumber(payload.room_number.trim());
  if (existing) throw new ConflictError('room_number must be unique.');
  return classroomsModel.create({
    room_number: payload.room_number.trim(),
    building: payload.building ?? null,
    floor: payload.floor ?? null,
    capacity: Number(payload.capacity),
  });
}

async function updateClassroom(id, payload) {
  const existing = await classroomsModel.findById(id);
  if (!existing) throw new NotFoundError('Classroom not found.');

  validatePayload(payload, { partial: true });

  const merged = {
    room_number: payload.room_number !== undefined ? payload.room_number.trim() : existing.room_number,
    building: payload.building !== undefined ? payload.building : existing.building,
    floor: payload.floor !== undefined ? payload.floor : existing.floor,
    capacity: payload.capacity !== undefined ? Number(payload.capacity) : existing.capacity,
  };

  const duplicate = await classroomsModel.findByRoomNumber(merged.room_number, id);
  if (duplicate) throw new ConflictError('room_number must be unique.');

  return classroomsModel.update(id, merged);
}

async function deleteClassroom(id) {
  const existing = await classroomsModel.findById(id);
  if (!existing) throw new NotFoundError('Classroom not found.');

  const referenced = await classroomsModel.isReferencedByAssignment(id);
  if (referenced) {
    throw new ConflictError(
      'Classroom is referenced by an existing assignment and cannot be deleted.'
    );
  }

  await classroomsModel.remove(id);
}

module.exports = {
  listClassrooms,
  getClassroom,
  createClassroom,
  updateClassroom,
  deleteClassroom,
  ValidationError,
  NotFoundError,
  ConflictError,
};
