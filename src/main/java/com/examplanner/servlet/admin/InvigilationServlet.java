package com.examplanner.servlet.admin;
import com.examplanner.dao.InvigilationDao;
import com.examplanner.util.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet("/api/admin/invigilation")
public class InvigilationServlet extends HttpServlet {
    private final InvigilationDao dao=new InvigilationDao();
    private int optional(HttpServletRequest r,String key) {return r.getParameter(key)==null||r.getParameter(key).isBlank()?0:Input.integer(r,key,0,Integer.MAX_VALUE);}
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws IOException {
        try {
            int year=optional(r,"academicYearId"),exam=optional(r,"examId"); String action=r.getParameter("action");
            Object result="preview".equals(action)?dao.preview(year,exam,Input.flag(r,"replaceExisting"))
                    :"previewReassign".equals(action)?dao.reassignPreview(year,Input.id(r,"dutyId"),Input.id(r,"facultyId")):dao.data(year);
            JsonUtil.send(s,200,JsonUtil.json(result));
        } catch(IllegalArgumentException e){JsonUtil.send(s,409,JsonUtil.message("error",e.getMessage()));}
        catch(SQLException e){log("Invigilation read failed",e);JsonUtil.send(s,500,JsonUtil.message("error","Unable to load duty planning. Apply database migration 002 if upgrading."));}
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException {
        try {
            int actor=(Integer)r.getSession().getAttribute("userId");String action=Input.text(r,"action",25);
            switch(action) {
                case "generate" -> dao.generate(optional(r,"academicYearId"),optional(r,"examId"),Input.flag(r,"replaceExisting"),Input.flag(r,"confirmReplace"),Input.text(r,"expectedRevision",64),actor,Input.text(r,"reason",250));
                case "reassign" -> dao.reassign(optional(r,"academicYearId"),Input.id(r,"dutyId"),Input.id(r,"facultyId"),Input.text(r,"expectedRevision",64),actor,Input.text(r,"reason",250));
                case "settings","unavailable","deleteUnavailable" -> dao.saveAvailability(r,actor,true);
                case "rules","courseConflict" -> dao.savePolicy(r);
                case "clear" -> dao.clear(Input.id(r,"examId"),actor,Input.text(r,"reason",250));
                default -> throw new IllegalArgumentException("Unknown invigilation action");
            }
            JsonUtil.send(s,200,JsonUtil.json(Map.of("success",true)));
        }catch(IllegalArgumentException e){JsonUtil.send(s,409,JsonUtil.message("error",e.getMessage()));}
        catch(SQLException e){log("Invigilation write failed",e);JsonUtil.send(s,409,JsonUtil.message("error","Duty change could not be saved. A linked record may have changed; refresh and review again."));}
    }
}
