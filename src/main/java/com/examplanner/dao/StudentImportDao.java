package com.examplanner.dao;
import com.examplanner.util.*;
import java.sql.*;
import java.util.*;

public class StudentImportDao {
 public record CsvRow(int line,String[] values) {}
 public Map<String,Object> importRows(List<CsvRow> rows,boolean legacy) throws SQLException {
  int imported=0,enrolled=0,duplicates=0; List<String> errors=new ArrayList<>();
  for(CsvRow row:rows) {
   try { String result=Sql.write(c->importRow(c,row.values(),legacy)); switch(result) { case "imported" -> imported++; case "enrolled" -> enrolled++; default -> duplicates++; } }
   catch(IllegalArgumentException e) { errors.add("Line "+row.line()+": "+e.getMessage()); }
   catch(SQLException e) { if(e.getSQLState()!=null&&e.getSQLState().startsWith("23")) errors.add("Line "+row.line()+": duplicate or invalid linked value"); else throw e; }
  }
  return Map.of("imported",imported,"enrolled",enrolled,"duplicates",duplicates,"invalid",errors.size(),"errors",errors);
 }
 private String importRow(Connection c,String[] v,boolean legacy) throws SQLException {
  if(v.length!=(legacy?6:9)) throw new IllegalArgumentException("Expected "+(legacy?6:9)+" columns; quoted commas are unsupported");
  for(int i=0;i<v.length;i++) v[i]=v[i].trim();
  if(v[0].isEmpty()||v[0].length()>30||v[1].isEmpty()||v[1].length()>100) throw new IllegalArgumentException("Valid roll number and name are required");
  if(!v[2].isEmpty()&&(!ValidationUtil.validEmail(v[2])||v[2].length()>120)) throw new IllegalArgumentException("Invalid email");
  int semester; try { semester=Integer.parseInt(v[4]); if(semester<1||semester>8) throw new NumberFormatException(); } catch(NumberFormatException e) { throw new IllegalArgumentException("Invalid semester"); }
  boolean accessible=false;
  if(!legacy) { if(!Set.of("true","false","1","0").contains(v[8].toLowerCase())) throw new IllegalArgumentException("requires_accessible must be true or false"); accessible=Set.of("true","1").contains(v[8].toLowerCase()); }
  int department=Sql.number(Sql.one(c,"SELECT id FROM departments WHERE code=?",v[3]),"id");
  int year=Sql.number(legacy?Sql.one(c,"SELECT id FROM academic_years WHERE is_current=TRUE"):Sql.one(c,"SELECT id FROM academic_years WHERE label=?",v[6]),"id");
  Map<String,Object> course=Sql.one(c,"SELECT c.id,c.semester FROM courses c JOIN course_offerings o ON o.course_id=c.id WHERE c.course_code=? AND o.academic_year_id=?",v[5],year);
  if(Sql.number(course,"semester")!=semester) throw new IllegalArgumentException("Course and student semesters differ");
  int courseId=Sql.number(course,"id"); AcademicDao.protectEnrollment(c,courseId,year);
  String division=legacy?"A":v[7]; if(division.isEmpty()||division.length()>20) throw new IllegalArgumentException("Invalid division");
  int group=Sql.number(Sql.one(c,"SELECT id FROM divisions WHERE academic_year_id=? AND department_id=? AND study_year=? AND code=?",year,department,(semester+1)/2,division),"id");
  List<Map<String,Object>> existing=Sql.rows(c,"SELECT * FROM students WHERE roll_number=?",v[0]); int student; boolean created=existing.isEmpty();
  if(created) student=Sql.insert(c,"INSERT INTO students(roll_number,name,email,department_id,semester,requires_accessible) VALUES (?,?,?,?,?,?)",v[0],v[1],v[2].isEmpty()?null:v[2],department,semester,accessible);
  else {
   Map<String,Object> old=existing.get(0); student=Sql.number(old,"id");
   if(!v[1].equals(old.get("name"))||!Objects.equals(v[2].isEmpty()?null:v[2],old.get("email"))||(!legacy&&accessible!=Sql.flag(old,"requires_accessible"))) throw new IllegalArgumentException("Existing student details differ; edit the student explicitly");
  }
  List<Map<String,Object>> placements=Sql.rows(c,"SELECT division_id,semester FROM student_academic_enrollments WHERE student_id=? AND academic_year_id=?",student,year);
  if(placements.isEmpty()) Sql.update(c,"INSERT INTO student_academic_enrollments VALUES (?,?,?,?)",student,year,group,semester);
  else if(Sql.number(placements.get(0),"division_id")!=group||Sql.number(placements.get(0),"semester")!=semester) throw new IllegalArgumentException("Existing annual placement differs");
  if(!Sql.rows(c,"SELECT id FROM course_enrollments WHERE student_id=? AND course_id=? AND academic_year_id=?",student,courseId,year).isEmpty()) return "duplicate";
  AcademicDao.validateNewEnrollment(c,student,courseId,year);
  Sql.update(c,"INSERT INTO course_enrollments(student_id,course_id,academic_year_id) VALUES (?,?,?)",student,courseId,year);
  return created?"imported":"enrolled";
 }
}
