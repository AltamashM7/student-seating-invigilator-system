package com.examplanner.servlet.admin;

import com.examplanner.dao.ExamDao;
import com.examplanner.model.Exam;
import com.examplanner.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/api/admin/exams")
public class ExamServlet extends HttpServlet {
    private final ExamDao examDao = new ExamDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            JsonUtil.send(response, 200, toJson(examDao.findAll(false)));
        } catch (SQLException e) {
            JsonUtil.send(response, 500, JsonUtil.message("error", "Database error"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String action = request.getParameter("action");
        try {
            if ("delete".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                boolean deleted = examDao.deleteDraft(id);
                JsonUtil.send(response, deleted ? 200 : 404, deleted ? "{\"success\":true}" : JsonUtil.message("error", "Exam not found"));
                return;
            }
            if ("status".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                String status = request.getParameter("status");
                if (!validStatus(status)) {
                    JsonUtil.send(response, 400, JsonUtil.message("error", "Invalid status"));
                    return;
                }
                boolean updated = examDao.changeStatus(id, status);
                JsonUtil.send(response, updated ? 200 : 404, updated ? "{\"success\":true}" : JsonUtil.message("error", "Exam not found"));
                return;
            }

            Integer id = "update".equals(action) ? Integer.parseInt(request.getParameter("id")) : null;
            int courseId = Integer.parseInt(request.getParameter("courseId"));
            LocalDate date = LocalDate.parse(request.getParameter("examDate"));
            LocalTime start = LocalTime.parse(request.getParameter("startTime"));
            LocalTime end = LocalTime.parse(request.getParameter("endTime"));
            String status = request.getParameter("status");
            List<Integer> roomIds = parseRoomIds(request.getParameterValues("roomId"));

            if (!end.isAfter(start) || !validEditableStatus(status) || roomIds.isEmpty()) {
                JsonUtil.send(response, 400, JsonUtil.message("error", "Valid date, time, status and at least one room are required"));
                return;
            }

            if (!"CANCELLED".equals(status)) {
                List<String> conflicts = examDao.findConflicts(id, courseId, date, start, end, roomIds);
                if (!conflicts.isEmpty()) {
                    JsonUtil.send(response, 409, conflictJson(conflicts));
                    return;
                }
            }

            if (id == null) {
                int examId = examDao.create(courseId, date, start, end, status, roomIds);
                JsonUtil.send(response, 201, "{\"success\":true,\"id\":" + examId + "}");
            } else {
                boolean updated = examDao.updateDraft(id, courseId, date, start, end, status, roomIds);
                JsonUtil.send(response, updated ? 200 : 404, updated ? "{\"success\":true}" : JsonUtil.message("error", "Exam not found"));
            }
        } catch (NumberFormatException | DateTimeParseException e) {
            JsonUtil.send(response, 400, JsonUtil.message("error", "Invalid exam input"));
        } catch (IllegalStateException e) {
            JsonUtil.send(response, 409, JsonUtil.message("error", e.getMessage()));
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                JsonUtil.send(response, 409, JsonUtil.message("error", "Exam references invalid or duplicate data"));
            } else {
                JsonUtil.send(response, 500, JsonUtil.message("error", "Database error"));
            }
        }
    }

    private List<Integer> parseRoomIds(String[] values) {
        List<Integer> ids = new ArrayList<>();
        if (values == null) return ids;
        for (String value : values) ids.add(Integer.parseInt(value));
        return ids;
    }

    private boolean validStatus(String status) {
        return "DRAFT".equals(status) || "SCHEDULED".equals(status)
                || "COMPLETED".equals(status) || "CANCELLED".equals(status);
    }

    private boolean validEditableStatus(String status) {
        return "DRAFT".equals(status) || "SCHEDULED".equals(status);
    }

    private String conflictJson(List<String> conflicts) {
        StringBuilder json = new StringBuilder("{\"error\":\"Timetable conflict\",\"conflicts\":[");
        for (int i = 0; i < conflicts.size(); i++) {
            if (i > 0) json.append(',');
            json.append(JsonUtil.quote(conflicts.get(i)));
        }
        return json.append("]}").toString();
    }

    private String toJson(List<Exam> exams) {
        StringBuilder json = new StringBuilder("{\"exams\":[");
        for (int i = 0; i < exams.size(); i++) {
            Exam exam = exams.get(i);
            if (i > 0) json.append(',');
            json.append("{\"id\":").append(exam.getId())
                    .append(",\"courseId\":").append(exam.getCourseId())
                    .append(",\"courseCode\":").append(JsonUtil.quote(exam.getCourseCode()))
                    .append(",\"courseName\":").append(JsonUtil.quote(exam.getCourseName()))
                    .append(",\"examDate\":").append(JsonUtil.quote(exam.getExamDate()))
                    .append(",\"startTime\":").append(JsonUtil.quote(exam.getStartTime()))
                    .append(",\"endTime\":").append(JsonUtil.quote(exam.getEndTime()))
                    .append(",\"status\":").append(JsonUtil.quote(exam.getStatus()))
                    .append(",\"rooms\":").append(JsonUtil.quote(exam.getRooms())).append('}');
        }
        return json.append("]}").toString();
    }
}
