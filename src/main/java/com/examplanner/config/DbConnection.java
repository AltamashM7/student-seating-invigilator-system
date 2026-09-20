package com.examplanner.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/exam_seating?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        String url = valueOrDefault(System.getenv("DB_URL"), DEFAULT_URL);
        String user = valueOrDefault(System.getenv("DB_USER"), "root");
        String password = valueOrDefault(System.getenv("DB_PASSWORD"), "");
        return DriverManager.getConnection(url, user, password);
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
