package com.examplanner.dao;

import com.examplanner.config.DbConnection;
import com.examplanner.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserDao {
    public User findByUsername(String username) throws SQLException {
        String sql = """
                SELECT u.id, u.username, u.password_hash, u.role,
                       CASE WHEN u.role = 'ADMIN' THEN a.name ELSE f.name END AS display_name,
                       CASE WHEN u.role = 'ADMIN' THEN a.email ELSE f.email END AS email
                FROM users u
                LEFT JOIN admins a ON a.user_id = u.id
                LEFT JOIN faculty f ON f.user_id = u.id
                WHERE u.username = ?
                """;
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new User(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getString("display_name"),
                        rs.getString("email")
                );
            }
        }
    }

    public void createUser(String username, String passwordHash, String role,
                           String name, String email, String employeeCode) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int userId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO users(username, password_hash, role) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, username);
                    statement.setString(2, passwordHash);
                    statement.setString(3, role);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        keys.next();
                        userId = keys.getInt(1);
                    }
                }

                if ("ADMIN".equals(role)) {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO admins(user_id, name, email) VALUES (?, ?, ?)")) {
                        statement.setInt(1, userId);
                        statement.setString(2, name);
                        statement.setString(3, email);
                        statement.executeUpdate();
                    }
                } else {
                    try (PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO faculty(user_id, employee_code, name, email) VALUES (?, ?, ?, ?)")) {
                        statement.setInt(1, userId);
                        statement.setString(2, employeeCode);
                        statement.setString(3, name);
                        statement.setString(4, email);
                        statement.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean resetPassword(String username, String email, String newPasswordHash) throws SQLException {
        String lookup = """
                SELECT u.id
                FROM users u
                LEFT JOIN admins a ON a.user_id = u.id
                LEFT JOIN faculty f ON f.user_id = u.id
                WHERE u.username = ?
                  AND (a.email = ? OR f.email = ?)
                """;
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(lookup)) {
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, email);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                int userId = rs.getInt("id");
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE users SET password_hash = ? WHERE id = ?")) {
                    update.setString(1, newPasswordHash);
                    update.setInt(2, userId);
                    update.executeUpdate();
                    return true;
                }
            }
        }
    }
}
