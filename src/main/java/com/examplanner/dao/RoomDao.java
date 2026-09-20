package com.examplanner.dao;

import com.examplanner.config.DbConnection;
import com.examplanner.model.Room;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class RoomDao {
    public List<Room> findAll() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT id, room_number, capacity FROM rooms ORDER BY room_number");
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                rooms.add(new Room(rs.getInt("id"), rs.getString("room_number"), rs.getInt("capacity")));
            }
        }
        return rooms;
    }

    public int create(String roomNumber, int capacity) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int roomId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO rooms(room_number, capacity) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, roomNumber);
                    statement.setInt(2, capacity);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        keys.next();
                        roomId = keys.getInt(1);
                    }
                }
                createSeats(connection, roomId, 1, capacity);
                connection.commit();
                return roomId;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean update(int id, String roomNumber, int capacity) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int oldCapacity;
                try (PreparedStatement find = connection.prepareStatement("SELECT capacity FROM rooms WHERE id = ?")) {
                    find.setInt(1, id);
                    try (ResultSet rs = find.executeQuery()) {
                        if (!rs.next()) return false;
                        oldCapacity = rs.getInt("capacity");
                    }
                }

                try (PreparedStatement update = connection.prepareStatement("UPDATE rooms SET room_number = ?, capacity = ? WHERE id = ?")) {
                    update.setString(1, roomNumber);
                    update.setInt(2, capacity);
                    update.setInt(3, id);
                    update.executeUpdate();
                }

                if (capacity > oldCapacity) {
                    createSeats(connection, id, oldCapacity + 1, capacity);
                } else if (capacity < oldCapacity) {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM seats WHERE room_id = ? AND CAST(SUBSTRING(seat_number, 2) AS UNSIGNED) > ?")) {
                        delete.setInt(1, id);
                        delete.setInt(2, capacity);
                        delete.executeUpdate();
                    }
                }
                connection.commit();
                return true;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean delete(int id) throws SQLException {
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM rooms WHERE id = ?")) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private void createSeats(Connection connection, int roomId, int from, int to) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO seats(room_id, seat_number) VALUES (?, ?)")) {
            for (int i = from; i <= to; i++) {
                statement.setInt(1, roomId);
                statement.setString(2, String.format("S%03d", i));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
