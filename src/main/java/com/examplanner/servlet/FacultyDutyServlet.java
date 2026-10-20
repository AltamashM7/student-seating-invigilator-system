package com.examplanner.servlet;
import com.examplanner.dao.InvigilationDao;
import com.examplanner.util.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet("/api/faculty/duties")
public class FacultyDutyServlet extends HttpServlet {
    private final InvigilationDao dao=new InvigilationDao();
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws IOException {
        try {JsonUtil.send(s,200,JsonUtil.json(dao.facultyData((Integer)r.getSession().getAttribute("userId"))));}
        catch(IllegalArgumentException e){JsonUtil.send(s,400,JsonUtil.message("error",e.getMessage()));}
        catch(SQLException e){log("Faculty duties read failed",e);JsonUtil.send(s,500,JsonUtil.message("error","Unable to load faculty duties. Check the database upgrade."));}
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException {
        try {
            int actor=(Integer)r.getSession().getAttribute("userId");String action=Input.text(r,"action",25);
            if("respond".equals(action)) dao.respond(actor,Input.id(r,"dutyId"),Input.id(r,"version"),Input.text(r,"response",10),r.getParameter("reason"));
            else dao.saveAvailability(r,actor,false);
            JsonUtil.send(s,200,JsonUtil.json(Map.of("success",true)));
        }catch(IllegalArgumentException e){JsonUtil.send(s,409,JsonUtil.message("error",e.getMessage()));}
        catch(SQLException e){log("Faculty duty change failed",e);JsonUtil.send(s,409,JsonUtil.message("error","Unable to save. Refresh the duty schedule and try again."));}
    }
}
