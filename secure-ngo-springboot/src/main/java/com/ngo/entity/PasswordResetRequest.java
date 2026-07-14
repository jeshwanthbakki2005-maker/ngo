package com.ngo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_requests")
public class PasswordResetRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, length = 50)
    private String userType;

    @Column(nullable = false, length = 50)
    private String status = "pending";

    @Column(length = 255)
    private String token;

    @Column()
    private LocalDateTime tokenGeneratedAt;

    @Column()
    private LocalDateTime approvedAt;

    @Column(length = 100)
    private String otpHash;

    @Column()
    private LocalDateTime otpExpiresAt;

    @Column()
    private LocalDateTime otpVerifiedAt;

    @Column
    private Integer otpAttempts = 0;

    @Transient
    private String otpCode;

    @Column()
    private Long userId;

    @Column()
    private LocalDateTime completedAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public PasswordResetRequest() {}

    public PasswordResetRequest(String email, String userType) {
        this.email = email;
        this.userType = userType;
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getTokenGeneratedAt() { return tokenGeneratedAt; }
    public void setTokenGeneratedAt(LocalDateTime tokenGeneratedAt) { this.tokenGeneratedAt = tokenGeneratedAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }

    public LocalDateTime getOtpExpiresAt() { return otpExpiresAt; }
    public void setOtpExpiresAt(LocalDateTime otpExpiresAt) { this.otpExpiresAt = otpExpiresAt; }

    public LocalDateTime getOtpVerifiedAt() { return otpVerifiedAt; }
    public void setOtpVerifiedAt(LocalDateTime otpVerifiedAt) { this.otpVerifiedAt = otpVerifiedAt; }

    public Integer getOtpAttempts() { return otpAttempts; }
    public void setOtpAttempts(Integer otpAttempts) { this.otpAttempts = otpAttempts; }

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
