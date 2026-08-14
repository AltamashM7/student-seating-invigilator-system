// NOTE: Authentication itself (login, session creation, admin table) is owned
// by Player 1. This middleware only guards Player 2's mutation endpoints and
// expects `req.session.admin` to already be populated by the shared auth
// flow. Coordinate with Player 1 before changing the shape of `req.session.admin`.
const { error } = require('../utils/response');

function requireAdmin(req, res, next) {
  if (req.session && req.session.admin) {
    return next();
  }
  return error(res, 'Admin authentication required', 401);
}

module.exports = requireAdmin;
