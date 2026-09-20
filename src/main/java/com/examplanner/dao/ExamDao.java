package com.examplanner.dao;

import com.examplanner.config.DbConnection;
import com.examplanner.model.Exam;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ExamDao {
    public List<Exam> findAll(boolean publicOnly) throws SQLException {
        List<Exam> exams = new ArrayList<>();
        String sql = """
                SELECT e.id, e.course_id, c.course_code, c.course_name, e.exam_date, e.start_time, e.end_time, e.status,
                       COALESCE(GROUP_CONCAT(r.room_number ORDER BY r.room_number SEPARATOR ', '), '') AS rooms
                FROM examinations e
                JOIN courses c ON c.id = e.course_id
                LEFT JOIN examination_rooms er ON er.examination_id = e.id
                LEFT JOIN rooms r ON r.id = er.room_id
                %s
                GROUP BY e.id, e.course_id, c.course_code, c.course_name, e.exam_date, e.start_time, e.end_time, e.status
                ORDER BY e.exam_date, e.start_time, c.course_code
                """.formatted(publicOnly ? "WHERE e.status IN ('SCHEDULED', 'COMPLETED')" : "");
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                exams.add(new Exam(
                        rs.getInt("id"), rs.getInt("course_id"), rs.getString("course_code"), rs.getString("course_name"),
                        rs.getDate("exam_date").toString(), rs.getTime("start_time").toString(), rs.getTime("end_time").toString(),
                        rs.getString("status"), rs.getString("rooms")
                ));
            }
        }
        return exams;
    }

    public List<String> findConflicts(Integer excludeExamId, int courseId, LocalDate date,
                                      LocalTime start, LocalTime end, List<Integer> roomIds) throws SQLException {
        List<String> conflicts = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection()) {
            conflicts.addAll(findRoomConflicts(connection, excludeExamId, date, start, end, roomIds));
            conflicts.addAll(findStudentConflicts(connection, excludeExamId, courseId, date, start, end));
        }
        return conflicts;
    }

    public int create(int courseId, LocalDate date, LocalTime start, LocalTime end,
                      String status, List<Integer> roomIds) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int examId;
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO examinations(course_id, exam_date, start_time, end_time, status) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    setExamValues(statement, courseId, date, start, end, status);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        keys.next();
                        examId = keys.getInt(1);
                    }
                }
                insertRooms(connection, examId, roomIds);
                connection.commit();
                return examId;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean updateDraft(int id, int courseId, LocalDate date, LocalTime start, LocalTime end,
                               String status, List<Integer> roomIds) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String current = getStatus(connection, id);
                if (current == null) return false;
                if (!"DRAFT".equals(current)) throw new IllegalStateException("Only draft exams can be edited");

                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE examinations SET course_id = ?, exam_date = ?, start_time = ?, end_time = ?, status = ? WHERE id = ?")) {
                    setExamValues(statement, courseId, date, start, end, status);
                    statement.setInt(6, id);
                    statement.executeUpdate();
                }
                try (PreparedStatement delete = connection.prepareStatement("DELETE FROM examination_rooms WHERE examination_id = ?")) {
                    delete.setInt(1, id);
                    delete.executeUpdate();
                }
                insertRooms(connection, id, roomIds);
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public boolean changeStatus(int id, String nextStatus) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            String current = getStatus(connection, id);
            if (current == null) return false;
            if (!allowedTransition(current, nextStatus)) {
                throw new IllegalStateException("Invalid state transition from " + current + " to " + nextStatus);
            }
            try (PreparedStatement statement = connection.prepareStatement("UPDATE examinations SET status = ? WHERE id = ?")) {
                statement.setString(1, nextStatus);
                statement.setInt(2, id);
                return statement.executeUpdate() > 0;
            }
        }
    }

    public boolean deleteDraft(int id) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            String current = getStatus(connection, id);
            if (current == null) return false;
            if (!"DRAFT".equals(current) && !"CANCELLED".equals(current)) {
                throw new IllegalStateException("Only draft or cancelled exams can be deleted");
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM examinations WHERE id = ?")) {
                statement.setInt(1, id);
                return statement.executeUpdate() > 0;
            }
        }
    }

    private List<String> findRoomConflicts(Connection connection, Integer excludeExamId, LocalDate date,
                                           LocalTime start, LocalTime end, List<Integer> roomIds) throws SQLException {
        List<String> result = new ArrayList<>();
        if (roomIds.isEmpty()) return result;
        String placeholders = String.join(",", java.util.Collections.nCopies(roomIds.size(), "?"));
        String sql = """
                SELECT DISTINCT c.course_code, r.room_number
                FROM examinations e
                JOIN courses c ON c.id = e.course_id
                JOIN examination_rooms er ON er.examination_id = e.id
                JOIN rooms r ON r.id = er.room_id
                WHERE e.exam_date = ? AND e.status <> 'CANCELLED'
                  AND e.start_time < ? AND e.end_time > ?
                  AND er.room_id IN (%s)
                """.formatted(placeholders);
        if (excludeExamId != null) sql += " AND e.id <> ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            statement.setDate(index++, Date.valueOf(date));
            statement.setTime(index++, Time.valueOf(end));
            statement.setTime(index++, Time.valueOf(start));
            for (Integer roomId : roomIds) statement.setInt(index++, roomId);
            if (excludeExamId != null) statement.setInt(index, excludeExamId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add("Room " + rs.getString("room_number") + " overlaps with " + rs.getString("course_code"));
                }
            }
        }
        return result;
    }

    private List<String> findStudentConflicts(Connection connection, Integer excludeExamId, int courseId,
                                              LocalDate date, LocalTime start, LocalTime end) throws SQLException {
        List<String> result = new ArrayList<>();
        String sql = """
                SELECT DISTINCT e.id, c.course_code
                FROM examinations e
                JOIN courses c ON c.id = e.course_id
                JOIN course_enrollments existing_enrollment ON existing_enrollment.course_id = e.course_id
                JOIN course_enrollments new_enrollment
                  ON new_enrollment.student_id = existing_enrollment.student_id
                 AND new_enrollment.course_id = ?
                WHERE e.exam_date = ? AND e.status <> 'CANCELLED'
                  AND e.start_time < ? AND e.end_time > ?
                """;
        if (excludeExamId != null) sql += " AND e.id <> ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, courseId);
            statement.setDate(2, Date.valueOf(date));
            statement.setTime(3, Time.valueOf(end));
            statement.setTime(4, Time.valueOf(start));
            if (excludeExamId != null) statement.setInt(5, excludeExamId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    result.add("Student enrollment overlaps with " + rs.getString("course_code"));
                }
            }
        }
        return result;
    }

    private void insertRooms(Connection connection, int examId, List<Integer> roomIds) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO examination_rooms(examination_id, room_id) VALUES (?, ?)")) {
            for (Integer roomId : roomIds) {
                statement.setInt(1, examId);
                statement.setInt(2, roomId);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void setExamValues(PreparedStatement statement, int courseId, LocalDate date,
                               LocalTime start, LocalTime end, String status) throws SQLException {
        statement.setInt(1, courseId);
        statement.setDate(2, Date.valueOf(date));
        statement.setTime(3, Time.valueOf(start));
        statement.setTime(4, Time.valueOf(end));
        statement.setString(5, status);
    }

    private String getStatus(Connection connection, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT status FROM examinations WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getString("status") : null;
            }
        }
    }

    private boolean allowedTransition(String current, String next) {
        if (current.equals(next)) return true;
        return switch (current) {
            case "DRAFT" -> next.equals("SCHEDULED") || next.equals("CANCELLED");
            case "SCHEDULED" -> next.equals("COMPLETED") || next.equals("CANCELLED");
            default -> false;
        };
    }
}
