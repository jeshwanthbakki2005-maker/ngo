package com.ngo.dto;

public class PasswordResetRequestDTO {
    private String email;
    private String userType;

    public PasswordResetRequestDTO() {}

    public PasswordResetRequestDTO(String email, String userType) {
        this.email = email;
        this.userType = userType;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
}
