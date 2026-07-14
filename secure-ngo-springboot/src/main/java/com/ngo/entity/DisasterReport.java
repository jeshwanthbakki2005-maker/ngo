package com.ngo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "disaster_reports")
public class DisasterReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String disasterType;

    @Column(nullable = false, length = 150)
    private String location;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(nullable = false)
    private Integer familiesAffected;

    @Column(nullable = false)
    private LocalDateTime disasterDate;

    @Column(nullable = false, length = 20)
    private String contactNumber;

    @Column(nullable = false, length = 20)
    private String severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String additionalNotes;

    @Column(nullable = false, length = 20)
    private String status = "pending"; // pending, active, resolved

    @Column
    private Long reportedByBeneficiaryId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(columnDefinition = "TEXT")
    private String photoBase64;

    @Column
    private Double requiredAmount;

    @Transient
    private Double amountRaised = 0.0;

    @Transient
    private String beneficiaryName;

    // ─── Constructors ──────────────────────────────────────────────────────────

    public DisasterReport() {}

    // ─── Getters & Setters ──────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDisasterType() { return disasterType; }
    public void setDisasterType(String disasterType) { this.disasterType = disasterType; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Integer getFamiliesAffected() { return familiesAffected; }
    public void setFamiliesAffected(Integer familiesAffected) { this.familiesAffected = familiesAffected; }

    public LocalDateTime getDisasterDate() { return disasterDate; }
    public void setDisasterDate(LocalDateTime disasterDate) { this.disasterDate = disasterDate; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getReportedByBeneficiaryId() { return reportedByBeneficiaryId; }
    public void setReportedByBeneficiaryId(Long reportedByBeneficiaryId) { this.reportedByBeneficiaryId = reportedByBeneficiaryId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPhotoBase64() { return photoBase64; }
    public void setPhotoBase64(String photoBase64) { this.photoBase64 = photoBase64; }

    public Double getRequiredAmount() { return requiredAmount; }
    public void setRequiredAmount(Double requiredAmount) { this.requiredAmount = requiredAmount; }

    public Double getAmountRaised() { return amountRaised; }
    public void setAmountRaised(Double amountRaised) { this.amountRaised = amountRaised; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
}
