package com.examplanner.servlet.admin;
import com.examplanner.dao.SeatingDao;
import com.examplanner.util.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
@WebServlet("/api/admin/seating")
public class SeatingServlet extends HttpServlet {
    private final SeatingDao dao=new SeatingDao();
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws IOException {
        try {
            int exam=Input.id(r,"examId");
            String action=r.getParameter("action");
            Map<String,Object> data;
            if(action==null) data=dao.chart(exam);
            else data=switch(action) {
                case "preview" -> dao.preview(exam,Input.flag(r,"replaceManual"));
                case "previewAccommodation" -> dao.previewAccommodation(exam,Input.id(r,"studentId"),accommodationFlag(r));
                default -> throw new IllegalArgumentException("Unknown read action");
            };
            JsonUtil.send(s,200,JsonUtil.json(data));
        }
        catch(IllegalArgumentException e) { JsonUtil.send(s,400,JsonUtil.message("error",e.getMessage())); }
        catch(SQLException e) { log("Seating read failed",e); JsonUtil.send(s,500,JsonUtil.message("error","Database error")); }
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException {
        try {
            int exam=Input.id(r,"examId"),actor=(Integer)r.getSession().getAttribute("userId");
            String action=Input.text(r,"action",20),reason=Input.text(r,"reason",250);
            String revision=r.getParameter("expectedRevision");
            Map<String,Object> result=new LinkedHashMap<>();
            result.put("success",true);
            switch(action) {
                case "generate","regenerate" -> result.put("assigned",dao.generate(exam,Input.flag(r,"replaceManual"),Input.flag(r,"confirmReplaceManual"),revision,actor,reason));
                case "swap","reassign" -> dao.override(exam,Input.id(r,"studentId"),Input.id(r,"seatId"),actor,reason,"swap".equals(action),revision);
                case "accommodation" -> result.putAll(dao.accommodation(exam,Input.id(r,"studentId"),accommodationFlag(r),revision,actor,reason));
                case "clear" -> dao.clear(exam,actor,reason,revision);
                default -> throw new IllegalArgumentException("Unknown action");
            }
            JsonUtil.send(s,200,JsonUtil.json(result));
        } catch(IllegalArgumentException e) { JsonUtil.send(s,409,JsonUtil.message("error",e.getMessage())); }
        catch(SQLException e) { log("Seating mutation failed",e); JsonUtil.send(s,500,JsonUtil.message("error","Database error; no changes saved")); }
    }
    private boolean accommodationFlag(HttpServletRequest r) {
        if(!Set.of("true","false","1","0").contains(Objects.toString(r.getParameter("requiresAccessible"),"")))
            throw new IllegalArgumentException("requiresAccessible must be true or false");
        return Input.flag(r,"requiresAccessible");
    }
}
