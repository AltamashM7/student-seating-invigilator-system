package com.examplanner.dao;

import com.examplanner.config.DbConnection;
import com.examplanner.util.*;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.*;
import java.util.*;

public class AcademicDao {
    public Map<String,Object> snapshot(int yearId) throws SQLException {
        try(Connection c=DbConnection.getConnection()) {
            if(yearId==0) yearId=Sql.number(Sql.one(c,"SELECT id FROM academic_years WHERE is_current=TRUE LIMIT 1"),"id");
            Map<String,Object> data=new LinkedHashMap<>();
            data.put("years",Sql.rows(c,"SELECT id,label,start_year AS startYear,is_current AS isCurrent FROM academic_years ORDER BY start_year DESC"));
            data.put("selectedYearId",yearId);
            data.put("departments",Sql.rows(c,"SELECT id,code,name FROM departments ORDER BY code"));
            data.put("divisions",Sql.rows(c,"SELECT d.id,d.academic_year_id AS academicYearId,d.department_id AS departmentId,p.code AS departmentCode,d.study_year AS studyYear,d.code FROM divisions d JOIN departments p ON p.id=d.department_id ORDER BY d.academic_year_id,d.department_id,d.study_year,d.code"));
            data.put("courses",Sql.rows(c,"SELECT c.id,c.course_code AS code,c.course_name AS name,c.department_id AS departmentId,d.code AS departmentCode,c.semester,c.study_year AS studyYear,EXISTS(SELECT 1 FROM course_offerings o WHERE o.course_id=c.id AND o.academic_year_id=?) AS offered FROM courses c JOIN departments d ON d.id=c.department_id ORDER BY c.course_code",yearId));
            data.put("rooms",Sql.rows(c,"SELECT id,room_number AS roomNumber,capacity,seat_columns AS seatColumns,is_accessible AS isAccessible FROM rooms ORDER BY room_number"));
            data.put("seats",Sql.rows(c,"SELECT id,room_id AS roomId,seat_number AS seatNumber,is_accessible AS isAccessible FROM seats ORDER BY room_id,seat_row,seat_column"));
            data.put("students",Sql.rows(c,"SELECT s.id,s.roll_number AS rollNumber,s.name,s.email,s.requires_accessible AS requiresAccessible,a.division_id AS divisionId,a.semester,d.department_id AS departmentId,d.study_year AS studyYear,d.code AS divisionCode,p.code AS departmentCode FROM students s LEFT JOIN student_academic_enrollments a ON a.student_id=s.id AND a.academic_year_id=? LEFT JOIN divisions d ON d.id=a.division_id LEFT JOIN departments p ON p.id=d.department_id ORDER BY s.roll_number",yearId));
            data.put("enrollments",Sql.rows(c,"SELECT id,student_id AS studentId,course_id AS courseId FROM course_enrollments WHERE academic_year_id=? ORDER BY student_id,course_id",yearId));
            data.put("faculty",Sql.rows(c,"SELECT f.id,f.user_id AS userId,f.employee_code AS employeeCode,f.name,f.email,f.department_id AS departmentId,u.username FROM faculty f LEFT JOIN users u ON u.id=f.user_id ORDER BY f.employee_code"));
            data.put("counts",Sql.rows(c,"SELECT d.id,d.department_id AS departmentId,p.code AS department,d.study_year AS studyYear,d.code AS division,COUNT(a.student_id) AS studentCount FROM divisions d JOIN departments p ON p.id=d.department_id LEFT JOIN student_academic_enrollments a ON a.division_id=d.id AND a.academic_year_id=d.academic_year_id WHERE d.academic_year_id=? GROUP BY d.id,d.department_id,p.code,d.study_year,d.code ORDER BY p.code,d.study_year,d.code",yearId));
            data.put("yearCounts",Sql.rows(c,"SELECT p.code AS department,sy.study_year AS studyYear,COUNT(a.student_id) AS studentCount FROM departments p CROSS JOIN (SELECT 1 AS study_year UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) sy LEFT JOIN divisions d ON d.department_id=p.id AND d.study_year=sy.study_year AND d.academic_year_id=? LEFT JOIN student_academic_enrollments a ON a.division_id=d.id AND a.academic_year_id=d.academic_year_id GROUP BY p.id,p.code,sy.study_year ORDER BY p.code,sy.study_year",yearId));
            return data;
        }
    }

