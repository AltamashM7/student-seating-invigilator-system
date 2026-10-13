package com.examplanner.servlet;
import com.examplanner.dao.SeatingDao;
import com.examplanner.util.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
@WebServlet("/api/seating/search")
public class StudentSeatingServlet extends HttpServlet {
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws IOException {
        try { JsonUtil.send(s,200,JsonUtil.json(Map.of("results",new SeatingDao().search(Input.text(r,"rollNumber",30),Input.id(r,"academicYearId"))))); }
        catch(IllegalArgumentException e) { JsonUtil.send(s,400,JsonUtil.message("error",e.getMessage())); }
        catch(SQLException e) { log("Student seating search failed",e); JsonUtil.send(s,500,JsonUtil.message("error","Database error")); }
    }
}
