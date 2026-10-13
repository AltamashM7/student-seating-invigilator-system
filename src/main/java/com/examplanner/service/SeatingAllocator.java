package com.examplanner.service;
import java.util.*;

public class SeatingAllocator {
    public record Student(int id,String roll,boolean accessible) {}
    public record Seat(int id,int roomId,String room,int row,int column,boolean accessible) {}

    public Map<Integer,Integer> allocate(List<Student> students,List<Seat> seats,Map<Integer,Integer> pinned,boolean spaced) {
        Map<Integer,Student> byStudent=new HashMap<>();
        Map<Integer,Seat> bySeat=new HashMap<>();
        for(Student s:students) if(byStudent.put(s.id(),s)!=null) throw new IllegalArgumentException("Duplicate student input");
        for(Seat s:seats) if(bySeat.put(s.id(),s)!=null) throw new IllegalArgumentException("Duplicate seat input");
        Map<Integer,Integer> result=new LinkedHashMap<>(); Set<Integer> used=new HashSet<>();
        for(var entry:pinned.entrySet()) {
            Student student=byStudent.get(entry.getKey()); Seat seat=bySeat.get(entry.getValue());
            if(student==null || seat==null || !eligible(student,seat,spaced) || !used.add(seat.id())) throw new IllegalArgumentException("Preserved manual assignment is invalid");
            result.put(student.id(),seat.id());
        }
        List<Student> ordered=new ArrayList<>(students);
        // Explicit insertion sort keeps the algorithm understandable for the coursework.
        for(int i=1;i<ordered.size();i++) { Student v=ordered.get(i); int j=i-1; while(j>=0 && compare(ordered.get(j),v)>0) { ordered.set(j+1,ordered.get(j)); j--; } ordered.set(j+1,v); }
        List<Seat> available=new ArrayList<>(seats);
        for(int i=1;i<available.size();i++) { Seat v=available.get(i); int j=i-1; while(j>=0 && compare(available.get(j),v)>0) { available.set(j+1,available.get(j)); j--; } available.set(j+1,v); }
        // Allocate students with an accessibility requirement first. Their eligible accessible
        // seats are therefore selected before any general-purpose allocation runs.
        for(Student student:ordered) {
            if(!student.accessible() || result.containsKey(student.id())) continue;
            Seat chosen=findSeat(student,available,used,spaced,true);
            if(chosen==null) throw new IllegalArgumentException("Insufficient accessible seats; no changes saved");
            used.add(chosen.id()); result.put(student.id(),chosen.id());
        }
        // Keep accessible seats available for future accommodation needs when ordinary seats
        // suffice. Only let other students use the remaining accessible seats as a capacity fallback.
        for(Student student:ordered) {
            if(student.accessible() || result.containsKey(student.id())) continue;
            Seat chosen=findSeat(student,available,used,spaced,false);
            if(chosen!=null) { used.add(chosen.id()); result.put(student.id(),chosen.id()); }
        }
        for(Student student:ordered) {
            if(student.accessible() || result.containsKey(student.id())) continue;
            Seat chosen=findSeat(student,available,used,spaced,true);
            if(chosen==null) throw new IllegalArgumentException("Insufficient usable seat capacity; no changes saved");
            used.add(chosen.id()); result.put(student.id(),chosen.id());
        }
        return result;
    }
    private Seat findSeat(Student student,List<Seat> available,Set<Integer> used,boolean spaced,boolean accessibleOnly) {
        for(Seat seat:available) if(!used.contains(seat.id()) && eligible(student,seat,spaced) && seat.accessible()==accessibleOnly) return seat;
        return null;
    }
    public boolean eligible(Student student,Seat seat,boolean spaced) { return (!spaced || seat.column()%2==1) && (!student.accessible() || seat.accessible()); }
    private int compare(Student a,Student b) { if(a.accessible()!=b.accessible()) return a.accessible()?-1:1; int v=a.roll().compareToIgnoreCase(b.roll()); return v!=0?v:Integer.compare(a.id(),b.id()); }
    private int compare(Seat a,Seat b) { int v=a.room().compareToIgnoreCase(b.room()); if(v==0) v=Integer.compare(a.roomId(),b.roomId()); if(v==0) v=Integer.compare(a.row(),b.row()); if(v==0) v=Integer.compare(a.column(),b.column()); return v; }
}
