package com.examplanner.dao;

import com.examplanner.config.DbConnection;
import com.examplanner.service.SmtpMailer;
import com.examplanner.util.*;
import java.sql.*;
import java.util.*;

public class NotificationDao {
    public static void user(Connection c,int user,String title,String message,Integer exam) throws SQLException {
        var contact=Sql.one(c,"SELECT COALESCE(a.email,f.email) AS email FROM users u LEFT JOIN admins a ON a.user_id=u.id LEFT JOIN faculty f ON f.user_id=u.id WHERE u.id=?",user);
        create(c,user,(String)contact.get("email"),title,message,exam);
    }
    public static void admins(Connection c,String title,String message,Integer exam) throws SQLException {
        for(var row:Sql.rows(c,"SELECT id FROM users WHERE role='ADMIN'")) user(c,Sql.number(row,"id"),title,message,exam);
    }
    private static void create(Connection c,Integer user,String email,String title,String message,Integer exam) throws SQLException {
        boolean canSend=email!=null&&!email.isBlank()&&SmtpMailer.configured();
        Sql.update(c,"INSERT INTO notifications(recipient_user_id,recipient_email,examination_id,title,message,email_status) VALUES (?,?,?,?,?,?)",user,email,exam,title,message,canSend?"QUEUED":"DISABLED");
    }
    public static void examStatus(Connection c,int exam,String status) throws SQLException {
        if(!Set.of("SCHEDULED","CANCELLED").contains(status)) return;
        var e=Sql.one(c,"SELECT c.course_code,e.exam_date,e.start_time,e.end_time FROM examinations e JOIN courses c ON c.id=e.course_id WHERE e.id=?",exam);
        String title="CANCELLED".equals(status)?"Examination cancelled":"Examination published";
        String message=e.get("course_code")+" on "+e.get("exam_date")+", "+e.get("start_time")+" - "+e.get("end_time")+". "+("CANCELLED".equals(status)?"This examination and its invigilation duties are cancelled.":"Check the examination timetable and your latest seating or duty details.");
        for(var f:Sql.rows(c,"SELECT DISTINCT f.user_id FROM invigilation_assignments a JOIN faculty f ON f.id=a.faculty_id WHERE a.examination_id=? AND f.user_id IS NOT NULL",exam)) user(c,Sql.number(f,"user_id"),title,message,exam);
        for(var student:Sql.rows(c,"SELECT DISTINCT s.email FROM students s JOIN course_enrollments ce ON ce.student_id=s.id JOIN examinations e ON e.course_id=ce.course_id AND e.academic_year_id=ce.academic_year_id WHERE e.id=? AND s.email IS NOT NULL",exam)) create(c,null,(String)student.get("email"),title,message,exam);
    }
    public Map<String,Object> list(int user,boolean admin) throws SQLException {
        try(Connection c=DbConnection.getConnection()) {
            Map<String,Object> result=new LinkedHashMap<>();
            result.put("notifications",Sql.rows(c,"SELECT id,title,message,examination_id AS examId,created_at AS createdAt,read_at AS readAt,email_status AS emailStatus FROM notifications WHERE recipient_user_id=? ORDER BY id DESC LIMIT 300",user));
            result.put("unread",Sql.one(c,"SELECT COUNT(*) AS total FROM notifications WHERE recipient_user_id=? AND read_at IS NULL",user).get("total"));
            result.put("emailConfigured",SmtpMailer.configured());
            if(admin) {
                result.put("outbox",Sql.rows(c,"SELECT id,recipient_email AS email,title,email_status AS emailStatus,email_attempts AS attempts,email_error AS error,created_at AS createdAt,email_sent_at AS sentAt FROM notifications WHERE recipient_email IS NOT NULL ORDER BY id DESC LIMIT 300"));
                result.put("queueCounts",Sql.rows(c,"SELECT email_status AS status,COUNT(*) AS total FROM notifications WHERE recipient_email IS NOT NULL GROUP BY email_status"));
            }
            return result;
        }
    }
    public void read(int user,int id) throws SQLException {
        Sql.write(c -> {if(id==0)Sql.update(c,"UPDATE notifications SET read_at=CURRENT_TIMESTAMP WHERE recipient_user_id=? AND read_at IS NULL",user);
            else if(Sql.update(c,"UPDATE notifications SET read_at=COALESCE(read_at,CURRENT_TIMESTAMP) WHERE id=? AND recipient_user_id=?",id,user)==0) throw new IllegalArgumentException("Notification not found");return null;});
    }
    public void queueExam(int exam,int actor) throws SQLException {
        Sql.write(c -> {var row=Sql.one(c,"SELECT status FROM examinations WHERE id=?",exam);
            if(!Set.of("SCHEDULED","CANCELLED").contains(row.get("status")))throw new IllegalArgumentException("Only published or cancelled exams can send notices");
            examStatus(c,exam,row.get("status").toString());return null;});
    }
    public int retry(int id) throws SQLException {
        if(!SmtpMailer.configured())throw new IllegalArgumentException("Configure SMTP before queuing email");
        return Sql.write(c -> {
            int count=Sql.update(c,"UPDATE notifications SET email_status='QUEUED',email_error=NULL WHERE id=? AND recipient_email IS NOT NULL AND (email_status IN ('FAILED','DISABLED') OR (email_status='SENDING' AND email_claimed_at<DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 30 MINUTE)))",id);
            if(count==0)throw new IllegalArgumentException("This email is not available for retry. In-progress sends can be recovered after 30 minutes.");
            return count;
        });
    }
    public Map<String,Object> sendQueued() throws SQLException {
        if(!SmtpMailer.configured())throw new IllegalArgumentException("Email is not configured. Set SMTP_HOST and SMTP_FROM and restart Tomcat.");
        int sent=0,failed=0;
        // One short claim transaction per email; network I/O never holds the planner lock.
        for(int n=0;n<20;n++) {
            Map<String,Object> item=Sql.write(c -> {
                List<Map<String,Object>> found=Sql.rows(c,"SELECT id,recipient_email,title,message FROM notifications WHERE email_status='QUEUED' ORDER BY id LIMIT 1");
                if(found.isEmpty())return null;
                var row=found.get(0);Sql.update(c,"UPDATE notifications SET email_status='SENDING',email_claimed_at=CURRENT_TIMESTAMP,email_attempts=email_attempts+1 WHERE id=?",row.get("id"));return row;
            });
            if(item==null)break;
            String error=null;
            try {new SmtpMailer().send(item.get("recipient_email").toString(),item.get("title").toString(),item.get("message").toString());sent++;}
            catch(java.io.IOException e) {error=Objects.toString(e.getMessage(),"SMTP delivery failed");failed++;}
            final String failure=error;
            Sql.write(c -> {if(failure==null)Sql.update(c,"UPDATE notifications SET email_status='SENT',email_sent_at=CURRENT_TIMESTAMP,email_error=NULL WHERE id=?",item.get("id"));
                else Sql.update(c,"UPDATE notifications SET email_status='FAILED',email_error=? WHERE id=?",failure.substring(0,Math.min(250,failure.length())),item.get("id"));return null;});
        }
        return Map.of("sent",sent,"failed",failed,"batchLimit",20);
    }
}
