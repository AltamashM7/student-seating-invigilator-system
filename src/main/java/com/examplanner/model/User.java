package com.examplanner.model;

public class User {
    private final int id;
    private final String username;
    private final String passwordHash;
    private final String role;
    private final String displayName;
    private final String email;

    public User(int id, String username, String passwordHash, String role, String displayName, String email) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
        this.email = email;
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
}
