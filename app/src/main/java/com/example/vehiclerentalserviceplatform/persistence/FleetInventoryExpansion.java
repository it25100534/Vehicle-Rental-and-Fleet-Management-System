package com.example.vehiclerentalserviceplatform.persistence;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One-time expansion of the original 45 model records into 100 physical units. */
@Component
public class FleetInventoryExpansion implements ApplicationRunner {
    private static final int VERSION = 1;
    private static final Set<String> SINGLE_UNITS = Set.of(
            "PRE-0001", "PRE-0002", "PRE-0003", "PRE-0004", "PRE-0005",
            "PRE-0006", "PRE-0007", "PRE-0008", "PRE-0009", "VAN-0007");
    private static final Set<String> THREE_UNITS = Set.of(
            "CAR-0001", "CAR-0002", "CAR-0003", "CAR-0004", "CAR-0005",
            "CAR-0006", "CAR-0007", "CAR-0008",
            "SUV-0001", "SUV-0002", "SUV-0003", "SUV-0006", "SUV-0007", "SUV-0009",
            "VAN-0001", "VAN-0002", "VAN-0004", "VAN-0005",
            "MOT-0001", "MOT-0003");

    private final JdbcTemplate jdbc;

    public FleetInventoryExpansion(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS fleet_expansion_versions (version_number INT PRIMARY KEY)");
        if (count("SELECT COUNT(*) FROM fleet_expansion_versions WHERE version_number=?", VERSION) != 0) return;

        List<String> baseIds = jdbc.queryForList(
                "SELECT vehicle_id FROM vehicles WHERE retired=FALSE AND vehicle_id NOT LIKE '%-U2' " +
                        "AND vehicle_id NOT LIKE '%-U3' ORDER BY vehicle_id", String.class);
        if (baseIds.size() != 45 || !baseIds.containsAll(SINGLE_UNITS) || !baseIds.containsAll(THREE_UNITS)) {
            // Do not expand a custom fleet or guess which records are models.
            return;
        }

        Map<String, BigDecimal> recordedMileage = latestValidOdometers();
        for (String id : baseIds) {
            jdbc.update("UPDATE vehicles SET mileage=? WHERE vehicle_id=?",
                    recordedMileage.getOrDefault(id, BigDecimal.ZERO), id);
        }

        int doubleIndex = 0;
        for (String id : baseIds) {
            if (SINGLE_UNITS.contains(id)) continue;
            String secondBranch = branchForCopy(id, 2, doubleIndex);
            insertCopy(id, id + "-U2", secondBranch);
            if (THREE_UNITS.contains(id)) {
                insertCopy(id, id + "-U3", branchForCopy(id, 3, doubleIndex));
            } else {
                doubleIndex++;
            }
        }

        long active = count("SELECT COUNT(*) FROM vehicles WHERE retired=FALSE");
        if (active != 100) throw new IllegalStateException("Fleet expansion expected 100 active units, found " + active);
        jdbc.update("INSERT INTO fleet_expansion_versions(version_number) VALUES (?)", VERSION);
    }

    private void insertCopy(String sourceId, String copyId, String branch) {
        if (count("SELECT COUNT(*) FROM vehicles WHERE vehicle_id=?", copyId) != 0) {
            throw new IllegalStateException("Fleet copy ID already exists: " + copyId);
        }
        jdbc.update("INSERT INTO vehicles (vehicle_id, make, model, manufacture_year, vehicle_type, " +
                        "fuel_type, seat_count, vehicle_subtype, category_code, branch_id, rental_rate, mileage, " +
                        "status, image_path, image_hash, retired) " +
                        "SELECT ?, make, model, manufacture_year, vehicle_type, fuel_type, seat_count, " +
                        "vehicle_subtype, category_code, ?, rental_rate, 0, 'AVAILABLE', image_path, NULL, FALSE " +
                        "FROM vehicles WHERE vehicle_id=?",
                copyId, branch, sourceId);
    }

    private String branchForCopy(String id, int copyNumber, int doubleIndex) {
        String preferred = copyNumber == 3 || (doubleIndex % 2 == 0) ? "BR-003" : "BR-002";
        if (copyNumber == 2 && THREE_UNITS.contains(id)) preferred = "BR-002";
        return count("SELECT COUNT(*) FROM branches WHERE branch_id=? AND status='OPEN'", preferred) > 0
                ? preferred : jdbc.queryForObject("SELECT branch_id FROM vehicles WHERE vehicle_id=?", String.class, id);
    }

    private Map<String, BigDecimal> latestValidOdometers() {
        Map<String, BigDecimal> mileage = new HashMap<>();
        List<String> rejected = new ArrayList<>();
        jdbc.query("SELECT record_id, vehicle_id, odometer_out, odometer_in, handed_over_at, returned_at " +
                        "FROM rentals WHERE status='RETURNED' AND odometer_in IS NOT NULL ORDER BY returned_at", rs -> {
            BigDecimal delta = rs.getBigDecimal("odometer_in").subtract(rs.getBigDecimal("odometer_out"));
            Timestamp out = rs.getTimestamp("handed_over_at");
            Timestamp in = rs.getTimestamp("returned_at");
            long elapsedHours = in == null ? 0 : Math.max(0, Duration.between(out.toInstant(), in.toInstant()).toHours());
            long days = Math.max(1, (elapsedHours + 23) / 24);
            // Large jumps in the old test handovers are data-entry errors, not real travel.
            if (delta.signum() < 0 || delta.compareTo(BigDecimal.valueOf(1200L * days)) > 0) {
                rejected.add(rs.getString("record_id"));
            } else {
                mileage.put(rs.getString("vehicle_id"), rs.getBigDecimal("odometer_in"));
            }
        });
        if (!rejected.isEmpty()) {
            System.err.println("Fleet mileage reset excluded implausible rental odometer records: " +
                    String.join(", ", rejected));
        }
        return mileage;
    }

    private long count(String sql, Object... parameters) {
        Long value = jdbc.queryForObject(sql, Long.class, parameters);
        return value == null ? 0 : value;
    }
}
