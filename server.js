require('dotenv').config();
const express = require('express');
const session = require('express-session');

const classroomsRoutes = require('./src/modules/classrooms/classrooms.routes');
const examsRoutes = require('./src/modules/exams/exams.routes');

const app = express();

app.use(express.json());
app.use(
  session({
    secret: process.env.SESSION_SECRET || 'dev-secret',
    resave: false,
    saveUninitialized: false,
  })
);

// Player 2 owned modules
app.use('/api/admin/classrooms', classroomsRoutes);
app.use('/api/admin/exams', examsRoutes);

app.get('/health', (req, res) => res.json({ success: true, data: { status: 'ok' } }));

app.use((req, res) => {
  res.status(404).json({ success: false, error: { message: 'Not found' } });
});

const PORT = process.env.PORT || 3000;

if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`Server listening on port ${PORT}`);
  });
}

module.exports = app;
