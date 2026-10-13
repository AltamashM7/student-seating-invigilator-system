package com.examplanner.servlet.admin;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
@WebServlet("/api/admin/rooms")
public class RoomServlet extends AcademicServlet {
 protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException { r.setAttribute("resource","rooms"); super.doPost(r,s); }
}
