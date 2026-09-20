package com.examplanner.model;

public class Exam {
    private final int id;
    private final int courseId;
    private final String courseCode;
    private final String courseName;
    private final String examDate;
    private final String startTime;
    private final String endTime;
    private final String status;
    private final String rooms;

    public Exam(int id, int courseId, String courseCode, String courseName, String examDate,
                String startTime, String endTime, String status, String rooms) {
        this.id = id;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.examDate = examDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.rooms = rooms;
    }

    public int getId() { return id; }
    public int getCourseId() { return courseId; }
    public String getCourseCode() { return courseCode; }
    public String getCourseName() { return courseName; }
    public String getExamDate() { return examDate; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getStatus() { return status; }
    public String getRooms() { return rooms; }
}
