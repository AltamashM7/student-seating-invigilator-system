package com.examplanner.dao;

import com.examplanner.service.InvigilationAllocator;
import com.examplanner.service.InvigilationAllocator.*;
import com.examplanner.util.*;
import java.sql.*;
import java.time.*;
import java.util.*;

public class ConflictDao {
    private final InvigilationDao duties=new InvigilationDao();
    private final InvigilationAllocator allocator=new InvigilationAllocator();
    public Map<String,Object> scan(int year) throws SQLException {return Sql.write(c -> scan(c,year));}
    private Map<String,Object> scan(Connection c,int year) throws SQLException {
        var planning=duties.snapshot(c,year);int selected=Sql.number(planning,"selectedYearId");
        var exams=InvigilationDao.rows(planning,"exams");Rules rules=InvigilationDao.rules(planning);
        var enrollments=Sql.rows(c,"SELECT ce.student_id AS studentId,ce.course_id AS courseId,ce.academic_year_id AS academicYearId,s.requires_accessible AS requiresAccessible FROM course_enrollments ce JOIN students s ON s.id=ce.student_id");
        var seats=Sql.rows(c,"SELECT a.examination_id AS examId,a.student_id AS studentId,a.seat_id AS seatId,s.room_id AS roomId,s.seat_column AS seatColumn,(s.is_accessible AND r.is_accessible) AS accessible,st.requires_accessible AS requiresAccessible FROM seating_assignments a JOIN seats s ON s.id=a.seat_id JOIN rooms r ON r.id=s.room_id JOIN students st ON st.id=a.student_id ORDER BY a.examination_id,a.id");
        var rooms=Sql.rows(c,"SELECT er.examination_id AS examId,r.id AS roomId,r.room_number AS roomNumber,r.capacity,COUNT(CASE WHEN NOT e.spacing_required OR MOD(s.seat_column,2)=1 THEN s.id END) AS usable,COUNT(CASE WHEN (NOT e.spacing_required OR MOD(s.seat_column,2)=1) AND s.is_accessible AND r.is_accessible THEN s.id END) AS accessible FROM examination_rooms er JOIN examinations e ON e.id=er.examination_id JOIN rooms r ON r.id=er.room_id LEFT JOIN seats s ON s.room_id=r.id GROUP BY er.examination_id,r.id,r.room_number,r.capacity ORDER BY er.examination_id,r.id");
        var spacing=Sql.rows(c,"SELECT id,spacing_required AS spaced FROM examinations");
        List<Map<String,Object>> conflicts=new ArrayList<>(),readiness=new ArrayList<>();
        Map<Integer,Set<Integer>> rosters=new HashMap<>(),roomIds=new HashMap<>();
        for(var exam:exams) {
            int id=Sql.number(exam,"id");Set<Integer> roster=new HashSet<>(),assignedStudents=new HashSet<>(),assignedSeats=new HashSet<>(),examRooms=new HashSet<>();int accessibleNeed=0;
            for(var row:enrollments)if(Sql.number(row,"courseId")==Sql.number(exam,"courseId")&&Sql.number(row,"academicYearId")==Sql.number(exam,"academicYearId")){roster.add(Sql.number(row,"studentId"));if(Sql.flag(row,"requiresAccessible"))accessibleNeed++;}
            rosters.put(id,roster);int usable=0,accessible=0;
            for(var room:rooms)if(Sql.number(room,"examId")==id){examRooms.add(Sql.number(room,"roomId"));usable+=Sql.number(room,"usable");accessible+=Sql.number(room,"accessible");}
            roomIds.put(id,examRooms);
            boolean relevant=Sql.number(exam,"academicYearId")==selected&&Set.of("DRAFT","SCHEDULED").contains(exam.get("status"));
            if(!relevant)continue;
            if(roster.isEmpty())add(conflicts,exam,"NO_STUDENTS","ERROR","No enrolled students","Add the course enrollments before generating seating.","enrollments.html");
            if(examRooms.isEmpty())add(conflicts,exam,"NO_ROOMS","ERROR","No examination rooms","Assign rooms on the Exams page.","exams.html");
            if(roster.size()>usable)add(conflicts,exam,"SEAT_CAPACITY","ERROR","Insufficient usable seats",roster.size()+" students need "+usable+" available seats after spacing.","seating.html");
            if(accessibleNeed>accessible)add(conflicts,exam,"ACCESSIBLE_CAPACITY","ERROR","Insufficient accessible seats",accessibleNeed+" students require accessible seating; "+accessible+" seats qualify.","seating.html");
            boolean spaced=spacing.stream().anyMatch(s -> Sql.number(s,"id")==id&&Sql.flag(s,"spaced"));
            for(var seat:seats)if(Sql.number(seat,"examId")==id){
                int student=Sql.number(seat,"studentId"),seatId=Sql.number(seat,"seatId");
                if(!assignedStudents.add(student)||!assignedSeats.add(seatId))add(conflicts,exam,"DUPLICATE_SEATING","ERROR","Duplicate seating assignment","Regenerate the seating plan.","seating.html");
                if(!roster.contains(student))add(conflicts,exam,"UNENROLLED_STUDENT","ERROR","An assigned student is outside the roster","Review course enrollments and regenerate seating.","seating.html");
                if(!examRooms.contains(Sql.number(seat,"roomId")))add(conflicts,exam,"WRONG_ROOM","ERROR","A seat is outside the exam rooms","Review examination rooms and seating.","seating.html");
                if(spaced&&Sql.number(seat,"seatColumn")%2==0)add(conflicts,exam,"SPACING","ERROR","A reserved spacing column is occupied","Move the student to an eligible odd column.","seating.html");
                if(Sql.flag(seat,"requiresAccessible")&&!Sql.flag(seat,"accessible"))add(conflicts,exam,"ACCESSIBILITY","ERROR","A required accessible seat is unsuitable","Both room entry and the seat must be accessible.","seating.html");
            }
            long missing=roster.stream().filter(s -> !assignedStudents.contains(s)).count();
            if(missing>0)add(conflicts,exam,"MISSING_SEATING","ERROR","Seating is incomplete",missing+" enrolled students have no seat.","seating.html");
            if(!LocalTime.parse(exam.get("endTime").toString()).isAfter(LocalTime.parse(exam.get("startTime").toString())))add(conflicts,exam,"INVALID_TIME","ERROR","Invalid exam interval","End time must follow start time.","exams.html");
            var required=duties.requiredSlots(c,exam,rules);Set<String> requiredKeys=new HashSet<>();
            for(Slot slot:required){requiredKeys.add(slot.key());if(InvigilationDao.rows(planning,"duties").stream().noneMatch(d -> InvigilationDao.slot(d).key().equals(slot.key())&&!"DECLINED".equals(d.get("status"))))add(conflicts,exam,"UNCOVERED_DUTY","ERROR","Invigilation coverage is incomplete","Room "+roomName(rooms,slot.room(),id)+", duty slot "+slot.number()+" needs a faculty member.","invigilation.html");}
            for(var duty:InvigilationDao.rows(planning,"duties"))if(Sql.number(duty,"examId")==id){
                if(!requiredKeys.contains(InvigilationDao.slot(duty).key()))add(conflicts,exam,"EXTRA_DUTY","ERROR","Duty no longer matches occupied seating","Regenerate duties after reviewing the seating plan or staffing ratio.","invigilation.html");
                if("DECLINED".equals(duty.get("status"))){add(conflicts,exam,"DECLINED_DUTY","ERROR","A faculty member declined a duty",duty.get("facultyName")+" / "+duty.get("roomNumber")+" needs reassignment.","invigilation.html");continue;}
                if("PENDING".equals(duty.get("status")))add(conflicts,exam,"PENDING_CONFIRMATION","WARNING","Duty confirmation is pending",duty.get("facultyName")+" has not accepted the duty.","invigilation.html");
                Faculty person=InvigilationDao.rows(planning,"faculty").stream().filter(f -> Sql.number(f,"id")==Sql.number(duty,"facultyId")).map(InvigilationDao::faculty).findFirst().orElseThrow();
                List<Duty> others=new ArrayList<>();for(var other:InvigilationDao.rows(planning,"duties"))if(Sql.number(other,"id")!=Sql.number(duty,"id")&&!"CANCELLED".equals(other.get("examStatus")))others.add(new Duty(InvigilationDao.slot(other),Sql.number(other,"facultyId"),other.get("status").toString()));
                for(String error:allocator.violations(person,InvigilationDao.slot(duty),others,InvigilationDao.unavailability(planning),InvigilationDao.exclusions(planning),rules))add(conflicts,exam,"FACULTY_CONSTRAINT","ERROR",error,duty.get("facultyName")+" / "+duty.get("roomNumber"),"availability.html");
            }
        }
        // Compare against all years, including exams outside the selected dashboard filter.
        for(int a=0;a<exams.size();a++)for(int b=a+1;b<exams.size();b++){
            var left=exams.get(a);var right=exams.get(b);
            if("CANCELLED".equals(left.get("status"))||"CANCELLED".equals(right.get("status"))||!overlap(left,right))continue;
            int first=Sql.number(left,"id"),second=Sql.number(right,"id");
            boolean roomClash=roomIds.getOrDefault(first,Set.of()).stream().anyMatch(roomIds.getOrDefault(second,Set.of())::contains);
            boolean studentClash=rosters.getOrDefault(first,Set.of()).stream().anyMatch(rosters.getOrDefault(second,Set.of())::contains);
            for(var exam:List.of(left,right))if(Sql.number(exam,"academicYearId")==selected&&Set.of("DRAFT","SCHEDULED").contains(exam.get("status"))){
                var other=exam==left?right:left;
                if(roomClash)add(conflicts,exam,"ROOM_OVERLAP","ERROR","Room timetable clash","Overlaps "+other.get("courseCode")+" on "+other.get("examDate")+".","exams.html");
                if(studentClash)add(conflicts,exam,"STUDENT_OVERLAP","ERROR","Student timetable clash","Shares students with "+other.get("courseCode")+" at the same time.","exams.html");
            }
        }
        for(var exam:exams)if(Sql.number(exam,"academicYearId")==selected){
            int id=Sql.number(exam,"id");long errors=conflicts.stream().filter(f -> Sql.number(f,"examId")==id&&"ERROR".equals(f.get("severity"))).count(),warnings=conflicts.stream().filter(f -> Sql.number(f,"examId")==id&&"WARNING".equals(f.get("severity"))).count();
            var row=new LinkedHashMap<>(exam);row.put("errors",errors);row.put("warnings",warnings);
            row.put("readiness",Set.of("COMPLETED","CANCELLED").contains(exam.get("status"))?exam.get("status"):(errors+warnings==0&&"SCHEDULED".equals(exam.get("status"))?"READY":"NEEDS_ATTENTION"));readiness.add(row);
        }
        return Map.of("selectedYearId",selected,"years",planning.get("years"),"exams",readiness,"conflicts",conflicts,
                "errors",conflicts.stream().filter(f -> "ERROR".equals(f.get("severity"))).count(),"warnings",conflicts.stream().filter(f -> "WARNING".equals(f.get("severity"))).count());
    }
    private static String roomName(List<Map<String,Object>> rooms,int room,int exam){return rooms.stream().filter(r -> Sql.number(r,"roomId")==room&&Sql.number(r,"examId")==exam).map(r -> r.get("roomNumber").toString()).findFirst().orElse(Integer.toString(room));}
    private static boolean overlap(Map<String,Object> a,Map<String,Object> b){return a.get("examDate").toString().equals(b.get("examDate").toString())&&a.get("startTime").toString().compareTo(b.get("endTime").toString())<0&&a.get("endTime").toString().compareTo(b.get("startTime").toString())>0;}
    private static void add(List<Map<String,Object>> out,Map<String,Object> exam,String type,String severity,String title,String detail,String page){out.add(Map.of("examId",exam.get("id"),"courseCode",exam.get("courseCode"),"examDate",exam.get("examDate"),"type",type,"severity",severity,"title",title,"detail",detail,"page",page));}

