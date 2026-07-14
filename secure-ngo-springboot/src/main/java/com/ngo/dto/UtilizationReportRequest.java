package com.ngo.dto;

public class UtilizationReportRequest {
    private Long allocationId;
    private Double amountUsed;
    private String description;
    private String receiptReference;

    public UtilizationReportRequest() {}

    public UtilizationReportRequest(Long allocationId, Double amountUsed, String description, String receiptReference) {
        this.allocationId = allocationId;
        this.amountUsed = amountUsed;
        this.description = description;
        this.receiptReference = receiptReference;
    }

    public Long getAllocationId() { return allocationId; }
    public void setAllocationId(Long allocationId) { this.allocationId = allocationId; }

    public Double getAmountUsed() { return amountUsed; }
    public void setAmountUsed(Double amountUsed) { this.amountUsed = amountUsed; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReceiptReference() { return receiptReference; }
    public void setReceiptReference(String receiptReference) { this.receiptReference = receiptReference; }
}
