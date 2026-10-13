package com.examplanner.servlet;
import com.examplanner.dao.ExamDao;
import com.examplanner.config.DbConnection;
import com.examplanner.util.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;
@WebServlet("/api/timetable")
public class TimetableServlet extends HttpServlet {
 protected void doGet(HttpServletRequest r,HttpServletResponse s) throws IOException {
  try(Connection c=DbConnection.getConnection()) { int year=r.getParameter("academicYearId")==null?0:Input.id(r,"academicYearId"); JsonUtil.send(s,200,JsonUtil.json(Map.of("exams",new ExamDao().list(true,year),"years",Sql.rows(c,"SELECT id,label,is_current AS isCurrent FROM academic_years ORDER BY start_year DESC")))); }
  catch(IllegalArgumentException e) { JsonUtil.send(s,400,JsonUtil.message("error",e.getMessage())); }
  catch(SQLException e) { log("Timetable read failed",e); JsonUtil.send(s,500,JsonUtil.message("error","Database error")); }
 }
}
