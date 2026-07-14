package com.ngo.security;

public class JwtDetails {
    private String userId;
    private String role;
    private String name;

    public JwtDetails() {}

    public JwtDetails(String userId, String role, String name) {
        this.userId = userId;
        this.role = role;
        this.name = name;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
