package com.examplanner.dao;

import com.examplanner.config.DbConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDao {
    public List<String[]> findAll() throws SQLException {
        List<String[]> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT id, code, name FROM departments ORDER BY code");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                result.add(new String[]{String.valueOf(rs.getInt("id")), rs.getString("code"), rs.getString("name")});
            }
        }
        return result;
    }
}