    public int save(HttpServletRequest r,int actor) throws SQLException {
        String resource=Input.text(r,"resource",30), action=Input.text(r,"action",20);
        if(!Set.of("create","update","delete","offer","unoffer").contains(action)) throw new IllegalArgumentException("Unknown action");
        int id="create".equals(action)?0:Input.id(r,"id");
        return Sql.write(c -> {
            if("delete".equals(action)) { delete(c,resource,id,actor); return id; }
            return switch(resource) {
                case "years" -> year(c,r,id);
                case "departments" -> simple(c,"departments",id,new String[]{"code","name"},new Object[]{Input.text(r,"code",20),Input.text(r,"name",100)});
                case "divisions" -> division(c,r,id);
                case "courses" -> course(c,r,id,action);
                case "students" -> student(c,r,id);
                case "faculty" -> faculty(c,r,id);
                case "rooms" -> room(c,r,id);
                case "seats" -> { if(id==0) throw new IllegalArgumentException("Seats are generated from room capacity"); Map<String,Object> seat=Sql.one(c,"SELECT room_id FROM seats WHERE id=?",id); if(!Sql.rows(c,"SELECT examination_id FROM examination_rooms WHERE room_id=?",seat.get("room_id")).isEmpty()) throw new IllegalArgumentException("Room is already assigned; accessibility is protected"); Sql.update(c,"UPDATE seats SET is_accessible=? WHERE id=?",Input.flag(r,"isAccessible"),id); yield id; }
                case "enrollments" -> enrollment(c,r,id);
                default -> throw new IllegalArgumentException("Unknown resource");
            };
        });
    }
    private int year(Connection c,HttpServletRequest r,int id) throws SQLException {
        int start=Input.integer(r,"startYear",2000,2100);
        boolean current=Input.flag(r,"isCurrent");
        if(id>0 && !current && Sql.flag(Sql.one(c,"SELECT is_current FROM academic_years WHERE id=?",id),"is_current")) throw new IllegalArgumentException("Select another current year before clearing this one");
        if(current) Sql.update(c,"UPDATE academic_years SET is_current=FALSE");
        return simple(c,"academic_years",id,new String[]{"label","start_year","is_current"},new Object[]{start+"-"+String.format("%02d",(start+1)%100),start,current});
    }
    private int course(Connection c,HttpServletRequest r,int id,String action) throws SQLException {
        int year=Input.id(r,"academicYearId");
        if("unoffer".equals(action)) {
            Sql.update(c,"DELETE FROM course_offerings WHERE course_id=? AND academic_year_id=?",id,year); return id;
        }
        if(!"offer".equals(action)) {
            int semester=Input.integer(r,"semester",1,8),studyYear=Input.integer(r,"studyYear",1,4);
            if(studyYear!=(semester+1)/2) throw new IllegalArgumentException("Study year and semester must agree");
            if(id>0) { Map<String,Object> old=Sql.one(c,"SELECT department_id,semester FROM courses WHERE id=?",id); if(Sql.number(old,"semester")!=semester || Sql.number(old,"department_id")!=Input.id(r,"departmentId")) protectCourse(c,id); }
            id=simple(c,"courses",id,new String[]{"course_code","course_name","department_id","semester","study_year"},new Object[]{Input.text(r,"code",30),Input.text(r,"name",120),Input.id(r,"departmentId"),semester,studyYear});
        }
        Sql.update(c,"INSERT IGNORE INTO course_offerings VALUES (?,?)",id,year);
        return id;
    }
    private int division(Connection c,HttpServletRequest r,int id) throws SQLException {
        int year=Input.id(r,"academicYearId"),department=Input.id(r,"departmentId"),study=Input.integer(r,"studyYear",1,4);
        if(id>0) { Map<String,Object> old=Sql.one(c,"SELECT * FROM divisions WHERE id=?",id); if((Sql.number(old,"academic_year_id")!=year||Sql.number(old,"department_id")!=department||Sql.number(old,"study_year")!=study) && !Sql.rows(c,"SELECT student_id FROM student_academic_enrollments WHERE division_id=?",id).isEmpty()) throw new IllegalArgumentException("Division placement fields are protected while students are enrolled"); }
        return simple(c,"divisions",id,new String[]{"academic_year_id","department_id","study_year","code"},new Object[]{year,department,study,Input.text(r,"code",20)});
    }
    private void protectCourse(Connection c,int id) throws SQLException {
        if(!Sql.rows(c,"SELECT e.id FROM examinations e WHERE course_id=? AND (status IN ('SCHEDULED','COMPLETED') OR EXISTS(SELECT 1 FROM seating_history h WHERE h.examination_id=e.id))",id).isEmpty()) throw new IllegalArgumentException("Course used by a published exam or seating history cannot be changed");
        if(!Sql.rows(c,"SELECT id FROM course_enrollments WHERE course_id=?",id).isEmpty()) throw new IllegalArgumentException("Remove enrollments before changing this course; its name/code can be managed before use");
    }
    private int student(Connection c,HttpServletRequest r,int id) throws SQLException {
        int year=Input.id(r,"academicYearId"),division=Input.id(r,"divisionId"),semester=Input.integer(r,"semester",1,8);
        Map<String,Object> group=Sql.one(c,"SELECT * FROM divisions WHERE id=?",division);
        if(Sql.number(group,"academic_year_id")!=year || Sql.number(group,"study_year")!=(semester+1)/2) throw new IllegalArgumentException("Division, academic year and semester must agree");
        String email=r.getParameter("email");
        if(email!=null) email=email.trim();
        if(email!=null && !email.isEmpty() && (!ValidationUtil.validEmail(email)||email.length()>120)) throw new IllegalArgumentException("Invalid email");
        if(id>0) protectStudent(c,id,year);
        if(id>0 && Sql.flag(Sql.one(c,"SELECT requires_accessible FROM students WHERE id=?",id),"requires_accessible")!=Input.flag(r,"requiresAccessible")
                && !Sql.rows(c,"SELECT a.id FROM seating_assignments a JOIN examinations e ON e.id=a.examination_id WHERE a.student_id=? AND e.status IN ('DRAFT','SCHEDULED')",id).isEmpty())
            throw new IllegalArgumentException("Accessibility needs are protected while this student has active seating in any academic year");
        if(id>0 && !Sql.rows(c,"SELECT ce.id FROM course_enrollments ce JOIN courses co ON co.id=ce.course_id WHERE ce.student_id=? AND ce.academic_year_id=? AND co.semester<>?",id,year,semester).isEmpty()) throw new IllegalArgumentException("Remove incompatible course enrollments before changing semester");
        id=simple(c,"students",id,new String[]{"roll_number","name","email","department_id","semester","requires_accessible"},new Object[]{Input.text(r,"rollNumber",30),Input.text(r,"name",100),email==null||email.isEmpty()?null:email,Sql.number(group,"department_id"),semester,Input.flag(r,"requiresAccessible")});
        Sql.update(c,"INSERT INTO student_academic_enrollments VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE division_id=VALUES(division_id),semester=VALUES(semester)",id,year,division,semester);
        return id;
    }
    private void protectStudent(Connection c,int id,int year) throws SQLException {
        if(!Sql.rows(c,"SELECT a.id FROM seating_assignments a JOIN examinations e ON e.id=a.examination_id WHERE a.student_id=? AND e.academic_year_id=? AND e.status IN ('DRAFT','SCHEDULED')",id,year).isEmpty()) throw new IllegalArgumentException("Student has active seating assignments; clear draft seating before editing");
        if(!Sql.rows(c,"SELECT e.id FROM examinations e JOIN course_enrollments ce ON ce.course_id=e.course_id AND ce.academic_year_id=e.academic_year_id WHERE ce.student_id=? AND e.academic_year_id=? AND e.status IN ('SCHEDULED','COMPLETED')",id,year).isEmpty()) throw new IllegalArgumentException("Student is enrolled in a published exam; placement is protected");
    }
    private int enrollment(Connection c,HttpServletRequest r,int id) throws SQLException {
        int student=Input.id(r,"studentId"),course=Input.id(r,"courseId"),year=Input.id(r,"academicYearId");
        protectEnrollment(c,course,year);
        Map<String,Object> placement=Sql.one(c,"SELECT semester FROM student_academic_enrollments WHERE student_id=? AND academic_year_id=?",student,year);
        Map<String,Object> subject=Sql.one(c,"SELECT semester FROM courses WHERE id=?",course);
        if(Sql.number(placement,"semester")!=Sql.number(subject,"semester")) throw new IllegalArgumentException("Student and course semesters must agree");
        validateNewEnrollment(c,student,course,year);
        if(id>0) { Map<String,Object> old=Sql.one(c,"SELECT * FROM course_enrollments WHERE id=?",id); protectEnrollment(c,Sql.number(old,"course_id"),Sql.number(old,"academic_year_id")); }
        return simple(c,"course_enrollments",id,new String[]{"student_id","course_id","academic_year_id"},new Object[]{student,course,year});
    }
    public static void protectEnrollment(Connection c,int course,int year) throws SQLException {
        if(!Sql.rows(c,"SELECT e.id FROM examinations e WHERE course_id=? AND academic_year_id=? AND (e.status IN ('SCHEDULED','COMPLETED') OR EXISTS(SELECT 1 FROM seating_assignments a WHERE a.examination_id=e.id))",course,year).isEmpty()) throw new IllegalArgumentException("Enrollments are protected after seating generation or exam publication");
    }
    public static void validateNewEnrollment(Connection c,int student,int course,int year) throws SQLException {
        if(!Sql.rows(c,"SELECT e.id FROM examinations e JOIN examinations other ON other.exam_date=e.exam_date AND other.start_time<e.end_time AND other.end_time>e.start_time AND other.id<>e.id JOIN course_enrollments ce ON ce.course_id=other.course_id AND ce.academic_year_id=other.academic_year_id WHERE e.course_id=? AND e.academic_year_id=? AND ce.student_id=? AND e.status<>'CANCELLED' AND other.status<>'CANCELLED'",course,year,student).isEmpty()) throw new IllegalArgumentException("Enrollment would introduce overlapping exams for this student");
    }
    private int faculty(Connection c,HttpServletRequest r,int id) throws SQLException {
        String email=Input.text(r,"email",120);
        if(!ValidationUtil.validEmail(email)) throw new IllegalArgumentException("Invalid email");
        String username=Input.text(r,"username",50),password=r.getParameter("password");
        int userId;
        if(id==0) {
            if(!ValidationUtil.validPassword(password)) throw new IllegalArgumentException("Password needs 8 characters, uppercase, lowercase and a digit");
            userId=Sql.insert(c,"INSERT INTO users(username,password_hash,role) VALUES (?,?,'FACULTY')",username,PasswordUtil.hash(password));
        } else {
            Map<String,Object> f=Sql.one(c,"SELECT user_id FROM faculty WHERE id=?",id);
            if(f.get("user_id")==null) throw new IllegalArgumentException("Faculty account is missing");
            userId=Sql.number(f,"user_id"); Sql.update(c,"UPDATE users SET username=? WHERE id=?",username,userId);
            if(password!=null&&!password.isBlank()) { if(!ValidationUtil.validPassword(password)) throw new IllegalArgumentException("Invalid password"); Sql.update(c,"UPDATE users SET password_hash=? WHERE id=?",PasswordUtil.hash(password),userId); }
        }
        return simple(c,"faculty",id,new String[]{"user_id","employee_code","name","email","department_id"},new Object[]{userId,Input.text(r,"employeeCode",30),Input.text(r,"name",100),email,Input.id(r,"departmentId")});
    }
    private int room(Connection c,HttpServletRequest r,int id) throws SQLException {
        int capacity=Input.integer(r,"capacity",1,500),columns=Input.integer(r,"seatColumns",1,30);
        if(id>0 && !Sql.rows(c,"SELECT examination_id FROM examination_rooms WHERE room_id=?",id).isEmpty()) throw new IllegalArgumentException("Room is assigned to exams; its layout is protected");
        id=simple(c,"rooms",id,new String[]{"room_number","capacity","seat_columns","is_accessible"},new Object[]{Input.text(r,"roomNumber",30),capacity,columns,Input.flag(r,"isAccessible")});
        Sql.update(c,"DELETE FROM seats WHERE room_id=?",id);
        for(int i=1;i<=capacity;i++) Sql.update(c,"INSERT INTO seats(room_id,seat_number,seat_row,seat_column,is_accessible) VALUES (?,?,?,?,?)",id,String.format("S%03d",i),(i-1)/columns+1,(i-1)%columns+1,Input.flag(r,"isAccessible")&&i<=2);
        return id;
    }
    private void delete(Connection c,String resource,int id,int actor) throws SQLException {
        switch(resource) {
            case "years" -> { if(Sql.flag(Sql.one(c,"SELECT is_current FROM academic_years WHERE id=?",id),"is_current")) throw new IllegalArgumentException("Cannot delete the current academic year"); requiredDelete(c,"academic_years",id); }
            case "departments" -> requiredDelete(c,"departments",id);
            case "divisions" -> requiredDelete(c,"divisions",id);
            case "courses" -> { protectCourse(c,id); Sql.update(c,"DELETE FROM course_offerings WHERE course_id=?",id); requiredDelete(c,"courses",id); }
            case "rooms" -> requiredDelete(c,"rooms",id);
            case "enrollments" -> { Map<String,Object> e=Sql.one(c,"SELECT * FROM course_enrollments WHERE id=?",id); protectEnrollment(c,Sql.number(e,"course_id"),Sql.number(e,"academic_year_id")); requiredDelete(c,"course_enrollments",id); }
            case "students" -> { if(!Sql.rows(c,"SELECT id FROM seating_history WHERE student_id=?",id).isEmpty()) throw new IllegalArgumentException("Student has seating history"); for(Map<String,Object> e:Sql.rows(c,"SELECT * FROM course_enrollments WHERE student_id=?",id)) protectEnrollment(c,Sql.number(e,"course_id"),Sql.number(e,"academic_year_id")); Sql.update(c,"DELETE FROM course_enrollments WHERE student_id=?",id); Sql.update(c,"DELETE FROM student_academic_enrollments WHERE student_id=?",id); requiredDelete(c,"students",id); }
            case "faculty" -> { Map<String,Object> f=Sql.one(c,"SELECT user_id FROM faculty WHERE id=?",id); requiredDelete(c,"faculty",id); if(f.get("user_id")!=null) requiredDelete(c,"users",Sql.number(f,"user_id")); }
            default -> throw new IllegalArgumentException("Unknown resource");
        }
    }
    private void requiredDelete(Connection c,String table,int id) throws SQLException { if(Sql.update(c,"DELETE FROM "+table+" WHERE id=?",id)==0) throw new IllegalArgumentException("Record not found"); }
    private int simple(Connection c,String table,int id,String[] columns,Object[] values) throws SQLException {
        if(id==0) return Sql.insert(c,"INSERT INTO "+table+"("+String.join(",",columns)+") VALUES ("+String.join(",",Collections.nCopies(columns.length,"?"))+")",values);
        Object[] args=Arrays.copyOf(values,values.length+1); args[values.length]=id;
        StringJoiner set=new StringJoiner(","); for(String column:columns) set.add(column+"=?");
        Sql.one(c,"SELECT id FROM "+table+" WHERE id=?",id);
        Sql.update(c,"UPDATE "+table+" SET "+set+" WHERE id=?",args); return id;
    }
}
