package com.example.vehicalrentalserviceplatform.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("cash")
public class CashPayment implements PaymentMethod {

    private String receiptNumber;

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    @Override
    public String getType() {
        return "cash";
    }

    @Override
    public void validate() {
        if (receiptNumber == null || receiptNumber.isBlank()) {
            throw new IllegalArgumentException("Receipt number is required for cash payments.");
        }
    }
}
