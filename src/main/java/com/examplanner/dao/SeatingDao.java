package com.examplanner.dao;

import com.examplanner.config.DbConnection;
import com.examplanner.service.ManualSeatingRules;
import com.examplanner.service.SeatingAllocator;
import com.examplanner.service.SeatingAllocator.*;
import com.examplanner.util.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.util.*;

public class SeatingDao {
    private final SeatingAllocator allocator = new SeatingAllocator();
    private final ManualSeatingRules manualRules = new ManualSeatingRules();
    private record Plan(Map<String,Object> data, Map<Integer,Integer> allocation, Map<Integer,Integer> pins) {}
    private record AccommodationPlan(Map<String,Object> current, int studentId, boolean required,
            List<Plan> plans, int releasedManual, String revision) {}

    public Map<String,Object> chart(int examId) throws SQLException {
        // The shared planner lock gives the chart and its revision one consistent snapshot.
        return Sql.write(c -> snapshot(c, examId));
    }

    public Map<String,Object> preview(int examId, boolean replaceManual) throws SQLException {
        return Sql.write(c -> summary(generation(c, examId, replaceManual), replaceManual));
    }

    public int generate(int examId, boolean replaceManual, boolean confirmed, String expectedRevision,
            int actor, String reason) throws SQLException {
        validateReason(reason);
        return Sql.write(c -> {
            if (replaceManual && !confirmed)
                throw new IllegalArgumentException("Confirm replacement of manual assignments before regenerating");
            Plan plan = generation(c, examId, replaceManual);
            checkRevision(plan.data(), expectedRevision);
            persist(c, plan, actor, reason, assignments(plan.data()).isEmpty() ? "GENERATE" : "REGENERATE");
            return plan.allocation().size();
        });
    }

    public void override(int examId, int studentId, int targetSeat, int actor, String reason,
            boolean swap, String expectedRevision) throws SQLException {
        validateReason(reason);
        Sql.write(c -> {
            Map<String,Object> data = snapshot(c, examId);
            editable(data);
            checkRevision(data, expectedRevision);
            Map<Integer,Integer> old = assignments(data);
            Map<Integer,Integer> changes = manualRules.changes(students(data), seats(data), old,
                    studentId, targetSeat, swap, spaced(data));
            // Delete both sides first so the unique seat constraint is never temporarily violated.
            for (int student : changes.keySet())
                Sql.update(c, "DELETE FROM seating_assignments WHERE examination_id=? AND student_id=?", examId, student);
            for (var change : changes.entrySet()) {
                Sql.update(c, "INSERT INTO seating_assignments(examination_id,student_id,seat_id,is_manual) VALUES (?,?,?,TRUE)",
                        examId, change.getKey(), change.getValue());
                history(c, examId, change.getKey(), swap ? "SWAP" : "REASSIGN",
                        old.get(change.getKey()), change.getValue(), actor, reason);
            }
            return null;
        });
    }

    public Map<String,Object> previewAccommodation(int examId, int studentId, boolean required) throws SQLException {
        return Sql.write(c -> accommodationSummary(accommodationPlan(c, examId, studentId, required)));
    }

    public Map<String,Object> accommodation(int examId, int studentId, boolean required,
            String expectedRevision, int actor, String reason) throws SQLException {
        validateReason(reason);
        return Sql.write(c -> {
            AccommodationPlan change = accommodationPlan(c, examId, studentId, required);
            if (expectedRevision == null || !change.revision().equals(expectedRevision))
                throw new IllegalArgumentException("Seating or accommodation data changed; preview the requirement again");
            // All affected plans have been validated before either the tag or any assignment is written.
            Sql.update(c, "UPDATE students SET requires_accessible=? WHERE id=?", required, studentId);
            Integer oldSeat = assignments(change.current()).get(studentId), newSeat = oldSeat;
            for (Plan plan : change.plans()) {
                persist(c, plan, actor, reason, "ACCOMMODATION_REGENERATE");
                if (examId(plan.data()) == examId) newSeat = plan.allocation().get(studentId);
            }
            history(c, examId, studentId, required ? "ACCOMMODATION_ON" : "ACCOMMODATION_OFF",
                    oldSeat, newSeat, actor, reason);
            return accommodationSummary(change);
        });
    }

