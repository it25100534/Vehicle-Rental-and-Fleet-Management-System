package com.example.vehiclerentalserviceplatform.maintenance;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class MaintenanceFileHandler {
    private static final Path LEGACY_FILE = Path.of("maintenance.txt").toAbsolutePath();
    private final JdbcTemplate jdbc;
    public MaintenanceFileHandler(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostConstruct
    void importLegacyWhenEmpty() {
        Long count = jdbc.queryForObject("select count(*) from maintenance_records", Long.class);
        if (count == null || count != 0 || Files.notExists(LEGACY_FILE)) return;
        try {
            for (String line : Files.readAllLines(LEGACY_FILE, StandardCharsets.UTF_8))
                if (!line.isBlank()) addRecord(parse(line));
        } catch (IOException ex) { throw new IllegalStateException("Could not import maintenance records.", ex); }
    }

    public List<MaintenanceRecord> loadAllRecords() {
        return jdbc.query("select m.*, v.vehicle_type from maintenance_records m join vehicles v on v.vehicle_id=m.vehicle_id order by m.scheduled_date desc, m.record_id",
                (rs, n) -> new MaintenanceRecord(rs.getString("record_id"), rs.getString("vehicle_id"),
                        rs.getString("vehicle_type"), rs.getString("service_type"), rs.getDate("scheduled_date").toLocalDate().toString(),
                        rs.getDate("expected_completion_date").toLocalDate().toString(),
                        rs.getDate("completed_date") == null ? "" : rs.getDate("completed_date").toLocalDate().toString(),
                        rs.getString("provider"), rs.getDouble("estimated_cost"), rs.getDouble("actual_cost"),
                        rs.getString("status"), rs.getString("notes")));
    }

    public void saveAllRecords(List<MaintenanceRecord> records) {
        jdbc.update("delete from maintenance_records");
        records.forEach(this::addRecord);
    }

    public void addRecord(MaintenanceRecord r) {
        jdbc.update("insert into maintenance_records(record_id,vehicle_id,service_type,scheduled_date,expected_completion_date,completed_date,provider,estimated_cost,actual_cost,status,notes) values(?,?,?,?,?,?,?,?,?,?,?)",
                r.getRecordId(), r.getVehicleId(), r.getServiceType(), LocalDate.parse(r.getServiceDate()),
                LocalDate.parse(r.getExpectedCompletionDate()), dateOrNull(r.getCompletedDate()), r.getProvider(),
                r.getEstimatedCost(), r.getActualCost(), r.getStatus(), r.getNotes());
    }

    public MaintenanceRecord updateStatus(String id, String status, double actualCost, String completedDate) {
        MaintenanceRecord current = findRecordById(id);
        if (current == null) throw new IllegalArgumentException("Maintenance record was not found.");
        LocalDate completed = "Completed".equalsIgnoreCase(status)
                ? (completedDate == null || completedDate.isBlank() ? LocalDate.now() : LocalDate.parse(completedDate))
                : dateOrNull(current.getCompletedDate());
        jdbc.update("update maintenance_records set status=?, actual_cost=?, completed_date=? where record_id=?",
                status, actualCost, completed, id);
        return findRecordById(id);
    }

    public void deleteRecord(String id) { jdbc.update("delete from maintenance_records where record_id=?", id); }
    public MaintenanceRecord findRecordById(String id) {
        return loadAllRecords().stream().filter(r -> r.getRecordId().equals(id)).findFirst().orElse(null);
    }
    public boolean hasBookingConflict(String vehicleId, LocalDate start, LocalDate end) {
        return hasBookingConflict(loadAllRecords(), vehicleId, start, end);
    }
    static boolean hasBookingConflict(List<MaintenanceRecord> records, String vehicleId, LocalDate start, LocalDate end) {
        for (MaintenanceRecord r : records) {
            if (!r.getVehicleId().equalsIgnoreCase(vehicleId) || !r.isActive()) continue;
            try {
                LocalDate from = LocalDate.parse(r.getServiceDate());
                LocalDate to = LocalDate.parse(r.getExpectedCompletionDate());
                if (!from.isAfter(end) && !to.isBefore(start)) return true;
            } catch (DateTimeParseException ignored) { }
        }
        return false;
    }
    public boolean isMaintenanceInProgress(String id) {
        return loadAllRecords().stream().anyMatch(r -> r.getVehicleId().equalsIgnoreCase(id) && "In Progress".equalsIgnoreCase(r.getStatus()));
    }
    public boolean isCurrentlyUnavailableForMaintenance(String id) {
        return loadAllRecords().stream().anyMatch(r -> r.getVehicleId().equalsIgnoreCase(id)
                && ("In Progress".equalsIgnoreCase(r.getStatus()) || r.isDue()));
    }
    private LocalDate dateOrNull(String value) { return value == null || value.isBlank() ? null : LocalDate.parse(value); }
    private MaintenanceRecord parse(String line) {
        if (!line.contains("|")) {
            String[] p = line.split(",", 7);
            if (p.length != 7) throw new IllegalStateException("Invalid maintenance record: " + line);
            return new MaintenanceRecord(p[0],p[1],p[2],p[3],p[4],p[5],p[6]);
        }
        String[] p=line.split("\\|",-1);
        if(p.length!=12) throw new IllegalStateException("Invalid maintenance record: " + line);
        return new MaintenanceRecord(p[0],p[1],p[2],p[3],p[4],p[5],p[6],p[7],Double.parseDouble(p[8]),Double.parseDouble(p[9]),p[10],p[11]);
    }
}
