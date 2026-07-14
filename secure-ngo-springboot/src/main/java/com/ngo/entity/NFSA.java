package com.ngo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "nfsa")
public class NFSA {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(unique = true, nullable = false, length = 120)
    private String email;

    @Column(nullable = false, length = 256)
    private String passwordHash;

    @Column(nullable = false, length = 150)
    private String organizationName;

    @Column(nullable = false, length = 100)
    private String designation;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 50)
    private String employeeId;

    @Column(nullable = false)
    private Boolean isVerified = true;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public NFSA() {}

    public NFSA(String fullName, String email, String passwordHash, String organizationName, String designation, String phone, String employeeId) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.organizationName = organizationName;
        this.designation = designation;
        this.phone = phone;
        this.employeeId = employeeId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
