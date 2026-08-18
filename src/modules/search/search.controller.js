const service = require('./search.service');
const { success, error } = require('../../utils/response');

async function search(req, res) {
  try {
    const results = await service.search(req.query.q);
    return success(res, results);
  } catch (err) {
    if (err.statusCode) {
      return error(res, err.message, err.statusCode);
    }

    console.error(err);
    return error(res, 'Internal server error.', 500);
  }
}

module.exports = { search };
