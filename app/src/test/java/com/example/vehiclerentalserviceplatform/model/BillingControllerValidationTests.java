package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.model.Booking;
import com.example.vehiclerentalserviceplatform.model.Car;
import com.example.vehiclerentalserviceplatform.model.Invoice;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BillingControllerValidationTests {

    @Test
    void calculatesInvoiceIdentityDurationAndRateFromServerData() {
        LocalDate start = LocalDate.now().plusDays(1);
        Booking booking = new Booking("TX-1", "real-customer", "CAR-1",
                start.toString(), start.plusDays(3).toString(), "Approved");
        Car vehicle = new Car("Toyota", "Corolla", "2020", 7500,
                "Petrol", 1000, true, 5, "car.png", "CAR-1");

        Invoice submitted = new Invoice();
        submitted.setTransactionId("TX-1");
        submitted.setCustomerName("forged-customer");
        submitted.setVehicleId("forged-vehicle");
        submitted.setVehicleName("forged-name");
        submitted.setRentalDays(99);
        submitted.setAmountPerDay(1.0);

        BillingController.applyServerInvoiceData(submitted, "real-customer", booking, vehicle);

        assertEquals("real-customer", submitted.getCustomerName());
        assertEquals("CAR-1", submitted.getVehicleId());
        assertEquals("Toyota Corolla", submitted.getVehicleName());
        assertEquals(3, submitted.getRentalDays());
        assertEquals(7500.0, submitted.getAmountPerDay());
    }

    @Test
    void includesReturnChargesDepositsAndRefundsInInvoiceTotal() {
        Invoice invoice = new Invoice();
        invoice.setRentalDays(2);
        invoice.setAmountPerDay(5000.0);
        invoice.applyReturnAdjustments(500, 2000, 750, 300, 400, 5000, 1500);

        assertEquals(17450.0, invoice.getTotalAmount());
    }
}
