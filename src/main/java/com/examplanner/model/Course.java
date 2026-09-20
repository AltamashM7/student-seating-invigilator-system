package com.examplanner.model;

public class Course {
    private final int id;
    private final String code;
    private final String name;
    private final int departmentId;
    private final String departmentCode;
    private final int semester;

    public Course(int id, String code, String name, int departmentId, String departmentCode, int semester) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.departmentId = departmentId;
        this.departmentCode = departmentCode;
        this.semester = semester;
    }

    public int getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getDepartmentId() { return departmentId; }
    public String getDepartmentCode() { return departmentCode; }
    public int getSemester() { return semester; }
}
