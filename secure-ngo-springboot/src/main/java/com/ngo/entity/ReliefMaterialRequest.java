package com.ngo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "relief_material_requests")
public class ReliefMaterialRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long beneficiaryId;
    @Column(nullable = false, length = 50) private String materialType;
    @Column(nullable = false) private Integer quantity;
    @Column(nullable = false, length = 20) private String urgency;
    @Column(nullable = false, length = 250) private String deliveryAddress;
    @Column(length = 500) private String notes;
    @Column(nullable = false, length = 30) private String status = "pending";
    @Column(nullable = false) private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    @Transient private String beneficiaryName;
    @Transient private Long donatedQuantity = 0L;

    public Long getId() { return id; }
    public Long getBeneficiaryId() { return beneficiaryId; }
    public void setBeneficiaryId(Long beneficiaryId) { this.beneficiaryId = beneficiaryId; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getUrgency() { return urgency; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; this.updatedAt = LocalDateTime.now(); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    public Long getDonatedQuantity() { return donatedQuantity; }
    public void setDonatedQuantity(Long donatedQuantity) { this.donatedQuantity = donatedQuantity; }
}
