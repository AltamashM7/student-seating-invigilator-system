package com.examplanner.servlet.admin;

import com.examplanner.dao.StudentImportDao;
import com.examplanner.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/api/admin/import/students")
@MultipartConfig(maxFileSize = 2 * 1024 * 1024)
public class StudentImportServlet extends HttpServlet {
    private final StudentImportDao importDao = new StudentImportDao();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        Part file = request.getPart("file");
        if (file == null || file.getSize() == 0) {
            JsonUtil.send(response, 400, JsonUtil.message("error", "CSV file is required"));
            return;
        }

        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    continue;
                }
                if (!line.isBlank()) rows.add(line.split(",", -1));
            }
        }

        try {
            StudentImportDao.ImportResult result = importDao.importRows(rows);
            StringBuilder errors = new StringBuilder("[");
            for (int i = 0; i < result.errors.size(); i++) {
                if (i > 0) errors.append(',');
                errors.append(JsonUtil.quote(result.errors.get(i)));
            }
            errors.append(']');
            String json = "{\"imported\":" + result.imported + ",\"duplicates\":" + result.duplicates
                    + ",\"invalid\":" + result.errors.size() + ",\"errors\":" + errors + "}";
            JsonUtil.send(response, 200, json);
        } catch (SQLException e) {
            JsonUtil.send(response, 500, JsonUtil.message("error", "Database error"));
        }
    }
}
