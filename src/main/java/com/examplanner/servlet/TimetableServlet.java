package com.examplanner.servlet;

import com.examplanner.dao.ExamDao;
import com.examplanner.model.Exam;
import com.examplanner.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/api/timetable")
public class TimetableServlet extends HttpServlet {
    private final ExamDao examDao = new ExamDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            List<Exam> exams = examDao.findAll(true);
            StringBuilder json = new StringBuilder("{\"exams\":[");
            for (int i = 0; i < exams.size(); i++) {
                Exam exam = exams.get(i);
                if (i > 0) json.append(',');
                json.append("{\"courseCode\":").append(JsonUtil.quote(exam.getCourseCode()))
                        .append(",\"courseName\":").append(JsonUtil.quote(exam.getCourseName()))
                        .append(",\"examDate\":").append(JsonUtil.quote(exam.getExamDate()))
                        .append(",\"startTime\":").append(JsonUtil.quote(exam.getStartTime()))
                        .append(",\"endTime\":").append(JsonUtil.quote(exam.getEndTime()))
                        .append(",\"status\":").append(JsonUtil.quote(exam.getStatus()))
                        .append(",\"rooms\":").append(JsonUtil.quote(exam.getRooms())).append('}');
            }
            JsonUtil.send(response, 200, json.append("]}").toString());
        } catch (SQLException e) {
            JsonUtil.send(response, 500, JsonUtil.message("error", "Database error"));
        }
    }
}
