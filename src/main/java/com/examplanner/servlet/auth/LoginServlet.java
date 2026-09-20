package com.examplanner.servlet.auth;

import com.examplanner.dao.UserDao;
import com.examplanner.model.User;
import com.examplanner.util.JsonUtil;
import com.examplanner.util.PasswordUtil;
import com.examplanner.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/api/auth/login")
public class LoginServlet extends HttpServlet {
    private final UserDao userDao = new UserDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (ValidationUtil.isBlank(username) || ValidationUtil.isBlank(password)) {
            JsonUtil.send(response, 400, JsonUtil.message("error", "Username and password are required"));
            return;
        }

        try {
            User user = userDao.findByUsername(username.trim());
            if (user == null || !PasswordUtil.hash(password).equals(user.getPasswordHash())) {
                JsonUtil.send(response, 401, JsonUtil.message("error", "Invalid username or password"));
                return;
            }

            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = request.getSession(true);
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole());
            session.setAttribute("displayName", user.getDisplayName());
            session.setMaxInactiveInterval(30 * 60);

            String json = "{\"success\":true,\"role\":" + JsonUtil.quote(user.getRole())
                    + ",\"name\":" + JsonUtil.quote(user.getDisplayName()) + "}";
            JsonUtil.send(response, 200, json);
        } catch (SQLException e) {
            JsonUtil.send(response, 500, JsonUtil.message("error", "Database error"));
        }
    }
}
