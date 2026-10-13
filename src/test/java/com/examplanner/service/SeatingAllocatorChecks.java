package com.examplanner.service;
import com.examplanner.service.SeatingAllocator.*;
import java.util.*;

public class SeatingAllocatorChecks {
    private static int passed;
    private static final SeatingAllocator allocator=new SeatingAllocator();
    private static void check(boolean condition,String name) { if(!condition) throw new AssertionError(name); passed++; System.out.println("PASS "+name); }
    private static void rejects(Runnable work,String name) { try { work.run(); } catch(IllegalArgumentException e) { check(true,name); return; } throw new AssertionError(name); }
    public static void main(String[] args) {
        Student a=new Student(1,"A001",false),b=new Student(2,"A002",false),pwd=new Student(3,"A003",true);
        List<Seat> seats=List.of(new Seat(10,1,"R101",1,1,true),new Seat(11,1,"R101",1,2,false),new Seat(12,1,"R101",1,3,false),new Seat(13,2,"R102",1,1,true));
        Map<Integer,Integer> allocation=allocator.allocate(List.of(b,pwd,a),seats,Map.of(),false);
        check(allocation.size()==3,"all students allocated");
        check(allocation.get(3)==10,"accommodation first");
        check(allocation.get(1)==11 && allocation.get(2)==12,"worked example deterministic roll ordering");
        check(new HashSet<>(allocation.values()).size()==3,"unique assigned seats");
        check(allocator.allocate(List.of(a,b),seats,Map.of(1,12),false).get(1)==12,"preserve manual assignment");
        check(allocator.allocate(List.of(a,b),seats,Map.of(),true).get(2)==12,"horizontal spacing uses odd columns");
        rejects(()->allocator.allocate(List.of(a,b,pwd,new Student(4,"A004",false),new Student(5,"A005",false)),seats,Map.of(),false),"capacity shortfall rejected");
        rejects(()->allocator.allocate(List.of(pwd),List.of(seats.get(1)),Map.of(),false),"missing accessible seats rejected");
        rejects(()->allocator.allocate(List.of(a,b),seats,Map.of(1,10,2,10),false),"duplicate pinned seat rejected");
        rejects(()->allocator.allocate(List.of(pwd),seats,Map.of(3,11),false),"invalid accommodation pin rejected");
        rejects(()->allocator.allocate(List.of(a),seats,Map.of(1,11),true),"spacing pin rejected");
        rejects(()->allocator.allocate(List.of(a),seats,Map.of(8,10),false),"unknown student pin rejected");
        rejects(()->allocator.allocate(List.of(a,a),seats,Map.of(),false),"duplicate student input rejected");
        rejects(()->allocator.allocate(List.of(a),List.of(seats.get(0),seats.get(0)),Map.of(),false),"duplicate seat input rejected");
        check(allocator.allocate(List.of(),seats,Map.of(),false).isEmpty(),"empty input");
        List<Student> load=new ArrayList<>(); List<Seat> capacity=new ArrayList<>();
        for(int i=1;i<=2000;i++) { load.add(new Student(i,String.format("ROLL%05d",i),i<=20)); capacity.add(new Seat(i,(i-1)/100+1,String.format("R%03d",(i-1)/100+1),(i-1)%100/10+1,(i-1)%10+1,i<=20)); }
        Collections.reverse(load); long start=System.nanoTime(); Map<Integer,Integer> large=allocator.allocate(load,capacity,Map.of(),false); long ms=(System.nanoTime()-start)/1_000_000;
        check(large.size()==2000&&new HashSet<>(large.values()).size()==2000,"2000-student load, unique full allocation ("+ms+" ms)");
        Collections.reverse(capacity); check(large.equals(allocator.allocate(load,capacity,Map.of(),false)),"input permutation does not change allocation");
        System.out.println("RESULT "+passed+" checks passed");
    }
}
