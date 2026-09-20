package com.examplanner.servlet.admin;

import com.examplanner.dao.CourseDao;
import com.examplanner.model.Course;
import com.examplanner.util.JsonUtil;
import com.examplanner.util.ValidationUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/api/admin/courses")
public class CourseServlet extends HttpServlet {
    private final CourseDao courseDao = new CourseDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            List<Course> courses = courseDao.findAll();
            StringBuilder json = new StringBuilder("{\"courses\":[");
            for (int i = 0; i < courses.size(); i++) {
                Course course = courses.get(i);
                if (i > 0) json.append(',');
                json.append("{\"id\":").append(course.getId())
                        .append(",\"code\":").append(JsonUtil.quote(course.getCode()))
                        .append(",\"name\":").append(JsonUtil.quote(course.getName()))
                        .append(",\"departmentId\":").append(course.getDepartmentId())
                        .append(",\"departmentCode\":").append(JsonUtil.quote(course.getDepartmentCode()))
                        .append(",\"semester\":").append(course.getSemester()).append('}');
            }
            json.append("]}");
            JsonUtil.send(response, 200, json.toString());
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
                boolean deleted = courseDao.delete(id);
                JsonUtil.send(response, deleted ? 200 : 404, deleted ? "{\"success\":true}" : JsonUtil.message("error", "Course not found"));
                return;
            }

            String code = request.getParameter("code");
            String name = request.getParameter("name");
            int departmentId = Integer.parseInt(request.getParameter("departmentId"));
            int semester = Integer.parseInt(request.getParameter("semester"));
            if (ValidationUtil.isBlank(code) || ValidationUtil.isBlank(name) || departmentId < 1 || semester < 1 || semester > 8) {
                JsonUtil.send(response, 400, JsonUtil.message("error", "Valid course details are required"));
                return;
            }

            if ("update".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                boolean updated = courseDao.update(id, code.trim(), name.trim(), departmentId, semester);
                JsonUtil.send(response, updated ? 200 : 404, updated ? "{\"success\":true}" : JsonUtil.message("error", "Course not found"));
            } else {
                courseDao.create(code.trim(), name.trim(), departmentId, semester);
                JsonUtil.send(response, 201, "{\"success\":true}");
            }
        } catch (NumberFormatException e) {
            JsonUtil.send(response, 400, JsonUtil.message("error", "Invalid numeric value"));
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                JsonUtil.send(response, 409, JsonUtil.message("error", "Duplicate course or course is already in use"));
            } else {
                JsonUtil.send(response, 500, JsonUtil.message("error", "Database error"));
            }
        }
    }
}
