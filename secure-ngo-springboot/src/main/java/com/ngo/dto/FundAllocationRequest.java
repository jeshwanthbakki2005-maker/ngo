package com.ngo.dto;

public class FundAllocationRequest {
    private Long donationId;
    private Long beneficiaryId;
    private Double amount;
    private String purpose;

    public FundAllocationRequest() {}

    public FundAllocationRequest(Long donationId, Long beneficiaryId, Double amount, String purpose) {
        this.donationId = donationId;
        this.beneficiaryId = beneficiaryId;
        this.amount = amount;
        this.purpose = purpose;
    }

    public Long getDonationId() { return donationId; }
    public void setDonationId(Long donationId) { this.donationId = donationId; }

    public Long getBeneficiaryId() { return beneficiaryId; }
    public void setBeneficiaryId(Long beneficiaryId) { this.beneficiaryId = beneficiaryId; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
