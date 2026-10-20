package com.examplanner.service;

import java.time.*;
import java.util.*;

/** Deterministic, least-loaded assignment with bounded backtracking. */
public class InvigilationAllocator {
    public record Faculty(int id, String code, int department, boolean available, int maxDaily, int maxTotal) {}
    public record Slot(int exam, int room, int number, int course, int department, int year,
                       LocalDate date, LocalTime start, LocalTime end) {
        public String key() { return exam+":"+room+":"+number; }
        public long minutes() { return Duration.between(start,end).toMinutes(); }
    }
    public record Duty(Slot slot, int faculty, String status) {}
    public record Unavailable(int faculty, LocalDate date, LocalTime start, LocalTime end) {}
    public record Rules(int restMinutes, int studentsPerInvigilator, boolean avoidOwnDepartment) {}
    private int explored;

    public List<String> violations(Faculty faculty, Slot slot, List<Duty> duties,
                                   List<Unavailable> unavailable, Set<String> exclusions, Rules rules) {
        List<String> errors=new ArrayList<>();
        if (!faculty.available()) errors.add("Faculty member is unavailable for invigilation");
        if (exclusions.contains(faculty.id()+":"+slot.course())) errors.add("Faculty member teaches or has a conflict with this course");
        if (rules.avoidOwnDepartment() && faculty.department()==slot.department()) errors.add("Faculty member belongs to the course department");
        for (Unavailable block:unavailable) if (block.faculty()==faculty.id() && block.date().equals(slot.date())
                && slot.start().isBefore(block.end()) && slot.end().isAfter(block.start())) {
            errors.add("Duty overlaps recorded unavailability"); break;
        }
        int daily=0,total=0;
        for (Duty duty:duties) {
            if (duty.faculty()!=faculty.id() || "DECLINED".equals(duty.status())) continue;
            Slot other=duty.slot();
            if (other.year()==slot.year()) total++;
            if (!other.date().equals(slot.date())) continue;
            daily++;
            if (other.exam()==slot.exam()) errors.add("Faculty member already has a room in this examination");
            else if (slot.start().isBefore(other.end()) && slot.end().isAfter(other.start())) errors.add("Faculty member has an overlapping duty");
            else {
                long gap=slot.start().isBefore(other.start()) ? Duration.between(slot.end(),other.start()).toMinutes()
                        : Duration.between(other.end(),slot.start()).toMinutes();
                if (gap<rules.restMinutes()) errors.add("Duty does not allow the required rest gap");
            }
        }
        if (daily>=faculty.maxDaily()) errors.add("Daily duty limit reached");
        if (total>=faculty.maxTotal()) errors.add("Academic-year duty limit reached");
        return errors;
    }

    public Map<String,Integer> allocate(List<Slot> requested, List<Faculty> faculty, List<Duty> existing,
            List<Unavailable> unavailable, Set<String> exclusions, Set<String> declined, Rules rules) {
        List<Duty> working=new ArrayList<>(existing);
        List<Slot> slots=new ArrayList<>(requested);
        slots.sort(Comparator.<Slot>comparingInt(slot -> candidates(slot,faculty,working,unavailable,exclusions,declined,rules).size())
                .thenComparing(Slot::date).thenComparing(Slot::start).thenComparingInt(Slot::exam)
                .thenComparingInt(Slot::room).thenComparingInt(Slot::number));
        Map<String,Integer> result=new LinkedHashMap<>(); explored=0;
        if (!assign(0,slots,faculty,working,unavailable,exclusions,declined,rules,result))
            throw new IllegalArgumentException(explored>100000
                    ? "Allocation search limit reached. Allocate fewer examinations at a time or adjust availability. No changes saved."
                    : "No complete duty plan satisfies availability, teaching conflicts, rest gaps and workload limits. Review faculty settings or allocate fewer exams. No changes saved.");
        return result;
    }
    private boolean assign(int index,List<Slot> slots,List<Faculty> faculty,List<Duty> duties,
            List<Unavailable> unavailable,Set<String> exclusions,Set<String> declined,Rules rules,Map<String,Integer> result) {
        if (index==slots.size()) return true;
        if (++explored>100000) return false;
        Slot slot=slots.get(index);
        for (Faculty person:candidates(slot,faculty,duties,unavailable,exclusions,declined,rules)) {
            duties.add(new Duty(slot,person.id(),"PENDING")); result.put(slot.key(),person.id());
            if (assign(index+1,slots,faculty,duties,unavailable,exclusions,declined,rules,result)) return true;
            duties.remove(duties.size()-1); result.remove(slot.key());
            if (explored>100000) return false;
        }
        return false;
    }
    private List<Faculty> candidates(Slot slot,List<Faculty> faculty,List<Duty> duties,List<Unavailable> unavailable,
            Set<String> exclusions,Set<String> declined,Rules rules) {
        List<Faculty> result=new ArrayList<>();
        for (Faculty person:faculty) if (!declined.contains(slot.exam()+":"+person.id())
                && violations(person,slot,duties,unavailable,exclusions,rules).isEmpty()) result.add(person);
        result.sort(Comparator.<Faculty>comparingLong(f -> duties.stream().filter(d -> d.faculty()==f.id()
                        && d.slot().year()==slot.year() && !"DECLINED".equals(d.status())).mapToLong(d -> d.slot().minutes()).sum())
                .thenComparingLong(f -> duties.stream().filter(d -> d.faculty()==f.id() && d.slot().year()==slot.year()
                        && !"DECLINED".equals(d.status())).count())
                .thenComparing(Faculty::code,String.CASE_INSENSITIVE_ORDER).thenComparingInt(Faculty::id));
        return result;
    }
}
