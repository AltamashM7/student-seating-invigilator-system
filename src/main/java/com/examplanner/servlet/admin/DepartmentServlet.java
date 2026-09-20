package com.examplanner.servlet.admin;

import com.examplanner.dao.DepartmentDao;
import com.examplanner.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/api/admin/departments")
public class DepartmentServlet extends HttpServlet {
    private final DepartmentDao departmentDao = new DepartmentDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            List<String[]> departments = departmentDao.findAll();
            StringBuilder json = new StringBuilder("{\"departments\":[");
            for (int i = 0; i < departments.size(); i++) {
                String[] department = departments.get(i);
                if (i > 0) json.append(',');
                json.append("{\"id\":").append(department[0])
                        .append(",\"code\":").append(JsonUtil.quote(department[1]))
                        .append(",\"name\":").append(JsonUtil.quote(department[2])).append('}');
            }
            json.append("]}");
            JsonUtil.send(response, 200, json.toString());
        } catch (SQLException e) {
            JsonUtil.send(response, 500, JsonUtil.message("error", "Database error"));
        }
    }
}
