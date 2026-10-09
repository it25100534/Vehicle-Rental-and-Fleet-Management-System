package com.example.vehicalrentalserviceplatform.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("paypal")
public class PaypalPayment implements PaymentMethod {

    private String paypalEmail;

    public String getPaypalEmail() {
        return paypalEmail;
    }

    public void setPaypalEmail(String paypalEmail) {
        this.paypalEmail = paypalEmail;
    }

    @Override
    public String getType() {
        return "paypal";
    }

    @Override
    public void validate() {
        if (paypalEmail == null || !paypalEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("A valid PayPal email address is required.");
        }
    }
}
