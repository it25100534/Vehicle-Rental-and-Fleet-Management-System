package com.example.vehicalrentalserviceplatform.model;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("creditCard")
public class CreditCardPayment implements PaymentMethod {

    private String cardNumber;
    private String cardHolder;
    private String expiryDate;
    private String cvv;

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardHolder() {
        return cardHolder;
    }

    public void setCardHolder(String cardHolder) {
        this.cardHolder = cardHolder;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }

    @Override
    public String getType() {
        return "creditCard";
    }

    @Override
    public void validate() {
        if (cardNumber == null || !cardNumber.matches("\\d{13,19}")) {
            throw new IllegalArgumentException("Credit card number must be 13-19 digits.");
        }
        if (cardHolder == null || cardHolder.isBlank()) {
            throw new IllegalArgumentException("Card holder name is required.");
        }
        if (expiryDate == null || !expiryDate.matches("(0[1-9]|1[0-2])/\\d{2}")) {
    throw new IllegalArgumentException("Expiry date must be in MM/YY format.");
    }

    try {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yy");
        YearMonth expiryMonth = YearMonth.parse(expiryDate, formatter);
        YearMonth currentMonth = YearMonth.now();

        if (expiryMonth.isBefore(currentMonth)) {
            throw new IllegalArgumentException("Expiry date must not be expired.");
        }
    } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Expiry date must be in MM/YY format.");
    }

        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            throw new IllegalArgumentException("CVV must be 3 or 4 digits.");
        }
    }
}
