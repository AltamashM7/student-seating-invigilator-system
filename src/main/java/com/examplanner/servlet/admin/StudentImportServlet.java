package com.examplanner.servlet.admin;
import com.examplanner.dao.StudentImportDao;
import com.examplanner.dao.StudentImportDao.CsvRow;
import com.examplanner.util.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
@WebServlet("/api/admin/import/students")
@MultipartConfig(maxFileSize=2*1024*1024,maxRequestSize=3*1024*1024)
public class StudentImportServlet extends HttpServlet {
 protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException,ServletException {
  try {
   Part file=r.getPart("file"); if(file==null||file.getSize()==0) throw new IllegalArgumentException("CSV file is required");
   List<CsvRow> rows=new ArrayList<>(); boolean legacy;
   try(BufferedReader reader=new BufferedReader(new InputStreamReader(file.getInputStream(),StandardCharsets.UTF_8))) {
    String header=reader.readLine(); if(header==null) throw new IllegalArgumentException("Header is required"); header=header.replace("\uFEFF","").trim();
    legacy="roll_number,name,email,department_code,semester,course_code".equals(header);
    if(!legacy&&!"roll_number,name,email,department_code,semester,course_code,academic_year,division,requires_accessible".equals(header)) throw new IllegalArgumentException("CSV header does not match the template");
    String line; int n=1; while((line=reader.readLine())!=null) { n++; if(!line.isBlank()) rows.add(new CsvRow(n,line.split(",",-1))); }
   }
   if(rows.isEmpty()) throw new IllegalArgumentException("CSV has no data rows");
   JsonUtil.send(s,200,JsonUtil.json(new StudentImportDao().importRows(rows,legacy)));
  } catch(IllegalArgumentException | IllegalStateException e) { JsonUtil.send(s,400,JsonUtil.message("error",e.getMessage())); }
  catch(SQLException e) { log("CSV import database failure",e); JsonUtil.send(s,500,JsonUtil.message("error","Database error; earlier valid rows may have committed; retrying detects duplicates")); }
 }
}
