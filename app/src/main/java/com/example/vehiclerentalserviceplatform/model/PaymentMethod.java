package com.example.vehiclerentalserviceplatform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = CreditCardPayment.class, name = "creditCard"),
        @JsonSubTypes.Type(value = PaypalPayment.class, name = "paypal"),
        @JsonSubTypes.Type(value = CashPayment.class, name = "cash")
})
public interface PaymentMethod {

    @JsonIgnore
    String getType();

    void validate();
}
