package com.examplanner.servlet.admin;
import com.examplanner.dao.ConflictDao;
import com.examplanner.util.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet({"/api/admin/conflicts","/api/admin/statistics"})
public class ConflictServlet extends HttpServlet {
    private final ConflictDao dao=new ConflictDao();
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws IOException {
        try {int year=r.getParameter("academicYearId")==null?0:Input.id(r,"academicYearId");
            Object data=r.getServletPath().endsWith("statistics")?dao.statistics(year,(Integer)r.getSession().getAttribute("userId")):dao.scan(year);
            JsonUtil.send(s,200,JsonUtil.json(data));
        }catch(IllegalArgumentException e){JsonUtil.send(s,400,JsonUtil.message("error",e.getMessage()));}
        catch(SQLException e){log("Planning overview failed",e);JsonUtil.send(s,500,JsonUtil.message("error","Unable to load the planning overview. Apply migration 002 if upgrading."));}
    }
}
