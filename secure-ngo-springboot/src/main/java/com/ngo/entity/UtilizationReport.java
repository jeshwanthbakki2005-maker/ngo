package com.ngo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "utilization_reports")
public class UtilizationReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long allocationId;

    @Column(nullable = false)
    private Long beneficiaryId;

    @Column(nullable = false)
    private Double amountUsed;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 200)
    private String receiptReference;

    @Column(nullable = false, length = 50)
    private String status = "submitted";

    @Column(nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(length = 100)
    private String verifiedBy;

    @Column()
    private LocalDateTime verifiedAt;

    public UtilizationReport() {}

    public UtilizationReport(Long allocationId, Long beneficiaryId, Double amountUsed, String description, String receiptReference) {
        this.allocationId = allocationId;
        this.beneficiaryId = beneficiaryId;
        this.amountUsed = amountUsed;
        this.description = description;
        this.receiptReference = receiptReference;
        this.status = "submitted";
        this.submittedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAllocationId() { return allocationId; }
    public void setAllocationId(Long allocationId) { this.allocationId = allocationId; }

    public Long getBeneficiaryId() { return beneficiaryId; }
    public void setBeneficiaryId(Long beneficiaryId) { this.beneficiaryId = beneficiaryId; }

    public Double getAmountUsed() { return amountUsed; }
    public void setAmountUsed(Double amountUsed) { this.amountUsed = amountUsed; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReceiptReference() { return receiptReference; }
    public void setReceiptReference(String receiptReference) { this.receiptReference = receiptReference; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}
