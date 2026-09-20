package com.examplanner.servlet.auth;

import com.examplanner.dao.UserDao;
import com.examplanner.util.JsonUtil;
import com.examplanner.util.PasswordUtil;
import com.examplanner.util.ValidationUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/api/auth/register")
public class RegisterServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !"ADMIN".equals(session.getAttribute("role"))) {
            JsonUtil.send(response, 403, JsonUtil.message("error", "Admin access required"));
            return;
        }

        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String role = request.getParameter("role");
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String employeeCode = request.getParameter("employeeCode");

        if (ValidationUtil.isBlank(username) || ValidationUtil.isBlank(name)
                || ValidationUtil.isBlank(email) || ValidationUtil.isBlank(role)) {
            JsonUtil.send(response, 400, JsonUtil.message("error", "Required fields are missing"));
            return;
        }
        if (!"ADMIN".equals(role) && !"FACULTY".equals(role)) {
            JsonUtil.send(response, 400, JsonUtil.message("error", "Invalid role"));
            return;
        }
        if ("FACULTY".equals(role) && ValidationUtil.isBlank(employeeCode)) {
            JsonUtil.send(response, 400, JsonUtil.message("error", "Employee code is required for faculty"));
            return;
        }
        if (!ValidationUtil.validEmail(email)) {
            JsonUtil.send(response, 400, JsonUtil.message("error", "Invalid email address"));
            return;
        }
        if (!ValidationUtil.validPassword(password)) {
            JsonUtil.send(response, 400, JsonUtil.message("error", "Password must be 8+ characters with uppercase, lowercase and a number"));
            return;
        }

        try {
            userDao.createUser(username.trim(), PasswordUtil.hash(password), role,
                    name.trim(), email.trim(), employeeCode == null ? null : employeeCode.trim());
            JsonUtil.send(response, 201, "{\"success\":true}");
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                JsonUtil.send(response, 409, JsonUtil.message("error", "Username, email or employee code already exists"));
            } else {
                JsonUtil.send(response, 500, JsonUtil.message("error", "Database error"));
            }
        }
    }
}
