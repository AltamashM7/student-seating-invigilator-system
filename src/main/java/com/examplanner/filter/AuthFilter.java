package com.examplanner.filter;

import com.examplanner.util.JsonUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebFilter(urlPatterns = {"/api/admin/*", "/api/faculty/*"})
public class AuthFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            JsonUtil.send(httpResponse, 401, JsonUtil.message("error", "Login required"));
            return;
        }

        String role = String.valueOf(session.getAttribute("role"));
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());

        if (path.startsWith("/api/admin/") && !"ADMIN".equals(role)) {
            JsonUtil.send(httpResponse, 403, JsonUtil.message("error", "Admin access required"));
            return;
        }
        if (path.startsWith("/api/faculty/") && !"FACULTY".equals(role)) {
            JsonUtil.send(httpResponse, 403, JsonUtil.message("error", "Faculty access required"));
            return;
        }

        chain.doFilter(request, response);
    }
}
