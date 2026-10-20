package com.examplanner.dao;
import com.examplanner.config.DbConnection;
import com.examplanner.util.*;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.*;
import java.time.*;
import java.util.*;

public class ExamDao {
    public List<Map<String,Object>> list(boolean publicOnly,int year) throws SQLException {
        try(Connection c=DbConnection.getConnection()) {
            List<Map<String,Object>> exams=Sql.rows(c,"SELECT e.id,e.course_id AS courseId,c.course_code AS courseCode,c.course_name AS courseName,e.academic_year_id AS academicYearId,y.label AS academicYear,e.exam_date AS examDate,e.start_time AS startTime,e.end_time AS endTime,e.status,e.spacing_required AS spacingRequired FROM examinations e JOIN courses c ON c.id=e.course_id JOIN academic_years y ON y.id=e.academic_year_id WHERE (?=0 OR e.academic_year_id=?) "+(publicOnly?"AND e.status IN ('SCHEDULED','COMPLETED') ":"")+"ORDER BY e.exam_date,e.start_time,c.course_code",year,year);
            for(Map<String,Object> e:exams) { List<Map<String,Object>> rooms=Sql.rows(c,"SELECT r.id,r.room_number AS roomNumber,r.capacity FROM examination_rooms er JOIN rooms r ON r.id=er.room_id WHERE er.examination_id=? ORDER BY r.room_number",e.get("id")); e.put("rooms",rooms); }
            return exams;
        }
    }
    public int save(HttpServletRequest r) throws SQLException {
        String action=Input.text(r,"action",20);
        if(!Set.of("create","update","status","delete").contains(action)) throw new IllegalArgumentException("Unknown exam action");
        int id="create".equals(action)?0:Input.id(r,"id");
        return Sql.write(c -> {
            if("delete".equals(action)) {
                Map<String,Object> old=Sql.one(c,"SELECT status FROM examinations WHERE id=?",id);
                if(!Set.of("DRAFT","CANCELLED").contains(old.get("status"))) throw new IllegalArgumentException("Only draft or cancelled exams can be deleted");
                if(!Sql.rows(c,"SELECT id FROM seating_history WHERE examination_id=?",id).isEmpty()) throw new IllegalArgumentException("Exam has seating history; cancel and retain its audit record");
                Sql.update(c,"DELETE FROM examinations WHERE id=?",id); return id;
            }
            if("status".equals(action)) {
                Map<String,Object> old=Sql.one(c,"SELECT * FROM examinations WHERE id=?",id);
                String next=Input.text(r,"status",20),current=old.get("status").toString();
                if(!allowedTransition(current,next)) throw new IllegalArgumentException("Invalid state transition from "+current+" to "+next);
                if("SCHEDULED".equals(next)) validateConflicts(c,id,Sql.number(old,"course_id"),Sql.number(old,"academic_year_id"),old.get("exam_date"),old.get("start_time"),old.get("end_time"),roomIds(c,id));
                Sql.update(c,"UPDATE examinations SET status=? WHERE id=?",next,id);
                if(!current.equals(next)) NotificationDao.examStatus(c,id,next);
                return id;
            }
            int course=Input.id(r,"courseId"),year=Input.id(r,"academicYearId");
            LocalDate date; LocalTime start,end;
            try { date=LocalDate.parse(Input.text(r,"examDate",10)); start=LocalTime.parse(Input.text(r,"startTime",8)); end=LocalTime.parse(Input.text(r,"endTime",8)); }
            catch(java.time.format.DateTimeParseException e) { throw new IllegalArgumentException("Invalid date or time"); }
            String status=Input.text(r,"status",20);
            if(!end.isAfter(start)||!Set.of("DRAFT","SCHEDULED").contains(status)) throw new IllegalArgumentException("End time must be later than start time; select draft or scheduled");
            Sql.one(c,"SELECT course_id FROM course_offerings WHERE course_id=? AND academic_year_id=?",course,year);
            List<Integer> rooms=new ArrayList<>(); String[] values=r.getParameterValues("roomId");
            if(values!=null) for(String value:values) { try { int room=Integer.parseInt(value); if(room<1||rooms.contains(room)) throw new NumberFormatException(); Sql.one(c,"SELECT id FROM rooms WHERE id=?",room); rooms.add(room); } catch(NumberFormatException e) { throw new IllegalArgumentException("Invalid or duplicate room id"); } }
            if(rooms.isEmpty()) throw new IllegalArgumentException("Select at least one room");
            if(id>0) {
                Map<String,Object> old=Sql.one(c,"SELECT status FROM examinations WHERE id=?",id);
                if(!"DRAFT".equals(old.get("status"))) throw new IllegalArgumentException("Only draft exams can be edited");
                if(!Sql.rows(c,"SELECT id FROM seating_history WHERE examination_id=?",id).isEmpty()) throw new IllegalArgumentException("Exam has seating history; retain it and create a new exam if its inputs change");
            }
            validateConflicts(c,id,course,year,date,start,end,rooms);
            Object[] args={course,year,date.toString(),start.toString(),end.toString(),status,Input.flag(r,"spacingRequired")};
            int saved;
            if(id==0) saved=Sql.insert(c,"INSERT INTO examinations(course_id,academic_year_id,exam_date,start_time,end_time,status,spacing_required) VALUES (?,?,?,?,?,?,?)",args);
            else { Object[] update=Arrays.copyOf(args,8); update[7]=id; Sql.update(c,"UPDATE examinations SET course_id=?,academic_year_id=?,exam_date=?,start_time=?,end_time=?,status=?,spacing_required=? WHERE id=?",update); Sql.update(c,"DELETE FROM examination_rooms WHERE examination_id=?",id); saved=id; }
            for(int room:rooms) Sql.update(c,"INSERT INTO examination_rooms VALUES (?,?)",saved,room);
            if("SCHEDULED".equals(status)) NotificationDao.examStatus(c,saved,status);
            return saved;
        });
    }
    private List<Integer> roomIds(Connection c,int exam) throws SQLException { List<Integer> ids=new ArrayList<>(); for(Map<String,Object> r:Sql.rows(c,"SELECT room_id FROM examination_rooms WHERE examination_id=?",exam)) ids.add(Sql.number(r,"room_id")); return ids; }
    private void validateConflicts(Connection c,int exclude,int course,int year,Object date,Object start,Object end,List<Integer> rooms) throws SQLException {
        for(int room:rooms) if(!Sql.rows(c,"SELECT e.id FROM examinations e JOIN examination_rooms er ON er.examination_id=e.id WHERE e.id<>? AND er.room_id=? AND e.exam_date=? AND e.status<>'CANCELLED' AND e.start_time<? AND e.end_time>?",exclude,room,date.toString(),end.toString(),start.toString()).isEmpty()) throw new IllegalArgumentException("Timetable conflict: selected room overlaps another exam");
        if(!Sql.rows(c,"SELECT DISTINCT e.id FROM examinations e JOIN course_enrollments existing ON existing.course_id=e.course_id AND existing.academic_year_id=e.academic_year_id JOIN course_enrollments incoming ON incoming.student_id=existing.student_id AND incoming.course_id=? AND incoming.academic_year_id=? WHERE e.id<>? AND e.exam_date=? AND e.status<>'CANCELLED' AND e.start_time<? AND e.end_time>?",course,year,exclude,date.toString(),end.toString(),start.toString()).isEmpty()) throw new IllegalArgumentException("Timetable conflict: shared student overlaps another exam");
    }
    public boolean allowedTransition(String current,String next) { if(current.equals(next)) return true; return switch(current) { case "DRAFT" -> Set.of("SCHEDULED","CANCELLED").contains(next); case "SCHEDULED" -> Set.of("COMPLETED","CANCELLED").contains(next); default -> false; }; }
}
