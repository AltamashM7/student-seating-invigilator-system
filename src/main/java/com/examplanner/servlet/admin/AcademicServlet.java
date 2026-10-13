package com.examplanner.servlet.admin;
import com.examplanner.dao.AcademicDao;
import com.examplanner.util.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
@WebServlet("/api/admin/data")
public class AcademicServlet extends HttpServlet {
    private final AcademicDao dao=new AcademicDao();
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws IOException {
        try { JsonUtil.send(s,200,JsonUtil.json(dao.snapshot(r.getParameter("academicYearId")==null?0:Input.id(r,"academicYearId")))); }
        catch(IllegalArgumentException e) { JsonUtil.send(s,400,JsonUtil.message("error",e.getMessage())); }
        catch(SQLException e) { log("Academic data read failed",e); JsonUtil.send(s,500,JsonUtil.message("error","Database error; check server logs")); }
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException {
        try { int id=dao.save(r,(Integer)r.getSession().getAttribute("userId")); JsonUtil.send(s,200,JsonUtil.json(Map.of("success",true,"id",id))); }
        catch(IllegalArgumentException e) { JsonUtil.send(s,400,JsonUtil.message("error",e.getMessage())); }
        catch(SQLException e) { log("Academic data write failed",e); JsonUtil.send(s,e.getSQLState()!=null&&e.getSQLState().startsWith("23")?409:500,JsonUtil.message("error",e.getSQLState()!=null&&e.getSQLState().startsWith("23")?"Duplicate value or record is referenced; remove dependent data first":"Database error; check server logs")); }
    }
}
