package com.examplanner.servlet.auth;

import com.examplanner.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/api/auth/session")
public class SessionServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            JsonUtil.send(response, 200, "{\"authenticated\":false}");
            return;
        }

        String json = "{\"authenticated\":true,\"username\":"
                + JsonUtil.quote(String.valueOf(session.getAttribute("username")))
                + ",\"role\":" + JsonUtil.quote(String.valueOf(session.getAttribute("role")))
                + ",\"name\":" + JsonUtil.quote(String.valueOf(session.getAttribute("displayName"))) + "}";
        JsonUtil.send(response, 200, json);
    }
}