    public void clear(int examId, int actor, String reason, String expectedRevision) throws SQLException {
        validateReason(reason);
        Sql.write(c -> {
            Map<String,Object> data = snapshot(c, examId);
            checkRevision(data, expectedRevision);
            if (!Set.of("DRAFT","CANCELLED").contains(exam(data).get("status")))
                throw new IllegalArgumentException("Only draft or cancelled seating can be cleared");
            if(!Sql.rows(c,"SELECT id FROM invigilation_assignments WHERE examination_id=?",examId).isEmpty())
                throw new IllegalArgumentException("Clear invigilation duties before clearing this seating plan");
            for (var assignment : assignments(data).entrySet())
                history(c, examId, assignment.getKey(), "CLEAR", assignment.getValue(), null, actor, reason);
            Sql.update(c, "DELETE FROM seating_assignments WHERE examination_id=?", examId);
            return null;
        });
    }

    public List<Map<String,Object>> search(String roll, int year) throws SQLException {
        try (Connection c = DbConnection.getConnection()) {
            return Sql.rows(c, "SELECT st.roll_number AS rollNumber,st.name,c.course_code AS courseCode,c.course_name AS courseName,e.exam_date AS examDate,e.start_time AS startTime,e.end_time AS endTime,r.room_number AS roomNumber,s.seat_number AS seatNumber,y.label AS academicYear FROM seating_assignments a JOIN students st ON st.id=a.student_id JOIN examinations e ON e.id=a.examination_id JOIN courses c ON c.id=e.course_id JOIN seats s ON s.id=a.seat_id JOIN rooms r ON r.id=s.room_id JOIN academic_years y ON y.id=e.academic_year_id WHERE st.roll_number=? AND e.academic_year_id=? AND e.status IN ('SCHEDULED','COMPLETED') ORDER BY e.exam_date,e.start_time", roll, year);
        }
    }

    private Map<String,Object> snapshot(Connection c, int examId) throws SQLException {
        Map<String,Object> data = new LinkedHashMap<>();
        data.put("exam", Sql.one(c, "SELECT e.id,e.status,e.spacing_required AS spacingRequired,c.course_code AS courseCode,c.course_name AS courseName,y.label AS academicYear,e.exam_date AS examDate FROM examinations e JOIN courses c ON c.id=e.course_id JOIN academic_years y ON y.id=e.academic_year_id WHERE e.id=?", examId));
        data.put("seats", Sql.rows(c, "SELECT s.id,s.room_id AS roomId,r.room_number AS roomNumber,r.seat_columns AS seatColumns,s.seat_number AS seatNumber,s.seat_row AS rowNumber,s.seat_column AS columnNumber,(s.is_accessible AND r.is_accessible) AS isAccessible,a.student_id AS studentId,a.is_manual AS isManual,st.roll_number AS rollNumber,st.name,st.requires_accessible AS requiresAccessible FROM examination_rooms er JOIN rooms r ON r.id=er.room_id JOIN seats s ON s.room_id=r.id LEFT JOIN seating_assignments a ON a.seat_id=s.id AND a.examination_id=er.examination_id LEFT JOIN students st ON st.id=a.student_id WHERE er.examination_id=? ORDER BY r.room_number,r.id,s.seat_row,s.seat_column,s.id", examId));
        data.put("students", Sql.rows(c, "SELECT s.id,s.roll_number AS rollNumber,s.name,s.requires_accessible AS requiresAccessible FROM examinations e JOIN course_enrollments ce ON ce.course_id=e.course_id AND ce.academic_year_id=e.academic_year_id JOIN students s ON s.id=ce.student_id WHERE e.id=? ORDER BY s.roll_number,s.id", examId));
        data.put("revision", digest(JsonUtil.json(data)));
        data.put("history", Sql.rows(c, "SELECT h.id,h.action,h.student_id AS studentId,st.roll_number AS rollNumber,st.name,h.old_seat AS oldSeat,h.new_seat AS newSeat,h.reason,h.created_at AS createdAt,u.username AS actor FROM seating_history h JOIN users u ON u.id=h.actor_user_id LEFT JOIN students st ON st.id=h.student_id WHERE h.examination_id=? ORDER BY h.id DESC LIMIT 200", examId));
        return data;
    }

