package com.examplanner.dao;

import com.examplanner.config.DbConnection;
import com.examplanner.service.InvigilationAllocator;
import com.examplanner.service.InvigilationAllocator.*;
import com.examplanner.util.*;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.time.*;
import java.util.*;

public class InvigilationDao {
    private final InvigilationAllocator allocator=new InvigilationAllocator();
    private static final String DUTIES="SELECT a.id,a.examination_id AS examId,a.room_id AS roomId,a.slot_number AS slotNumber,a.faculty_id AS facultyId,a.status,a.is_manual AS isManual,a.version,a.reason,a.response_note AS responseNote,a.responded_at AS respondedAt,e.status AS examStatus,e.exam_date AS examDate,e.start_time AS startTime,e.end_time AS endTime,e.course_id AS courseId,e.academic_year_id AS academicYearId,c.department_id AS departmentId,c.course_code AS courseCode,c.course_name AS courseName,y.label AS academicYear,r.room_number AS roomNumber,r.capacity AS roomCapacity,r.seat_columns AS roomColumns,r.is_accessible AS roomAccessible,f.name AS facultyName,f.employee_code AS employeeCode,f.user_id AS userId FROM invigilation_assignments a JOIN examinations e ON e.id=a.examination_id JOIN courses c ON c.id=e.course_id JOIN academic_years y ON y.id=e.academic_year_id JOIN rooms r ON r.id=a.room_id JOIN faculty f ON f.id=a.faculty_id ORDER BY e.exam_date,e.start_time,a.examination_id,r.room_number,a.slot_number";

