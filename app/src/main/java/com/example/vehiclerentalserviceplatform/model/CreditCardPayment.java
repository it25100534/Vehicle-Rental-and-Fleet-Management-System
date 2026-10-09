package com.example.vehiclerentalserviceplatform.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@JsonTypeName("creditCard")
public class CreditCardPayment implements PaymentMethod {

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String cardNumber;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String cardHolder;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String expiryDate;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String cvv;

    private String cardLastFour;

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

    public String getCardLastFour() {
        return cardLastFour;
    }

    public void retainSafeReference() {
        validateSubmittedCard();
        cardLastFour = cardNumber.substring(cardNumber.length() - 4);
        clearSensitiveFields();
    }

    public void loadSafeReference(String lastFour) {
        if (lastFour == null || !lastFour.matches("\\d{4}")) {
            throw new IllegalArgumentException("Stored card reference must contain four digits.");
        }
        cardLastFour = lastFour;
        clearSensitiveFields();
    }

    private void clearSensitiveFields() {
        cardNumber = null;
        cardHolder = null;
        expiryDate = null;
        cvv = null;
    }

    @Override
    public String getType() {
        return "creditCard";
    }

    @Override
    public void validate() {
        if (cardNumber == null && cardLastFour != null) {
            if (!cardLastFour.matches("\\d{4}")) {
                throw new IllegalArgumentException("Stored card reference must contain four digits.");
            }
            return;
        }
        validateSubmittedCard();
    }

    private void validateSubmittedCard() {
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
            if (expiryMonth.isBefore(YearMonth.now())) {
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
