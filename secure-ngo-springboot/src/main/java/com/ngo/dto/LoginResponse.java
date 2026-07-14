package com.ngo.dto;

public class LoginResponse {
    private String token;
    private Long userId;
    private String name;
    private String role;
    private String email;

    public LoginResponse() {}

    public LoginResponse(String token, Long userId, String name, String role, String email) {
        this.token = token;
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.email = email;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
