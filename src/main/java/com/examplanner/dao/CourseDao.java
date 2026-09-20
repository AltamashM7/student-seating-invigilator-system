package com.examplanner.dao;

import com.examplanner.config.DbConnection;
import com.examplanner.model.Course;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CourseDao {
    public List<Course> findAll() throws SQLException {
        List<Course> courses = new ArrayList<>();
        String sql = """
                SELECT c.id, c.course_code, c.course_name, c.department_id, d.code AS department_code, c.semester
                FROM courses c JOIN departments d ON d.id = c.department_id
                ORDER BY c.course_code
                """;
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                courses.add(new Course(
                        rs.getInt("id"), rs.getString("course_code"), rs.getString("course_name"),
                        rs.getInt("department_id"), rs.getString("department_code"), rs.getInt("semester")
                ));
            }
        }
        return courses;
    }

    public void create(String code, String name, int departmentId, int semester) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO courses(course_code, course_name, department_id, semester) VALUES (?, ?, ?, ?)")) {
            statement.setString(1, code);
            statement.setString(2, name);
            statement.setInt(3, departmentId);
            statement.setInt(4, semester);
            statement.executeUpdate();
        }
    }

    public boolean update(int id, String code, String name, int departmentId, int semester) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE courses SET course_code = ?, course_name = ?, department_id = ?, semester = ? WHERE id = ?")) {
            statement.setString(1, code);
            statement.setString(2, name);
            statement.setInt(3, departmentId);
            statement.setInt(4, semester);
            statement.setInt(5, id);
            return statement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM courses WHERE id = ?")) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }
}
