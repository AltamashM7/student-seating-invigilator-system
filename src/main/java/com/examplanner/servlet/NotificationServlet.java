package com.examplanner.servlet;
import com.examplanner.dao.NotificationDao;
import com.examplanner.util.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

@WebServlet({"/api/admin/notifications","/api/faculty/notifications"})
public class NotificationServlet extends HttpServlet {
    private final NotificationDao dao=new NotificationDao();
    protected void doGet(HttpServletRequest r,HttpServletResponse s) throws IOException {
        try {JsonUtil.send(s,200,JsonUtil.json(dao.list((Integer)r.getSession().getAttribute("userId"),"ADMIN".equals(r.getSession().getAttribute("role")))));}
        catch(SQLException e){log("Notifications read failed",e);JsonUtil.send(s,500,JsonUtil.message("error","Notifications could not be loaded."));}
    }
    protected void doPost(HttpServletRequest r,HttpServletResponse s) throws IOException {
        try {
            int user=(Integer)r.getSession().getAttribute("userId");String action=Input.text(r,"action",20);Object result=Map.of("success",true);
            if("read".equals(action)||"readAll".equals(action))dao.read(user,"readAll".equals(action)?0:Input.id(r,"id"));
            else {
                if(!"ADMIN".equals(r.getSession().getAttribute("role"))){JsonUtil.send(s,403,JsonUtil.message("error","Administrator access required"));return;}
                switch(action){case "sendEmail" -> result=dao.sendQueued();case "retry" -> dao.retry(Input.id(r,"id"));case "publishExam" -> dao.queueExam(Input.id(r,"examId"),user);default -> throw new IllegalArgumentException("Unknown notification action");}
            }
            JsonUtil.send(s,200,JsonUtil.json(result));
        }catch(IllegalArgumentException e){JsonUtil.send(s,400,JsonUtil.message("error",e.getMessage()));}
        catch(SQLException e){log("Notifications write failed",e);JsonUtil.send(s,500,JsonUtil.message("error","Notification change could not be saved."));}
    }
}
