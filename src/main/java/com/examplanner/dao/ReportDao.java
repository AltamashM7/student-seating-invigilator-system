package com.examplanner.dao;

import com.examplanner.config.DbConnection;
import com.examplanner.util.Sql;
import java.sql.*;
import java.util.*;

public class ReportDao {
    public record Report(String title,List<String> headings,List<List<String>> rows,String scope) {}
    public Report report(String type,int year,int exam,int room,int user,boolean facultyOnly) throws SQLException {
        try(Connection c=DbConnection.getConnection()) {
            if(year==0) year=Sql.number(Sql.one(c,"SELECT id FROM academic_years ORDER BY is_current DESC,start_year DESC LIMIT 1"),"id");
            String label=Sql.one(c,"SELECT label FROM academic_years WHERE id=?",year).get("label").toString();
            if(facultyOnly&&!"faculty-duties".equals(type))throw new IllegalArgumentException("Faculty may download only their own duty report");
            String query;List<String> headings,keys;String title;List<Object> values=new ArrayList<>();
            String filter=" WHERE e.academic_year_id=? AND (?=0 OR e.id=?)";values.add(year);values.add(exam);values.add(exam);
            switch(type) {
                case "room-seating","student-seating" -> {
                    title="room-seating".equals(type)?"Room-wise seating list":"Student-wise seating list";
                    headings=List.of("Date","Time","Course","Room","Seat","Roll number","Student","Access required");
                    keys=List.of("examDate","examTime","courseCode","roomNumber","seatNumber","rollNumber","name","accommodation");
                    query="SELECT e.exam_date AS examDate,CONCAT(TIME_FORMAT(e.start_time,'%H:%i'),' - ',TIME_FORMAT(e.end_time,'%H:%i')) AS examTime,c.course_code AS courseCode,r.room_number AS roomNumber,s.seat_number AS seatNumber,st.roll_number AS rollNumber,st.name,CASE WHEN st.requires_accessible THEN 'Yes' ELSE 'No' END AS accommodation FROM seating_assignments a JOIN examinations e ON e.id=a.examination_id JOIN courses c ON c.id=e.course_id JOIN seats s ON s.id=a.seat_id JOIN rooms r ON r.id=s.room_id JOIN students st ON st.id=a.student_id"+filter+" AND (?=0 OR r.id=?) ORDER BY "+("room-seating".equals(type)?"e.exam_date,e.start_time,r.room_number,s.seat_row,s.seat_column":"st.roll_number,e.exam_date,e.start_time");values.add(room);values.add(room);
                }
                case "faculty-duties" -> {
                    title="Faculty invigilation duties";
                    headings=List.of("Faculty","Employee code","Date","Time","Course","Room","Slot","Duty status","Exam status");
                    keys=List.of("name","employeeCode","examDate","examTime","courseCode","roomNumber","slotNumber","status","examStatus");
                    query="SELECT f.name,f.employee_code AS employeeCode,e.exam_date AS examDate,CONCAT(TIME_FORMAT(e.start_time,'%H:%i'),' - ',TIME_FORMAT(e.end_time,'%H:%i')) AS examTime,c.course_code AS courseCode,r.room_number AS roomNumber,a.slot_number AS slotNumber,a.status,e.status AS examStatus FROM invigilation_assignments a JOIN faculty f ON f.id=a.faculty_id JOIN examinations e ON e.id=a.examination_id JOIN courses c ON c.id=e.course_id JOIN rooms r ON r.id=a.room_id"+filter;
                    if(facultyOnly){query+=" AND f.user_id=? AND e.status<>'DRAFT'";values.add(user);}
                    query+=" ORDER BY f.employee_code,e.exam_date,e.start_time,r.room_number";
                }
                case "exam-schedule" -> {
                    title="Examination schedule";
                    headings=List.of("Date","Time","Course","Subject","Room","Capacity","Status");keys=List.of("examDate","examTime","courseCode","courseName","roomNumber","capacity","status");
                    query="SELECT e.exam_date AS examDate,CONCAT(TIME_FORMAT(e.start_time,'%H:%i'),' - ',TIME_FORMAT(e.end_time,'%H:%i')) AS examTime,c.course_code AS courseCode,c.course_name AS courseName,r.room_number AS roomNumber,r.capacity,e.status FROM examinations e JOIN courses c ON c.id=e.course_id JOIN examination_rooms er ON er.examination_id=e.id JOIN rooms r ON r.id=er.room_id"+filter+" AND (?=0 OR r.id=?) ORDER BY e.exam_date,e.start_time,c.course_code,r.room_number";values.add(room);values.add(room);
                }
                default -> throw new IllegalArgumentException("Unknown report type");
            }
            List<List<String>> rows=new ArrayList<>();for(var row:Sql.rows(c,query,values.toArray())){List<String> cells=new ArrayList<>();for(String key:keys)cells.add(row.get(key)==null?"":row.get(key).toString());rows.add(cells);}
            return new Report(title,headings,rows,"Academic year "+label+(exam==0?"":" / exam "+exam)+(room==0?"":" / room "+room)+" / "+rows.size()+" records");
        }
    }
}
