package com.ngo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "allocations")
public class Allocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long donationId;

    @Column(nullable = false)
    private Long beneficiaryId;

    @Column(nullable = false)
    private Double amount;

    @Column(length = 500)
    private String purpose;

    @Column(nullable = false, length = 50)
    private String status = "allocated";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column()
    private Long approvedBy;

    @Column()
    private LocalDateTime disbursedAt;

    @Transient private String beneficiaryName;
    @Transient private String donationPurpose;

    public Allocation() {}

    public Allocation(Long donationId, Long beneficiaryId, Double amount, String purpose) {
        this.donationId = donationId;
        this.beneficiaryId = beneficiaryId;
        this.amount = amount;
        this.purpose = purpose;
        this.status = "allocated";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDonationId() { return donationId; }
    public void setDonationId(Long donationId) { this.donationId = donationId; }

    public Long getBeneficiaryId() { return beneficiaryId; }
    public void setBeneficiaryId(Long beneficiaryId) { this.beneficiaryId = beneficiaryId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Long approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getDisbursedAt() { return disbursedAt; }
    public void setDisbursedAt(LocalDateTime disbursedAt) { this.disbursedAt = disbursedAt; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    public String getDonationPurpose() { return donationPurpose; }
    public void setDonationPurpose(String donationPurpose) { this.donationPurpose = donationPurpose; }
}
