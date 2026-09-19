package com.example.vehicalrentalserviceplatform.rental;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RentalHandoverServiceTests {
    @TempDir
    Path tempDir;

    @Test
    void completesTheFullHandoverCrudFlow() {
        RentalHandoverService service = service();

        RentalHandover created = service.create("BOOK-1", "Test Customer", "CAR-1",
                LocalDate.now().plusDays(3), 12500, "FULL", "No visible damage", "tester");
        assertEquals(1, service.getAll().size());
        assertTrue(created.isActive());

        RentalHandover updated = service.update(created.getRecordId(), LocalDate.now().plusDays(4),
                12510, "THREE_QUARTERS", "Small mark on rear bumper", "tester");
        assertEquals(12510, updated.getOdometerOut());

        RentalHandover returned = service.completeReturn(created.getRecordId(), 12725,
                "HALF", "Returned with existing mark", 500, "tester");
        assertFalse(returned.isActive());
        assertEquals(RentalHandover.RETURNED, returned.getStatus());
        assertEquals(12725, returned.getOdometerIn());

        service.delete(created.getRecordId(), "tester");
        assertTrue(service.getAll().isEmpty());
    }

    @Test
    void rejectsDuplicateActiveHandoverAndInvalidReturnMileage() {
        RentalHandoverService service = service();
        RentalHandover created = service.create("BOOK-1", "Test Customer", "CAR-1",
                LocalDate.now().plusDays(3), 5000, "FULL", "Good", "tester");

        assertThrows(IllegalArgumentException.class, () -> service.create("BOOK-2", "Another Customer", "CAR-1",
                LocalDate.now().plusDays(2), 100, "FULL", "Good", "tester"));
        assertThrows(IllegalArgumentException.class, () -> service.completeReturn(created.getRecordId(), 4999,
                "FULL", "Good", 0, "tester"));
    }

    private RentalHandoverService service() {
        return new RentalHandoverService(
                tempDir.resolve("rentals.txt").toString(),
                tempDir.resolve("rental-activity.log").toString());
    }
}
