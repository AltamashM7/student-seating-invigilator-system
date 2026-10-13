package com.examplanner.servlet.admin;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
@WebServlet("/api/admin/courses")
public class CourseServlet extends AcademicServlet {
 protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException { r.setAttribute("resource","courses"); super.doPost(r,s); }
}
