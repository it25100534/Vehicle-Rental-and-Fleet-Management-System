package com.example.vehiclerentalserviceplatform.security;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InputValidationTests {

    @Test
    void validatesCustomerRegistrationFields() {
        assertTrue(InputValidation.isUsername("customer_01"));
        assertFalse(InputValidation.isUsername("x"));
        assertTrue(InputValidation.isEmail("customer@example.com"));
        assertFalse(InputValidation.isEmail("customer"));
        assertTrue(InputValidation.isPhone("0771234567"));
        assertFalse(InputValidation.isPhone("771234567"));
        assertTrue(InputValidation.isLicenseId("LIC-TEST-009"));
        assertFalse(InputValidation.isLicenseId("LIC,009"));
        assertTrue(InputValidation.isPassword("drive123"));
        assertFalse(InputValidation.isPassword("123456"));
    }

    @Test
    void validatesBookingDateOrderAndPastDates() {
        LocalDate start = LocalDate.now().plusDays(1);
        assertDoesNotThrow(() -> InputValidation.requireBookingDates(
                start.toString(), start.plusDays(2).toString()));
        assertThrows(IllegalArgumentException.class, () -> InputValidation.requireBookingDates(
                start.toString(), start.toString()));
        assertThrows(IllegalArgumentException.class, () -> InputValidation.requireBookingDates(
                LocalDate.now().minusDays(1).toString(), start.toString()));
    }

    @Test
    void rejectsInvalidVehicleNumbers() {
        assertDoesNotThrow(() -> InputValidation.requireVehicle(
                "CAR", "Toyota", "Corolla", "2020", "Petrol", 7500, 1000,
                5, null, null));
        assertThrows(IllegalArgumentException.class, () -> InputValidation.requireVehicle(
                "CAR", "Toyota", "Corolla", "2020", "Petrol", -1, 1000,
                5, null, null));
        assertThrows(IllegalArgumentException.class, () -> InputValidation.requireVehicle(
                "CAR", "Toyota", "Corolla", "2020", "Petrol", 7500, -1,
                5, null, null));
    }

    @Test
    void preventsMaintenanceStatusMovingBackwards() {
        assertTrue(InputValidation.isForwardMaintenanceTransition("Pending", "In Progress"));
        assertTrue(InputValidation.isForwardMaintenanceTransition("In Progress", "Completed"));
        assertFalse(InputValidation.isForwardMaintenanceTransition("Completed", "Pending"));
        assertFalse(InputValidation.isForwardMaintenanceTransition("Pending", "Unknown"));
    }

    @Test
    void validatesAdminAccountChoicesAndPasswordPolicy() {
        assertDoesNotThrow(() -> InputValidation.requireAdminAccount(
                "Jane Doe", "jane_admin", "ADMIN", "FULL_TIME", "drive123", true));
        assertThrows(IllegalArgumentException.class, () -> InputValidation.requireAdminAccount(
                "Jane Doe", "jane_admin", "SUPERUSER", "FULL_TIME", "drive123", true));
        assertThrows(IllegalArgumentException.class, () -> InputValidation.requireAdminAccount(
                "Jane Doe", "jane_admin", "ADMIN", "FULL_TIME", "password", true));
    }

    @Test
    void validatesHandoverAndReturnMeasurements() {
        assertDoesNotThrow(() -> InputValidation.requireHandover(
                LocalDate.now().plusDays(2), 12000, "FULL", "No visible damage"));
        assertThrows(IllegalArgumentException.class, () -> InputValidation.requireHandover(
                LocalDate.now().plusDays(2), -1, "FULL", "No visible damage"));
        assertThrows(IllegalArgumentException.class, () -> InputValidation.requireReturn(
                12500, "INVALID", "Good", 0));
        assertThrows(IllegalArgumentException.class, () -> InputValidation.requireReturn(
                12500, "HALF", "Good", -1));
    }
}
