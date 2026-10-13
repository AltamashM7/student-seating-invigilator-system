package com.examplanner.service;

import com.examplanner.service.SeatingAllocator.*;
import java.util.*;

public class ManualSeatingRulesChecks {
    private static int passed;
    private static final ManualSeatingRules rules = new ManualSeatingRules();
    private static final List<Student> students = List.of(new Student(1,"A001",false),new Student(2,"A002",false),new Student(3,"A003",true));
    private static final List<Seat> seats = List.of(new Seat(10,1,"R101",1,1,true),new Seat(11,1,"R101",1,2,false),new Seat(12,1,"R101",1,3,false),new Seat(13,1,"R101",2,1,true));
    private static final Map<Integer,Integer> assignments = Map.of(1,11,2,12,3,10);
    private static void check(boolean condition,String name) { if(!condition)throw new AssertionError(name);passed++;System.out.println("PASS "+name); }
    private static void rejects(Runnable work,String name) {try{work.run();}catch(IllegalArgumentException e){check(true,name);return;}throw new AssertionError(name);}
    public static void main(String[] args) {
        check(rules.changes(students,seats,assignments,1,13,false,false).equals(Map.of(1,13)),"move produces exactly one changed assignment");
        check(rules.changes(students,seats,assignments,1,12,true,false).equals(Map.of(1,12,2,11)),"swap produces both changed assignments");
        check(assignments.equals(Map.of(1,11,2,12,3,10)),"validation does not mutate the saved assignment input");
        rejects(()->rules.changes(students,seats,assignments,99,13,false,false),"unenrolled student rejected");
        rejects(()->rules.changes(students,seats,Map.of(2,12,3,10),1,13,false,false),"unassigned student rejected");
        rejects(()->rules.changes(students,seats,assignments,1,99,false,false),"seat outside examination rejected");
        rejects(()->rules.changes(students,seats,Map.of(1,99),1,13,false,false),"invalid source seat rejected");
        rejects(()->rules.changes(students,seats,assignments,1,11,false,false),"no-op reassignment rejected");
        rejects(()->rules.changes(students,seats,assignments,1,12,false,false),"move into occupied seat rejected");
        rejects(()->rules.changes(students,seats,assignments,1,13,true,false),"swap into empty seat rejected");
        rejects(()->rules.changes(students,seats,assignments,3,12,true,false),"accommodation student's inaccessible target rejected");
        rejects(()->rules.changes(students,seats,assignments,1,10,true,false),"swap protects the other student's accessibility");
        rejects(()->rules.changes(students,seats,Map.of(1,12,3,10),1,11,false,true),"move to reserved spacing column rejected");
        rejects(()->rules.changes(students,seats,assignments,1,12,true,true),"swap protects the other student's spacing");
        rejects(()->rules.changes(students,seats,Map.of(1,11,2,12,99,13),1,13,true,false),"swap with unenrolled occupant rejected");
        rejects(()->rules.changes(students,seats,Map.of(1,11,2,12,3,12),1,12,true,false),"duplicate destination occupants rejected");
        check(rules.changes(students,seats,Map.of(1,12,3,10),3,13,false,true).equals(Map.of(3,13)),"accessible move in spaced layout succeeds");
        System.out.println("RESULT "+passed+" manual rule checks passed");
    }
}
