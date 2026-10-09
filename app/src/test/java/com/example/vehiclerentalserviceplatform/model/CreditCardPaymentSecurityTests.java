package com.example.vehiclerentalserviceplatform.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditCardPaymentSecurityTests {

    @Test
    void retainsOnlyLastFourAndHidesSubmittedCardFieldsFromJson() throws Exception {
        CreditCardPayment payment = new CreditCardPayment();
        payment.setCardNumber("4111111111111111");
        payment.setCardHolder("Test Customer");
        payment.setExpiryDate("12/40");
        payment.setCvv("123");

        payment.retainSafeReference();

        assertNull(payment.getCardNumber());
        assertNull(payment.getCardHolder());
        assertNull(payment.getExpiryDate());
        assertNull(payment.getCvv());

        String json = new ObjectMapper().writeValueAsString(payment);
        assertTrue(json.contains("\"cardLastFour\":\"1111\""));
        assertFalse(json.contains("4111111111111111"));
        assertFalse(json.contains("Test Customer"));
        assertFalse(json.contains("12/40"));
        assertFalse(json.contains("123"));
        assertFalse(json.toLowerCase().contains("cvv"));
    }
}
