package com.examplanner.dao;

import com.examplanner.config.DbConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StudentImportDao {
    public ImportResult importRows(List<String[]> rows) throws SQLException {
        ImportResult result = new ImportResult();
        try (Connection connection = DbConnection.getConnection()) {
            for (int i = 0; i < rows.size(); i++) {
                String[] row = rows.get(i);
                int line = i + 2;
                if (row.length != 6) {
                    result.errors.add("Line " + line + ": expected 6 columns");
                    continue;
                }

                String roll = row[0].trim();
                String name = row[1].trim();
                String email = row[2].trim();
                String departmentCode = row[3].trim();
                String semesterText = row[4].trim();
                String courseCode = row[5].trim();

                if (roll.isEmpty() || name.isEmpty() || departmentCode.isEmpty() || semesterText.isEmpty() || courseCode.isEmpty()) {
                    result.errors.add("Line " + line + ": required value missing");
                    continue;
                }

                int semester;
                try {
                    semester = Integer.parseInt(semesterText);
                    if (semester < 1 || semester > 8) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    result.errors.add("Line " + line + ": invalid semester");
                    continue;
                }

                if (!email.isEmpty() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                    result.errors.add("Line " + line + ": invalid email");
                    continue;
                }

                if (studentExists(connection, roll)) {
                    result.duplicates++;
                    continue;
                }

                Integer departmentId = findDepartmentId(connection, departmentCode);
                Integer courseId = findCourseId(connection, courseCode);
                if (departmentId == null) {
                    result.errors.add("Line " + line + ": unknown department " + departmentCode);
                    continue;
                }
                if (courseId == null) {
                    result.errors.add("Line " + line + ": unknown course " + courseCode);
                    continue;
                }

                connection.setAutoCommit(false);
                try {
                    int studentId;
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO students(roll_number, name, email, department_id, semester) VALUES (?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        statement.setString(1, roll);
                        statement.setString(2, name);
                        if (email.isEmpty()) statement.setNull(3, java.sql.Types.VARCHAR); else statement.setString(3, email);
                        statement.setInt(4, departmentId);
                        statement.setInt(5, semester);
                        statement.executeUpdate();
                        try (ResultSet keys = statement.getGeneratedKeys()) {
                            keys.next();
                            studentId = keys.getInt(1);
                        }
                    }
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO course_enrollments(student_id, course_id) VALUES (?, ?)")) {
                        statement.setInt(1, studentId);
                        statement.setInt(2, courseId);
                        statement.executeUpdate();
                    }
                    connection.commit();
                    result.imported++;
                } catch (SQLException e) {
                    connection.rollback();
                    result.errors.add("Line " + line + ": " + readableMessage(e));
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        }
        return result;
    }

    private boolean studentExists(Connection connection, String roll) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM students WHERE roll_number = ?")) {
            statement.setString(1, roll);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Integer findDepartmentId(Connection connection, String code) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM departments WHERE code = ?")) {
            statement.setString(1, code);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt("id") : null;
            }
        }
    }

    private Integer findCourseId(Connection connection, String code) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM courses WHERE course_code = ?")) {
            statement.setString(1, code);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt("id") : null;
            }
        }
    }

    private String readableMessage(SQLException e) {
        if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
            return "duplicate or invalid linked value";
        }
        return "database error";
    }

    public static class ImportResult {
        public int imported;
        public int duplicates;
        public final List<String> errors = new ArrayList<>();
    }
}
