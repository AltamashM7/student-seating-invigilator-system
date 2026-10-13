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
        Map<Integer,Integer> priority=allocator.allocate(List.of(a),List.of(new Seat(20,1,"R100",1,1,true),new Seat(21,1,"R100",1,2,false)),Map.of(),false);
        check(priority.get(1)==21,"accessible seats are reserved while ordinary capacity is sufficient");
        Map<Integer,Integer> fallback=allocator.allocate(List.of(a,pwd),List.of(new Seat(22,1,"R100",1,1,true),new Seat(23,1,"R100",1,2,true)),Map.of(),false);
        check(fallback.get(3)==22&&fallback.get(1)==23,"ordinary students use extra accessible seats only as capacity fallback");
        check(allocator.allocate(List.of(a,b),seats,Map.of(1,12),false).get(1)==12,"preserve manual assignment");
        Map<Integer,Integer> spaced=allocator.allocate(List.of(a,b),seats,Map.of(),true);
        check(spaced.values().stream().allMatch(id->seats.stream().filter(s->s.id()==id).findFirst().orElseThrow().column()%2==1),"horizontal spacing uses odd columns");
        rejects(()->allocator.allocate(List.of(a,b,pwd,new Student(4,"A004",false),new Student(5,"A005",false)),seats,Map.of(),false),"capacity shortfall rejected");
        rejects(()->allocator.allocate(List.of(pwd),List.of(seats.get(1)),Map.of(),false),"missing accessible seats rejected");
        rejects(()->allocator.allocate(List.of(a,b),seats,Map.of(1,10,2,10),false),"duplicate pinned seat rejected");
        rejects(()->allocator.allocate(List.of(pwd),seats,Map.of(3,11),false),"invalid accommodation pin rejected");
        rejects(()->allocator.allocate(List.of(a,pwd),List.of(seats.get(0),seats.get(1)),Map.of(1,10),false),"preserved regular pin cannot silently displace an accommodation student");
        check(allocator.allocate(List.of(a,pwd),List.of(seats.get(0),seats.get(1)),Map.of(),false).get(3)==10,"full replacement restores accommodation capacity");
        rejects(()->allocator.allocate(List.of(pwd,new Student(4,"A004",true)),List.of(seats.get(0),seats.get(1)),Map.of(),false),"multiple accommodation students cannot share one qualifying seat");
        List<Seat> irregular=List.of(new Seat(30,1,"R101",1,1,false),new Seat(31,1,"R101",1,2,false),new Seat(32,1,"R101",2,1,true));
        check(allocator.allocate(List.of(a,pwd),irregular,Map.of(),true).equals(Map.of(1,30,3,32)),"partial last row respects coordinates and accessibility with spacing");
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
