package com.examplanner.model;

public class Room {
    private final int id;
    private final String roomNumber;
    private final int capacity;

    public Room(int id, String roomNumber, int capacity) {
        this.id = id;
        this.roomNumber = roomNumber;
        this.capacity = capacity;
    }

    public int getId() { return id; }
    public String getRoomNumber() { return roomNumber; }
    public int getCapacity() { return capacity; }
}
