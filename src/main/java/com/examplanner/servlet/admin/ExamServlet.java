package com.examplanner.servlet.admin;
import com.examplanner.dao.ExamDao;
import com.examplanner.util.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
@WebServlet("/api/admin/exams")
public class ExamServlet extends HttpServlet {
 private final ExamDao dao=new ExamDao();
 protected void doGet(HttpServletRequest r,HttpServletResponse s) throws IOException {
  try { int year=r.getParameter("academicYearId")==null?0:Input.id(r,"academicYearId"); JsonUtil.send(s,200,JsonUtil.json(Map.of("exams",dao.list(false,year)))); }
  catch(IllegalArgumentException e) { JsonUtil.send(s,400,JsonUtil.message("error",e.getMessage())); }
  catch(SQLException e) { log("Exam read failed",e); JsonUtil.send(s,500,JsonUtil.message("error","Database error")); }
 }
 protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException {
  try { JsonUtil.send(s,200,JsonUtil.json(Map.of("success",true,"id",dao.save(r)))); }
  catch(IllegalArgumentException e) { JsonUtil.send(s,409,JsonUtil.message("error",e.getMessage())); }
  catch(SQLException e) { log("Exam mutation failed",e); JsonUtil.send(s,409,JsonUtil.message("error","Invalid reference or database failure; no changes saved")); }
 }
}