    public Map<String,Object> statistics(int year,int user) throws SQLException {
        return Sql.write(c -> {
            var scan=scan(c,year);int selected=Sql.number(scan,"selectedYearId");
            var totals=Sql.one(c,"SELECT COUNT(*) AS exams,SUM(status='SCHEDULED') AS scheduled,SUM(status='COMPLETED') AS completed,SUM(status='DRAFT') AS drafts FROM examinations WHERE academic_year_id=?",selected);
            var workload=Sql.rows(c,"SELECT f.id,f.name,f.employee_code AS employeeCode,COUNT(CASE WHEN a.status<>'DECLINED' AND e.status<>'CANCELLED' THEN a.id END) AS duties,COALESCE(SUM(CASE WHEN a.status<>'DECLINED' AND e.status<>'CANCELLED' THEN TIMESTAMPDIFF(MINUTE,e.start_time,e.end_time) ELSE 0 END),0) AS minutes,COUNT(CASE WHEN a.status='PENDING' AND e.status='SCHEDULED' THEN a.id END) AS pending FROM faculty f LEFT JOIN invigilation_assignments a ON a.faculty_id=f.id LEFT JOIN examinations e ON e.id=a.examination_id AND e.academic_year_id=? GROUP BY f.id,f.name,f.employee_code ORDER BY duties,f.employee_code",selected);
            return Map.of("selectedYearId",selected,"totals",totals,"workload",workload,"readiness",scan.get("exams"),"errors",scan.get("errors"),"warnings",scan.get("warnings"),"seatingAssignments",Sql.one(c,"SELECT COUNT(*) AS total FROM seating_assignments a JOIN examinations e ON e.id=a.examination_id WHERE e.academic_year_id=? AND e.status<>'CANCELLED'",selected).get("total"),"unreadNotifications",Sql.one(c,"SELECT COUNT(*) AS total FROM notifications WHERE recipient_user_id=? AND read_at IS NULL",user).get("total"));
        });
    }
}