    private Plan generation(Connection c, int examId, boolean replaceManual) throws SQLException {
        Map<String,Object> data = snapshot(c, examId);
        editable(data);
        List<Student> roster = students(data);
        if (roster.isEmpty()) throw new IllegalArgumentException("No students enrolled for this course and academic year");
        Map<Integer,Integer> pins = replaceManual ? new LinkedHashMap<>() : pins(data);
        return new Plan(data, allocator.allocate(roster, seats(data), pins, spaced(data)), pins);
    }

    private AccommodationPlan accommodationPlan(Connection c, int examId, int studentId, boolean required) throws SQLException {
        Map<String,Object> current = snapshot(c, examId);
        editable(current);
        Student selected = students(current).stream().filter(s -> s.id() == studentId).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Student is not enrolled in this examination"));
        if (selected.accessible() == required) throw new IllegalArgumentException("The accommodation requirement is already set to this value");
        List<Plan> plans = new ArrayList<>();
        List<String> revisions = new ArrayList<>();
        revisions.add(current.get("revision").toString());
        int released = 0;
        for (Map<String,Object> row : Sql.rows(c, "SELECT DISTINCT e.id FROM examinations e JOIN course_enrollments ce ON ce.course_id=e.course_id AND ce.academic_year_id=e.academic_year_id WHERE ce.student_id=? AND e.status IN ('DRAFT','SCHEDULED') ORDER BY e.id", studentId)) {
            int id = Sql.number(row, "id");
            Map<String,Object> data = id == examId ? current : snapshot(c, id);
            revisions.add(data.get("revision").toString());
            if (assignments(data).isEmpty()) continue; // Tag ungenerated exams without generating them implicitly.
            List<Student> roster = new ArrayList<>();
            for (Student student : students(data))
                roster.add(student.id() == studentId ? new Student(student.id(), student.roll(), required) : student);
            Map<Integer,Integer> pins = pins(data);
            Integer pin = pins.get(studentId);
            if (pin != null) {
                Seat seat = seats(data).stream().filter(s -> s.id() == pin).findFirst().orElse(null);
                if (seat == null || !allocator.eligible(new Student(studentId, selected.roll(), required), seat, spaced(data))) {
                    pins.remove(studentId);
                    released++;
                }
            }
            try {
                plans.add(new Plan(data, allocator.allocate(roster, seats(data), pins, spaced(data)), pins));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(exam(data).get("courseCode") + " / " + exam(data).get("academicYear") + ": " + e.getMessage());
            }
        }
        String revision = digest(studentId + ":" + required + ":" + String.join(":", revisions));
        return new AccommodationPlan(current, studentId, required, plans, released, revision);
    }

    private Map<String,Object> summary(Plan plan, boolean replaceManual) {
        Map<String,Object> data = plan.data();
        List<Map<String,Object>> changes = new ArrayList<>();
        Map<Integer,Integer> old = assignments(data);
        for (Student student : students(data)) {
            Integer seat = plan.allocation().get(student.id());
            if (!Objects.equals(old.get(student.id()), seat)) {
                Map<String,Object> change = new LinkedHashMap<>();
                change.put("rollNumber", student.roll());
                change.put("oldSeat", seatLabel(data, old.get(student.id())));
                change.put("newSeat", seatLabel(data, seat));
                changes.add(change);
            }
        }
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("revision", data.get("revision"));
        result.put("assigned", plan.allocation().size());
        result.put("changed", changes.size());
        result.put("manualAssignments", pins(data).size());
        result.put("preservedManual", plan.pins().size());
        result.put("replacedManual", pins(data).size() - plan.pins().size());
        result.put("requiresAccessible", students(data).stream().filter(Student::accessible).count());
        result.put("replaceManual", replaceManual);
        result.put("changes", changes);
        return result;
    }

    private Map<String,Object> accommodationSummary(AccommodationPlan change) {
        List<Map<String,Object>> exams = new ArrayList<>();
        int changed = 0;
        for (Plan plan : change.plans()) {
            Map<String,Object> summary = summary(plan, false);
            int count = ((Number)summary.get("changed")).intValue();
            changed += count;
            exams.add(Map.of("id", examId(plan.data()), "courseCode", exam(plan.data()).get("courseCode"),
                    "academicYear", exam(plan.data()).get("academicYear"), "changed", count));
        }
        return Map.of("revision", change.revision(), "studentId", change.studentId(),
                "requiresAccessible", change.required(), "affectedExams", exams,
                "changed", changed, "releasedManual", change.releasedManual());
    }

