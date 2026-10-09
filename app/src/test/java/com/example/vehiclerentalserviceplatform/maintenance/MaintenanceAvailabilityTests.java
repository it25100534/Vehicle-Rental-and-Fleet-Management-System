package com.example.vehiclerentalserviceplatform.maintenance;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceAvailabilityTests {
    @Test
    void pendingOrInProgressServiceBlocksOverlappingBookingDates() {
        List<MaintenanceRecord> records = List.of(
                new MaintenanceRecord("M1", "CAR-0001", "CAR", "Engine", "2030-04-12", "Pending", "Check"),
                new MaintenanceRecord("M2", "CAR-0002", "CAR", "Brake", "2030-04-15", "Completed", "Done"));

        assertThat(MaintenanceFileHandler.hasBookingConflict(records, "CAR-0001",
                LocalDate.parse("2030-04-10"), LocalDate.parse("2030-04-13"))).isTrue();
        assertThat(MaintenanceFileHandler.hasBookingConflict(records, "CAR-0002",
                LocalDate.parse("2030-04-14"), LocalDate.parse("2030-04-16"))).isFalse();
    }

    @Test
    void maintenanceWindowBlocksEveryOverlappingBookingDate() {
        List<MaintenanceRecord> records = List.of(new MaintenanceRecord(
                "M3", "CAR-0003", "CAR", "Engine", "2030-04-12", "2030-04-16",
                "", "City Workshop", 10000, 0, "Pending", "Engine repair"));

        assertThat(MaintenanceFileHandler.hasBookingConflict(records, "CAR-0003",
                LocalDate.parse("2030-04-10"), LocalDate.parse("2030-04-13"))).isTrue();
        assertThat(MaintenanceFileHandler.hasBookingConflict(records, "CAR-0003",
                LocalDate.parse("2030-04-17"), LocalDate.parse("2030-04-18"))).isFalse();
    }

    @Test
    void calculatesDowntimeAndComplianceAlerts() {
        MaintenanceRecord completed = new MaintenanceRecord(
                "M4", "CAR-0004", "CAR", "General", "2030-04-10", "2030-04-12",
                "2030-04-12", "City Workshop", 8000, 7500, "Completed", "Done");
        assertThat(completed.getDowntimeDays()).isEqualTo(3);

        FleetComplianceRecord expired = new FleetComplianceRecord("CAR-0004",
                LocalDate.now().minusDays(1).toString(), LocalDate.now().plusDays(60).toString(),
                LocalDate.now().plusDays(90).toString());
        assertThat(expired.getState()).isEqualTo("EXPIRED");
    }
}
