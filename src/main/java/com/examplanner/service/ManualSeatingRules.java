package com.examplanner.service;

import com.examplanner.service.SeatingAllocator.Seat;
import com.examplanner.service.SeatingAllocator.Student;
import java.util.*;

/** Validates both sides of a manual change before persistence writes any rows. */
public class ManualSeatingRules {
    private final SeatingAllocator allocator = new SeatingAllocator();

    public Map<Integer,Integer> changes(List<Student> students, List<Seat> seats,
            Map<Integer,Integer> assignments, int studentId, int targetSeatId,
            boolean swap, boolean spaced) {
        Map<Integer,Student> roster = new HashMap<>();
        Map<Integer,Seat> layout = new HashMap<>();
        for (Student student : students) roster.put(student.id(), student);
        for (Seat seat : seats) layout.put(seat.id(), seat);
        Student student = roster.get(studentId);
        Seat target = layout.get(targetSeatId);
        if (student == null) throw new IllegalArgumentException("Student is not enrolled in this examination");
        if (target == null) throw new IllegalArgumentException("Target seat is outside this examination's rooms");
        Integer oldSeatId = assignments.get(studentId);
        if (oldSeatId == null) throw new IllegalArgumentException("Student has no current seat assignment");
        Seat oldSeat = layout.get(oldSeatId);
        if (oldSeat == null) throw new IllegalArgumentException("Current seat is outside this examination's rooms");
        if (oldSeatId == targetSeatId) throw new IllegalArgumentException("Choose a different seat");
        if (!allocator.eligible(student, target, spaced))
            throw new IllegalArgumentException("Target seat violates spacing or the student's accommodation requirement");

        Integer occupant = null;
        for (var entry : assignments.entrySet()) {
            if (entry.getValue() == targetSeatId) {
                if (occupant != null) throw new IllegalArgumentException("Target seat has duplicate assignments");
                occupant = entry.getKey();
            }
        }
        if (occupant != null && !swap) throw new IllegalArgumentException("Target seat is occupied; choose Swap");
        if (occupant == null && swap) throw new IllegalArgumentException("Swap requires an occupied target seat");
        Map<Integer,Integer> result = new LinkedHashMap<>();
        result.put(studentId, targetSeatId);
        if (occupant != null) {
            Student other = roster.get(occupant);
            if (other == null || !allocator.eligible(other, oldSeat, spaced))
                throw new IllegalArgumentException("Swap would violate enrollment, spacing or the other student's accommodation requirement");
            result.put(occupant, oldSeatId);
        }
        return result;
    }
}
