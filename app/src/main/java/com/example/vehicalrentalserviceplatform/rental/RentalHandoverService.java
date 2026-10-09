package com.example.vehicalrentalserviceplatform.rental;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RentalHandoverService {
    private static final String SEPARATOR = "|";
    private final Path storageFile;
    private final Path activityFile;

    public RentalHandoverService(
            @Value("${rental.storage.file:rentals.txt}") String storageFile,
            @Value("${rental.activity.file:rental-activity.log}") String activityFile) {
        this.storageFile = Paths.get(storageFile).toAbsolutePath();
        this.activityFile = Paths.get(activityFile).toAbsolutePath();
    }

    public synchronized List<RentalHandover> getAll() {
        ensureFile(storageFile);
        List<RentalHandover> records = new ArrayList<>();
        try {
            for (String line : Files.readAllLines(storageFile, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    records.add(parse(line));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read rental records.", e);
        }
        return records;
    }

    public synchronized Optional<RentalHandover> findById(String recordId) {
        return getAll().stream().filter(record -> record.getRecordId().equals(recordId)).findFirst();
    }

    public synchronized RentalHandover create(String bookingId, String customerName,
                                               String vehicleId, LocalDate scheduledReturnDate,
                                               double odometerOut, String fuelLevelOut,
                                               String conditionOut, String actor) {
        requireText(bookingId, "Booking is required.");
        requireText(customerName, "Customer name is required.");
        requireText(vehicleId, "Vehicle is required.");
        requireText(fuelLevelOut, "Outgoing fuel level is required.");
        requireText(conditionOut, "Outgoing vehicle condition is required.");
        validateNonNegative(odometerOut, "Outgoing odometer cannot be negative.");
        if (scheduledReturnDate == null) {
            throw new IllegalArgumentException("Scheduled return date is required.");
        }

        List<RentalHandover> records = getAll();
        boolean duplicate = records.stream().anyMatch(record -> record.isActive()
                && (record.getBookingId().equals(bookingId) || record.getVehicleId().equals(vehicleId)));
        if (duplicate) {
            throw new IllegalArgumentException("This booking or vehicle already has an active handover.");
        }

        RentalHandover record = new RentalHandover(
                "RENT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                clean(bookingId), clean(customerName), clean(vehicleId), scheduledReturnDate,
                LocalDateTime.now(), odometerOut, clean(fuelLevelOut), clean(conditionOut),
                RentalHandover.ACTIVE, null, null, "", "", 0.0);
        records.add(record);
        saveAll(records);
        log(actor, "CREATE", record.getRecordId() + " handed over vehicle " + record.getVehicleId());
        return record;
    }

    public synchronized RentalHandover update(String recordId, LocalDate scheduledReturnDate,
                                               double odometerOut, String fuelLevelOut,
                                               String conditionOut, String actor) {
        validateNonNegative(odometerOut, "Outgoing odometer cannot be negative.");
        requireText(fuelLevelOut, "Outgoing fuel level is required.");
        requireText(conditionOut, "Outgoing vehicle condition is required.");
        if (scheduledReturnDate == null) {
            throw new IllegalArgumentException("Scheduled return date is required.");
        }
        List<RentalHandover> records = getAll();
        RentalHandover record = requireRecord(records, recordId);
        if (!record.isActive()) {
            throw new IllegalArgumentException("Only active handovers can be updated.");
        }
        record.updateHandover(scheduledReturnDate, odometerOut, clean(fuelLevelOut), clean(conditionOut));
        saveAll(records);
        log(actor, "UPDATE", "Updated handover " + recordId);
        return record;
    }

    public synchronized RentalHandover completeReturn(String recordId, double odometerIn,
                                                       String fuelLevelIn, String conditionIn,
                                                       double lateFee, String actor) {
        validateNonNegative(odometerIn, "Return odometer cannot be negative.");
        validateNonNegative(lateFee, "Late fee cannot be negative.");
        requireText(fuelLevelIn, "Return fuel level is required.");
        requireText(conditionIn, "Return vehicle condition is required.");
        List<RentalHandover> records = getAll();
        RentalHandover record = requireRecord(records, recordId);
        if (!record.isActive()) {
            throw new IllegalArgumentException("This rental has already been returned.");
        }
        if (odometerIn < record.getOdometerOut()) {
            throw new IllegalArgumentException("Return odometer cannot be lower than handover odometer.");
        }
        record.completeReturn(LocalDateTime.now(), odometerIn, clean(fuelLevelIn), clean(conditionIn), lateFee);
        saveAll(records);
        log(actor, "RETURN", recordId + " returned vehicle " + record.getVehicleId());
        return record;
    }

    public synchronized RentalHandover delete(String recordId, String actor) {
        List<RentalHandover> records = getAll();
        RentalHandover record = requireRecord(records, recordId);
        records.remove(record);
        saveAll(records);
        log(actor, "DELETE", "Deleted rental record " + recordId);
        return record;
    }

    private RentalHandover requireRecord(List<RentalHandover> records, String recordId) {
        return records.stream().filter(record -> record.getRecordId().equals(recordId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Rental record was not found."));
    }

    private void saveAll(List<RentalHandover> records) {
        ensureFile(storageFile);
        List<String> lines = records.stream().map(this::serialize).toList();
        try {
            Files.write(storageFile, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save rental records.", e);
        }
    }

    private String serialize(RentalHandover record) {
        return String.join(SEPARATOR,
                record.getRecordId(), record.getBookingId(), record.getCustomerName(), record.getVehicleId(),
                record.getScheduledReturnDate().toString(), record.getHandedOverAt().toString(),
                Double.toString(record.getOdometerOut()), record.getFuelLevelOut(), record.getConditionOut(),
                record.getStatus(), value(record.getReturnedAt()), value(record.getOdometerIn()),
                value(record.getFuelLevelIn()), value(record.getConditionIn()), Double.toString(record.getLateFee()));
    }

    private RentalHandover parse(String line) {
        String[] part = line.split("\\|", -1);
        if (part.length != 15) {
            throw new IllegalStateException("Invalid rental record: " + line);
        }
        return new RentalHandover(part[0], part[1], part[2], part[3], LocalDate.parse(part[4]),
                LocalDateTime.parse(part[5]), Double.parseDouble(part[6]), part[7], part[8], part[9],
                part[10].isBlank() ? null : LocalDateTime.parse(part[10]),
                part[11].isBlank() ? null : Double.parseDouble(part[11]), part[12], part[13],
                Double.parseDouble(part[14]));
    }

    private void log(String actor, String action, String details) {
        ensureFile(activityFile);
        String line = LocalDateTime.now() + SEPARATOR + clean(actor) + SEPARATOR + action + SEPARATOR + clean(details);
        try {
            Files.writeString(activityFile, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write rental activity log.", e);
        }
    }

    private void ensureFile(Path file) {
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            if (Files.notExists(file)) Files.createFile(file);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to prepare storage file.", e);
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
    }

    private void validateNonNegative(double value, String message) {
        if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException(message);
    }

    private String clean(String value) {
        return value == null ? "" : value.replace('|', '/').replace('\r', ' ').replace('\n', ' ').trim();
    }

    private String value(Object value) { return value == null ? "" : value.toString(); }
}
