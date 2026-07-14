package com.ngo.dto;

public class DonationRequest {
    private Double amount;
    private String purpose;
    private String paymentMethod;

    public DonationRequest() {}

    public DonationRequest(Double amount, String purpose, String paymentMethod) {
        this.amount = amount;
        this.purpose = purpose;
        this.paymentMethod = paymentMethod;
    }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}