    public Map<String,Object> data(int year) throws SQLException {
        return Sql.write(c -> snapshot(c,year));
    }
    public Map<String,Object> facultyData(int user) throws SQLException {
        return Sql.write(c -> {
            Map<String,Object> person=Sql.one(c,"SELECT f.id,f.name,f.employee_code AS employeeCode,COALESCE(s.available,TRUE) AS available,COALESCE(s.max_daily,2) AS maxDaily,COALESCE(s.max_total,20) AS maxTotal FROM faculty f LEFT JOIN faculty_duty_settings s ON s.faculty_id=f.id WHERE f.user_id=?",user);
            int id=Sql.number(person,"id");
            Map<String,Object> result=new LinkedHashMap<>(); result.put("faculty",person);
            result.put("years",Sql.rows(c,"SELECT id,label,is_current AS isCurrent FROM academic_years ORDER BY start_year DESC"));
            result.put("duties",dutyRows(c).stream().filter(d -> Sql.number(d,"facultyId")==id && !"DRAFT".equals(d.get("examStatus"))).toList());
            result.put("unavailability",Sql.rows(c,"SELECT id,unavailable_date AS unavailableDate,start_time AS startTime,end_time AS endTime,reason FROM faculty_unavailability WHERE faculty_id=? ORDER BY unavailable_date,start_time",id));
            result.put("rules",Sql.one(c,"SELECT rest_minutes AS restMinutes,students_per_invigilator AS studentsPerInvigilator,avoid_own_department AS avoidOwnDepartment FROM invigilation_rules WHERE id=1"));
            return result;
        });
    }
    Map<String,Object> snapshot(Connection c,int requestedYear) throws SQLException {
        int year=requestedYear==0?Sql.number(Sql.one(c,"SELECT id FROM academic_years ORDER BY is_current DESC,start_year DESC LIMIT 1"),"id"):requestedYear;
        Sql.one(c,"SELECT id FROM academic_years WHERE id=?",year);
        Map<String,Object> result=new LinkedHashMap<>(); result.put("selectedYearId",year);
        result.put("years",Sql.rows(c,"SELECT id,label,is_current AS isCurrent FROM academic_years ORDER BY start_year DESC"));
        result.put("rules",Sql.one(c,"SELECT rest_minutes AS restMinutes,students_per_invigilator AS studentsPerInvigilator,avoid_own_department AS avoidOwnDepartment FROM invigilation_rules WHERE id=1"));
        result.put("faculty",Sql.rows(c,"SELECT f.id,f.user_id AS userId,f.employee_code AS employeeCode,f.name,f.email,f.department_id AS departmentId,COALESCE(s.available,TRUE) AS available,COALESCE(s.max_daily,2) AS maxDaily,COALESCE(s.max_total,20) AS maxTotal FROM faculty f LEFT JOIN faculty_duty_settings s ON s.faculty_id=f.id ORDER BY f.employee_code,f.id"));
        result.put("unavailability",Sql.rows(c,"SELECT id,faculty_id AS facultyId,unavailable_date AS unavailableDate,start_time AS startTime,end_time AS endTime,reason FROM faculty_unavailability ORDER BY faculty_id,unavailable_date,start_time,id"));
        result.put("courseConflicts",Sql.rows(c,"SELECT faculty_id AS facultyId,course_id AS courseId FROM faculty_course_conflicts ORDER BY faculty_id,course_id"));
        result.put("courses",Sql.rows(c,"SELECT id,course_code AS code,course_name AS name FROM courses ORDER BY course_code"));
        result.put("duties",dutyRows(c));
        result.put("exams",Sql.rows(c,"SELECT e.id,e.course_id AS courseId,c.course_code AS courseCode,c.course_name AS courseName,c.department_id AS departmentId,e.academic_year_id AS academicYearId,e.exam_date AS examDate,e.start_time AS startTime,e.end_time AS endTime,e.status FROM examinations e JOIN courses c ON c.id=e.course_id ORDER BY e.exam_date,e.start_time,e.id"));
        result.put("history",Sql.rows(c,"SELECT h.id,h.examination_id AS examId,c.course_code AS courseCode,r.room_number AS roomNumber,h.slot_number AS slotNumber,pf.name AS previousFaculty,nf.name AS newFaculty,h.action,h.reason,h.created_at AS createdAt,u.username AS actor FROM invigilation_history h JOIN examinations e ON e.id=h.examination_id JOIN courses c ON c.id=e.course_id JOIN rooms r ON r.id=h.room_id LEFT JOIN faculty pf ON pf.id=h.previous_faculty_id LEFT JOIN faculty nf ON nf.id=h.new_faculty_id JOIN users u ON u.id=h.actor_user_id WHERE e.academic_year_id=? ORDER BY h.id DESC LIMIT 200",year));
        result.put("revision",revision(List.of(result,Sql.rows(c,"SELECT examination_id,student_id,seat_id FROM seating_assignments ORDER BY examination_id,student_id"),Sql.rows(c,"SELECT examination_id,room_id FROM examination_rooms ORDER BY examination_id,room_id"),Sql.rows(c,"SELECT student_id,course_id,academic_year_id FROM course_enrollments ORDER BY academic_year_id,course_id,student_id")))); return result;
    }
    List<Map<String,Object>> dutyRows(Connection c) throws SQLException { return Sql.rows(c,DUTIES); }
    @SuppressWarnings("unchecked") public static List<Map<String,Object>> rows(Map<String,Object> data,String key) { return (List<Map<String,Object>>)data.get(key); }
    @SuppressWarnings("unchecked") public static Rules rules(Map<String,Object> data) {
        Map<String,Object> r=(Map<String,Object>)data.get("rules");
        return new Rules(Sql.number(r,"restMinutes"),Sql.number(r,"studentsPerInvigilator"),Sql.flag(r,"avoidOwnDepartment"));
    }
    public static Faculty faculty(Map<String,Object> row) {
        return new Faculty(Sql.number(row,"id"),row.get("employeeCode").toString(),row.get("departmentId")==null?0:Sql.number(row,"departmentId"),row.get("userId")!=null && Sql.flag(row,"available"),Sql.number(row,"maxDaily"),Sql.number(row,"maxTotal"));
    }
    public static Slot slot(Map<String,Object> row) {
        int exam=row.containsKey("examId")?Sql.number(row,"examId"):Sql.number(row,"id");
        return new Slot(exam,Sql.number(row,"roomId"),Sql.number(row,"slotNumber"),Sql.number(row,"courseId"),Sql.number(row,"departmentId"),Sql.number(row,"academicYearId"),LocalDate.parse(row.get("examDate").toString()),LocalTime.parse(row.get("startTime").toString()),LocalTime.parse(row.get("endTime").toString()));
    }
    public static List<Duty> duties(Map<String,Object> data) {
        return rows(data,"duties").stream().filter(d -> !"CANCELLED".equals(d.get("examStatus")))
                .map(d -> new Duty(slot(d),Sql.number(d,"facultyId"),d.get("status").toString())).toList();
    }
    public static List<Unavailable> unavailability(Map<String,Object> data) {
        return rows(data,"unavailability").stream().map(r -> new Unavailable(Sql.number(r,"facultyId"),LocalDate.parse(r.get("unavailableDate").toString()),LocalTime.parse(r.get("startTime").toString()),LocalTime.parse(r.get("endTime").toString()))).toList();
    }
    public static Set<String> exclusions(Map<String,Object> data) {
        Set<String> result=new HashSet<>(); for(var r:rows(data,"courseConflicts")) result.add(r.get("facultyId")+":"+r.get("courseId")); return result;
    }
    List<Slot> requiredSlots(Connection c,Map<String,Object> exam,Rules rules) throws SQLException {
        List<Slot> result=new ArrayList<>();
        for (var room:Sql.rows(c,"SELECT s.room_id AS roomId,COUNT(*) AS seated FROM seating_assignments a JOIN seats s ON s.id=a.seat_id JOIN examination_rooms er ON er.examination_id=a.examination_id AND er.room_id=s.room_id WHERE a.examination_id=? GROUP BY s.room_id ORDER BY s.room_id",exam.get("id"))) {
            int count=Sql.number(room,"seated"),needed=(count+rules.studentsPerInvigilator()-1)/rules.studentsPerInvigilator();
            for(int n=1;n<=needed;n++) { Map<String,Object> row=new LinkedHashMap<>(exam); row.put("roomId",room.get("roomId")); row.put("slotNumber",n); result.add(slot(row)); }
        }
        return result;
    }
    private record Plan(Map<String,Object> data,List<Integer> exams,List<Slot> slots,Map<String,Integer> allocation,Set<String> kept,boolean replace) {}
    private Plan plan(Connection c,int year,int examId,boolean replace) throws SQLException {
        Map<String,Object> data=snapshot(c,year); Rules rule=rules(data);
        List<Map<String,Object>> exams=rows(data,"exams").stream().filter(e -> Sql.number(e,"academicYearId")==Sql.number(data,"selectedYearId") && (examId==0||Sql.number(e,"id")==examId) && Set.of("DRAFT","SCHEDULED").contains(e.get("status"))).toList();
        if(exams.isEmpty()) throw new IllegalArgumentException("Select a draft or scheduled exam in this academic year");
        List<Slot> slots=new ArrayList<>(); List<Integer> ids=new ArrayList<>();
        for(var exam:exams) {
            int id=Sql.number(exam,"id"); ids.add(id);
            int roster=Sql.number(Sql.one(c,"SELECT COUNT(*) AS total FROM course_enrollments WHERE course_id=? AND academic_year_id=?",exam.get("courseId"),exam.get("academicYearId")),"total");
            int seated=Sql.number(Sql.one(c,"SELECT COUNT(*) AS total FROM seating_assignments a JOIN course_enrollments ce ON ce.student_id=a.student_id AND ce.course_id=? AND ce.academic_year_id=? JOIN seats s ON s.id=a.seat_id JOIN examination_rooms er ON er.examination_id=a.examination_id AND er.room_id=s.room_id WHERE a.examination_id=?",exam.get("courseId"),exam.get("academicYearId"),id),"total");
            if(roster==0||seated!=roster) throw new IllegalArgumentException("Generate complete seating for "+exam.get("courseCode")+" before assigning invigilators");
            slots.addAll(requiredSlots(c,exam,rule));
        }
        if(slots.size()>256)throw new IllegalArgumentException("Select fewer exams: a single allocation batch supports up to 256 duty slots");
        List<Faculty> people=rows(data,"faculty").stream().map(InvigilationDao::faculty).toList();
        Map<String,Slot> byKey=new HashMap<>(); for(Slot s:slots) byKey.put(s.key(),s);
        List<Duty> working=new ArrayList<>(duties(data).stream().filter(d -> !ids.contains(d.slot().exam())).toList());
        Map<String,Integer> output=new LinkedHashMap<>(); Set<String> kept=new HashSet<>(),declined=new HashSet<>();
        for(var row:rows(data,"duties")) {
            Slot slot=slot(row); int fid=Sql.number(row,"facultyId");
            if("DECLINED".equals(row.get("status"))) declined.add(slot.exam()+":"+fid);
            if(!ids.contains(slot.exam())||replace||"DECLINED".equals(row.get("status"))||(!Sql.flag(row,"isManual")&&!"ACCEPTED".equals(row.get("status")))) continue;
            Faculty person=people.stream().filter(f -> f.id()==fid).findFirst().orElseThrow();
            if(!byKey.containsKey(slot.key())||!allocator.violations(person,slot,working,unavailability(data),exclusions(data),rule).isEmpty())
                throw new IllegalArgumentException("A preserved manual/accepted duty is no longer valid. Review the conflicts or explicitly replace existing duties.");
            kept.add(slot.key()); output.put(slot.key(),fid); working.add(new Duty(slot,fid,row.get("status").toString()));
        }
        output.putAll(allocator.allocate(slots.stream().filter(s -> !kept.contains(s.key())).toList(),people,working,unavailability(data),exclusions(data),declined,rule));
        return new Plan(data,ids,slots,output,kept,replace);
    }
    public Map<String,Object> preview(int year,int exam,boolean replace) throws SQLException { return Sql.write(c -> summary(c,plan(c,year,exam,replace))); }
    private Map<String,Object> summary(Connection c,Plan plan) throws SQLException {
        List<Map<String,Object>> changes=new ArrayList<>();
        for(Slot slot:plan.slots()) {
            int fid=plan.allocation().get(slot.key()); var person=rows(plan.data(),"faculty").stream().filter(f -> Sql.number(f,"id")==fid).findFirst().orElseThrow();
            var old=rows(plan.data(),"duties").stream().filter(d -> slot(d).key().equals(slot.key())).findFirst().orElse(null);
            var exam=rows(plan.data(),"exams").stream().filter(e -> Sql.number(e,"id")==slot.exam()).findFirst().orElseThrow();
            Map<String,Object> row=new LinkedHashMap<>(); row.put("courseCode",exam.get("courseCode")); row.put("examDate",slot.date()); row.put("roomNumber",Sql.one(c,"SELECT room_number FROM rooms WHERE id=?",slot.room()).get("room_number")); row.put("slotNumber",slot.number()); row.put("previousFaculty",old==null?null:old.get("facultyName")); row.put("facultyName",person.get("name")); row.put("preserved",plan.kept().contains(slot.key())); changes.add(row);
        }
        return Map.of("revision",plan.data().get("revision"),"duties",changes,"total",changes.size(),"preserved",plan.kept().size(),"replaceExisting",plan.replace());
    }
    public int generate(int year,int exam,boolean replace,boolean confirmed,String revision,int actor,String reason) throws SQLException {
        reason(reason);
        return Sql.write(c -> {
            Plan plan=plan(c,year,exam,replace); checkRevision(plan.data(),revision);
            if(replace&&!confirmed) throw new IllegalArgumentException("Confirm replacement of accepted and manual duties");
            Map<String,Map<String,Object>> old=new HashMap<>();
            for(var row:rows(plan.data(),"duties")) if(plan.exams().contains(Sql.number(row,"examId"))) old.put(slot(row).key(),row);
            for(var row:old.entrySet()) if(!plan.kept().contains(row.getKey())) Sql.update(c,"DELETE FROM invigilation_assignments WHERE id=?",row.getValue().get("id"));
            for(Slot slot:plan.slots()) {
                if(plan.kept().contains(slot.key())) continue;
                int fid=plan.allocation().get(slot.key()); var previous=old.get(slot.key());
                Sql.update(c,"INSERT INTO invigilation_assignments(examination_id,room_id,slot_number,faculty_id,reason) VALUES (?,?,?,?,?)",slot.exam(),slot.room(),slot.number(),fid,reason);
                history(c,slot,previous==null?null:previous.get("facultyId"),fid,"GENERATE",actor,reason);
                if(previous!=null&&Sql.number(previous,"facultyId")!=fid) notifyFaculty(c,Sql.number(previous,"facultyId"),slot.exam(),"Duty replaced","Your previous duty has been reassigned. "+reason);
                notifyFaculty(c,fid,slot.exam(),"Invigilation duty assigned","Review your duty schedule. Published duties require acceptance. "+reason);
            }
            for(var entry:old.entrySet()) if(plan.slots().stream().noneMatch(s -> s.key().equals(entry.getKey()))) {
                var row=entry.getValue(); history(c,slot(row),row.get("facultyId"),null,"REMOVE",actor,reason);
                notifyFaculty(c,Sql.number(row,"facultyId"),Sql.number(row,"examId"),"Duty removed",reason);
            }
            return plan.slots().size();
        });
    }
    public Map<String,Object> reassignPreview(int year,int dutyId,int facultyId) throws SQLException {
        return Sql.write(c -> { Map<String,Object> data=snapshot(c,year); var duty=validatedReplacement(data,dutyId,facultyId); return Map.of("revision",data.get("revision"),"duty",duty,"facultyName",rows(data,"faculty").stream().filter(f -> Sql.number(f,"id")==facultyId).findFirst().orElseThrow().get("name")); });
    }
    private Map<String,Object> validatedReplacement(Map<String,Object> data,int dutyId,int facultyId) {
        var duty=rows(data,"duties").stream().filter(d -> Sql.number(d,"id")==dutyId).findFirst().orElseThrow(() -> new IllegalArgumentException("Duty not found"));
        if(!Set.of("DRAFT","SCHEDULED").contains(duty.get("examStatus"))) throw new IllegalArgumentException("Only draft or scheduled duties can be reassigned");
        if(Sql.number(duty,"academicYearId")!=Sql.number(data,"selectedYearId")) throw new IllegalArgumentException("Select the duty's academic year");
        if(Sql.number(duty,"facultyId")==facultyId) throw new IllegalArgumentException("Choose a different faculty member");
        Faculty person=rows(data,"faculty").stream().filter(f -> Sql.number(f,"id")==facultyId).map(InvigilationDao::faculty).findFirst().orElseThrow(() -> new IllegalArgumentException("Faculty member not found"));
        List<Duty> others=new ArrayList<>(); for(var row:rows(data,"duties")) if(Sql.number(row,"id")!=dutyId&&!"CANCELLED".equals(row.get("examStatus"))) others.add(new Duty(slot(row),Sql.number(row,"facultyId"),row.get("status").toString()));
        List<String> errors=allocator.violations(person,slot(duty),others,unavailability(data),exclusions(data),rules(data));
        if(!errors.isEmpty()) throw new IllegalArgumentException(String.join("; ",errors));
        return duty;
    }
    public void reassign(int year,int dutyId,int facultyId,String revision,int actor,String reason) throws SQLException {
        reason(reason); Sql.write(c -> {
            Map<String,Object> data=snapshot(c,year); checkRevision(data,revision); var old=validatedReplacement(data,dutyId,facultyId);
            Sql.update(c,"UPDATE invigilation_assignments SET faculty_id=?,status='PENDING',is_manual=TRUE,version=version+1,reason=?,response_note=NULL,responded_at=NULL WHERE id=?",facultyId,reason,dutyId);
            history(c,slot(old),old.get("facultyId"),facultyId,"REASSIGN",actor,reason);
            notifyFaculty(c,Sql.number(old,"facultyId"),Sql.number(old,"examId"),"Duty reassigned",reason);
            notifyFaculty(c,facultyId,Sql.number(old,"examId"),"Invigilation duty assigned","Review and respond to your new duty. "+reason); return null;
        });
    }
    public void respond(int user,int dutyId,int version,String response,String note) throws SQLException {
        if(!Set.of("ACCEPTED","DECLINED").contains(response)) throw new IllegalArgumentException("Choose accept or decline");
        if("DECLINED".equals(response)) reason(note);
        Sql.write(c -> {
            var row=Sql.one(c,"SELECT a.*,e.status AS examStatus,f.user_id FROM invigilation_assignments a JOIN faculty f ON f.id=a.faculty_id JOIN examinations e ON e.id=a.examination_id WHERE a.id=? AND f.user_id=?",dutyId,user);
            if(!"SCHEDULED".equals(row.get("examStatus"))) throw new IllegalArgumentException("Only scheduled examination duties can be confirmed");
            if(Sql.number(row,"version")!=version) throw new IllegalArgumentException("This duty changed. Refresh your schedule.");
            if("DECLINED".equals(row.get("status"))) throw new IllegalArgumentException("This duty was declined; the administrator must reassign it");
            if(response.equals(row.get("status"))) throw new IllegalArgumentException("The duty already has this response");
            Sql.update(c,"UPDATE invigilation_assignments SET status=?,response_note=?,responded_at=CURRENT_TIMESTAMP,version=version+1 WHERE id=?",response,note,dutyId);
            var full=dutyRows(c).stream().filter(d -> Sql.number(d,"id")==dutyId).findFirst().orElseThrow();
            history(c,slot(full),row.get("faculty_id"),row.get("faculty_id"),response,user,note==null||note.isBlank()?"Duty accepted":note);
            NotificationDao.admins(c,"Duty "+response.toLowerCase(),full.get("facultyName")+" "+response.toLowerCase()+" "+full.get("courseCode")+" / "+full.get("roomNumber")+". "+(note==null?"":note),Sql.number(full,"examId")); return null;
        });
    }
    public void saveAvailability(HttpServletRequest r,int user,boolean admin) throws SQLException {
        Sql.write(c -> {
            int fid=admin?Input.id(r,"facultyId"):Sql.number(Sql.one(c,"SELECT id FROM faculty WHERE user_id=?",user),"id");
            String action=Input.text(r,"action",25);
            if("settings".equals(action)) {
                var current=Sql.rows(c,"SELECT max_daily,max_total FROM faculty_duty_settings WHERE faculty_id=?",fid);
                int daily=admin?Input.integer(r,"maxDaily",1,20):current.isEmpty()?2:Sql.number(current.get(0),"max_daily");
                int total=admin?Input.integer(r,"maxTotal",1,500):current.isEmpty()?20:Sql.number(current.get(0),"max_total");
                Sql.update(c,"INSERT INTO faculty_duty_settings(faculty_id,available,max_daily,max_total) VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE available=VALUES(available),max_daily=VALUES(max_daily),max_total=VALUES(max_total)",fid,Input.flag(r,"available"),daily,total);
            } else if("unavailable".equals(action)) {
                LocalDate date; LocalTime start,end;
                try {date=LocalDate.parse(Input.text(r,"date",10));start=LocalTime.parse(Input.text(r,"startTime",8));end=LocalTime.parse(Input.text(r,"endTime",8));}
                catch(java.time.format.DateTimeParseException e) {throw new IllegalArgumentException("Invalid date or time");}
                if(!end.isAfter(start)) throw new IllegalArgumentException("End time must be later than start time");
                Sql.update(c,"INSERT INTO faculty_unavailability(faculty_id,unavailable_date,start_time,end_time,reason) VALUES (?,?,?,?,?)",fid,date.toString(),start.toString(),end.toString(),Input.text(r,"reason",250));
            } else if("deleteUnavailable".equals(action)) {
                if(Sql.update(c,"DELETE FROM faculty_unavailability WHERE id=? AND faculty_id=?",Input.id(r,"id"),fid)==0) throw new IllegalArgumentException("Unavailable interval not found");
            } else throw new IllegalArgumentException("Unknown availability action");
            validateExisting(c,fid); return null;
        });
    }
    public void savePolicy(HttpServletRequest r) throws SQLException {
        Sql.write(c -> {
            if("rules".equals(Input.text(r,"action",25))) Sql.update(c,"UPDATE invigilation_rules SET rest_minutes=?,students_per_invigilator=?,avoid_own_department=? WHERE id=1",Input.integer(r,"restMinutes",0,240),Input.integer(r,"studentsPerInvigilator",1,500),Input.flag(r,"avoidOwnDepartment"));
            else {
                int fid=Input.id(r,"facultyId"),course=Input.id(r,"courseId");
                if(Input.flag(r,"excluded")) Sql.update(c,"INSERT IGNORE INTO faculty_course_conflicts(faculty_id,course_id) VALUES (?,?)",fid,course);
                else Sql.update(c,"DELETE FROM faculty_course_conflicts WHERE faculty_id=? AND course_id=?",fid,course);
            }
            validateExisting(c,0); return null;
        });
    }
    private void validateExisting(Connection c,int selectedFaculty) throws SQLException {
        var data=snapshot(c,0);
        for(var row:rows(data,"duties")) {
            if(!Set.of("DRAFT","SCHEDULED").contains(row.get("examStatus"))||"DECLINED".equals(row.get("status"))||(selectedFaculty!=0&&Sql.number(row,"facultyId")!=selectedFaculty)) continue;
            Faculty person=rows(data,"faculty").stream().filter(f -> Sql.number(f,"id")==Sql.number(row,"facultyId")).map(InvigilationDao::faculty).findFirst().orElseThrow();
            List<Duty> others=new ArrayList<>(); for(var other:rows(data,"duties")) if(Sql.number(other,"id")!=Sql.number(row,"id")&&!"CANCELLED".equals(other.get("examStatus"))) others.add(new Duty(slot(other),Sql.number(other,"facultyId"),other.get("status").toString()));
            var problems=allocator.violations(person,slot(row),others,unavailability(data),exclusions(data),rules(data));
            if(!problems.isEmpty()) throw new IllegalArgumentException("This change would invalidate an assigned duty. Decline or reassign it first: "+problems.get(0));
        }
    }
    public void clear(int exam,int actor,String reason) throws SQLException {
        reason(reason); Sql.write(c -> {
            var state=Sql.one(c,"SELECT status FROM examinations WHERE id=?",exam);
            if(!Set.of("DRAFT","CANCELLED").contains(state.get("status"))) throw new IllegalArgumentException("Only draft or cancelled duties can be cleared");
            for(var d:dutyRows(c)) if(Sql.number(d,"examId")==exam) {history(c,slot(d),d.get("facultyId"),null,"CLEAR",actor,reason);notifyFaculty(c,Sql.number(d,"facultyId"),exam,"Duty removed",reason);}
            Sql.update(c,"DELETE FROM invigilation_assignments WHERE examination_id=?",exam); return null;
        });
    }
    private static void history(Connection c,Slot slot,Object previous,Object next,String action,int actor,String reason) throws SQLException {
        Sql.update(c,"INSERT INTO invigilation_history(examination_id,room_id,slot_number,previous_faculty_id,new_faculty_id,action,actor_user_id,reason) VALUES (?,?,?,?,?,?,?,?)",slot.exam(),slot.room(),slot.number(),previous,next,action,actor,reason);
    }
    private static void notifyFaculty(Connection c,int faculty,int exam,String title,String message) throws SQLException {
        if("DRAFT".equals(Sql.one(c,"SELECT status FROM examinations WHERE id=?",exam).get("status")))return;
        var person=Sql.one(c,"SELECT user_id FROM faculty WHERE id=?",faculty);
        if(person.get("user_id")!=null) NotificationDao.user(c,Sql.number(person,"user_id"),title,message,exam);
    }
    public static void reason(String reason) {if(reason==null||reason.isBlank()||reason.length()>250) throw new IllegalArgumentException("A reason of 1-250 characters is required");}
    private static void checkRevision(Map<String,Object> data,String expected) {if(expected==null||!expected.equals(data.get("revision"))) throw new IllegalArgumentException("Planning data changed. Review the preview again before saving.");}
    private static String revision(Object value) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JsonUtil.json(value).getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
    }
}
