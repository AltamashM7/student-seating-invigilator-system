package com.examplanner.servlet.admin;

import com.examplanner.dao.RoomDao;
import com.examplanner.model.Room;
import com.examplanner.util.JsonUtil;
import com.examplanner.util.ValidationUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/api/admin/rooms")
public class RoomServlet extends HttpServlet {
    private final RoomDao roomDao = new RoomDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            List<Room> rooms = roomDao.findAll();
            StringBuilder json = new StringBuilder("{\"rooms\":[");
            for (int i = 0; i < rooms.size(); i++) {
                Room room = rooms.get(i);
                if (i > 0) json.append(',');
                json.append("{\"id\":").append(room.getId())
                        .append(",\"roomNumber\":").append(JsonUtil.quote(room.getRoomNumber()))
                        .append(",\"capacity\":").append(room.getCapacity()).append('}');
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
                boolean deleted = roomDao.delete(id);
                JsonUtil.send(response, deleted ? 200 : 404, deleted ? "{\"success\":true}" : JsonUtil.message("error", "Room not found"));
                return;
            }

            String roomNumber = request.getParameter("roomNumber");
            int capacity = Integer.parseInt(request.getParameter("capacity"));
            if (ValidationUtil.isBlank(roomNumber) || capacity < 1 || capacity > 500) {
                JsonUtil.send(response, 400, JsonUtil.message("error", "Valid room number and capacity are required"));
                return;
            }

            if ("update".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                boolean updated = roomDao.update(id, roomNumber.trim(), capacity);
                JsonUtil.send(response, updated ? 200 : 404, updated ? "{\"success\":true}" : JsonUtil.message("error", "Room not found"));
            } else {
                int id = roomDao.create(roomNumber.trim(), capacity);
                JsonUtil.send(response, 201, "{\"success\":true,\"id\":" + id + "}");
            }
        } catch (NumberFormatException e) {
            JsonUtil.send(response, 400, JsonUtil.message("error", "Invalid numeric value"));
        } catch (SQLException e) {
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                JsonUtil.send(response, 409, JsonUtil.message("error", "Duplicate room or room is already in use"));
            } else {
                JsonUtil.send(response, 500, JsonUtil.message("error", "Database error"));
            }
        }
    }
}