    private void persist(Connection c, Plan plan, int actor, String reason, String action) throws SQLException {
        int examId = examId(plan.data());
        Map<Integer,Integer> old = assignments(plan.data());
        Sql.update(c, "DELETE FROM seating_assignments WHERE examination_id=?", examId);
        for (var assignment : plan.allocation().entrySet()) {
            Sql.update(c, "INSERT INTO seating_assignments(examination_id,student_id,seat_id,is_manual) VALUES (?,?,?,?)",
                    examId, assignment.getKey(), assignment.getValue(), plan.pins().containsKey(assignment.getKey()));
            history(c, examId, assignment.getKey(), action, old.get(assignment.getKey()), assignment.getValue(), actor, reason);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String,Object>> rows(Map<String,Object> data, String key) { return (List<Map<String,Object>>)data.get(key); }
    @SuppressWarnings("unchecked")
    private Map<String,Object> exam(Map<String,Object> data) { return (Map<String,Object>)data.get("exam"); }
    private int examId(Map<String,Object> data) { return Sql.number(exam(data), "id"); }
    private boolean spaced(Map<String,Object> data) { return Sql.flag(exam(data), "spacingRequired"); }
    private void editable(Map<String,Object> data) {
        if (!Set.of("DRAFT","SCHEDULED").contains(exam(data).get("status")))
            throw new IllegalArgumentException("Completed or cancelled exams cannot be allocated, tagged or overridden");
    }
    private List<Student> students(Map<String,Object> data) {
        List<Student> result = new ArrayList<>();
        for (var student : rows(data, "students"))
            result.add(new Student(Sql.number(student, "id"), student.get("rollNumber").toString(), Sql.flag(student, "requiresAccessible")));
        return result;
    }
    private List<Seat> seats(Map<String,Object> data) {
        List<Seat> result = new ArrayList<>();
        for (var seat : rows(data, "seats"))
            result.add(new Seat(Sql.number(seat, "id"), Sql.number(seat, "roomId"), seat.get("roomNumber").toString(),
                    Sql.number(seat, "rowNumber"), Sql.number(seat, "columnNumber"), Sql.flag(seat, "isAccessible")));
        return result;
    }
    private Map<Integer,Integer> assignments(Map<String,Object> data) {
        Map<Integer,Integer> result = new LinkedHashMap<>();
        for (var seat : rows(data, "seats")) if (seat.get("studentId") != null)
            result.put(Sql.number(seat, "studentId"), Sql.number(seat, "id"));
        return result;
    }
    private Map<Integer,Integer> pins(Map<String,Object> data) {
        Map<Integer,Integer> result = new LinkedHashMap<>();
        for (var seat : rows(data, "seats")) if (seat.get("studentId") != null && Sql.flag(seat, "isManual"))
            result.put(Sql.number(seat, "studentId"), Sql.number(seat, "id"));
        return result;
    }
    private void checkRevision(Map<String,Object> data, String expected) {
        if (expected != null && !expected.equals(data.get("revision")))
            throw new IllegalArgumentException("Seating data changed; refresh the chart and review the operation again");
    }
    private void validateReason(String reason) {
        if (reason == null || reason.trim().isEmpty() || reason.trim().length() > 250)
            throw new IllegalArgumentException("A reason of 1 to 250 characters is required");
    }
    private String digest(String text) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private String seatLabel(Map<String,Object> data, Integer id) {
        if (id == null) return null;
        for (var seat : rows(data, "seats")) if (Sql.number(seat, "id") == id) return seat.get("roomNumber") + " / " + seat.get("seatNumber");
        return null;
    }
    private void history(Connection c, int examId, int student, String action, Integer oldSeat, Integer newSeat,
            int actor, String reason) throws SQLException {
        Sql.update(c, "INSERT INTO seating_history(examination_id,student_id,action,old_seat,new_seat,actor_user_id,reason) VALUES (?,?,?,?,?,?,?)",
                examId, student, action, label(c, oldSeat), label(c, newSeat), actor, reason.trim());
    }
    private String label(Connection c, Integer seat) throws SQLException {
        if (seat == null) return null;
        return Sql.one(c, "SELECT CONCAT(r.room_number,' / ',s.seat_number) AS label FROM seats s JOIN rooms r ON r.id=s.room_id WHERE s.id=?", seat).get("label").toString();
    }
}
