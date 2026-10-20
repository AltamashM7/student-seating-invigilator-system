package com.examplanner.servlet;
import com.examplanner.dao.ReportDao;
import com.examplanner.service.PdfReport;
import com.examplanner.util.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet({"/api/admin/reports","/api/faculty/reports"})
public class ReportServlet extends HttpServlet {
    private int optional(HttpServletRequest r,String key){return r.getParameter(key)==null||r.getParameter(key).isBlank()?0:Input.id(r,key);}
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws IOException {
        try {
            String type=Input.text(r,"type",30),format=Input.text(r,"format",5);
            if(!java.util.Set.of("pdf","csv").contains(format))throw new IllegalArgumentException("Choose PDF or CSV");
            var report=new ReportDao().report(type,optional(r,"academicYearId"),optional(r,"examId"),optional(r,"roomId"),(Integer)r.getSession().getAttribute("userId"),"FACULTY".equals(r.getSession().getAttribute("role")));
            PdfReport writer=new PdfReport();byte[] bytes="pdf".equals(format)?writer.create(report):writer.csv(report);
            s.setContentType("pdf".equals(format)?"application/pdf":"text/csv; charset=UTF-8");s.setHeader("Content-Disposition","attachment; filename=\""+type+"."+format+"\"");s.setContentLength(bytes.length);s.getOutputStream().write(bytes);
        }catch(IllegalArgumentException e){JsonUtil.send(s,400,JsonUtil.message("error",e.getMessage()));}
        catch(SQLException e){log("Report export failed",e);JsonUtil.send(s,500,JsonUtil.message("error","The report could not be generated."));}
    }
}
