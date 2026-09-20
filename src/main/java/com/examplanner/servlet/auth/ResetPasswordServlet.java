package com.examplanner.servlet.auth;

import com.examplanner.dao.UserDao;
import com.examplanner.util.JsonUtil;
import com.examplanner.util.PasswordUtil;
import com.examplanner.util.ValidationUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/api/auth/reset-password")
public class ResetPasswordServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (ValidationUtil.isBlank(username) || !ValidationUtil.validEmail(email)) {
            JsonUtil.send(response, 400, JsonUtil.message("error", "Valid username and email are required"));
            return;
        }
        if (!ValidationUtil.validPassword(password)) {
            JsonUtil.send(response, 400, JsonUtil.message("error", "Password must be 8+ characters with uppercase, lowercase and a number"));
            return;
        }

        try {
            boolean changed = userDao.resetPassword(username.trim(), email.trim(), PasswordUtil.hash(password));
            if (!changed) {
                JsonUtil.send(response, 404, JsonUtil.message("error", "Account details did not match"));
                return;
            }
            JsonUtil.send(response, 200, "{\"success\":true}");
        } catch (SQLException e) {
            JsonUtil.send(response, 500, JsonUtil.message("error", "Database error"));
        }
    }
}
