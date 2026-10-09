package com.example.vehiclerentalserviceplatform.persistence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Explicit local demonstration fixture. Never enabled by the default application profile. */
@Component
@ConditionalOnProperty(name = "driveease.demo.seed", havingValue = "true")
public class DemoScenarioSeeder {
    private final JdbcTemplate jdbc;
    private final NewVehicleBatchImport newModels;

    public DemoScenarioSeeder(JdbcTemplate jdbc, NewVehicleBatchImport newModels) {
        this.jdbc = jdbc;
        this.newModels = newModels;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() throws IOException {
        jdbc.execute("CREATE TABLE IF NOT EXISTS demo_seed_versions (version_number INT PRIMARY KEY)");
        if (count("SELECT COUNT(*) FROM demo_seed_versions WHERE version_number=1") == 0) {
            insertBaseline();
            newModels.importModels();
            expandNewModels();
            insertCustomers();
            insertRentalHistory();
            recalculateMileage();

            long active = count("SELECT COUNT(*) FROM vehicles WHERE retired=FALSE");
            long customers = count("SELECT COUNT(*) FROM customers WHERE username LIKE 'demo%'");
            long rentals = count("SELECT COUNT(*) FROM rentals WHERE record_id LIKE 'DEMO-RENT-2026-%'");
            if (active != 200 || customers != 500 || rentals != 1000) {
                throw new IllegalStateException("Demo data totals must be 200 vehicles, 500 users and 1000 rentals; got "
                        + active + ", " + customers + ", " + rentals);
            }
            jdbc.update("INSERT INTO demo_seed_versions(version_number) VALUES (1)");
        }
        if (count("SELECT COUNT(*) FROM demo_seed_versions WHERE version_number=2") == 0) {
            seedDemoReviews();
            jdbc.update("INSERT INTO demo_seed_versions(version_number) VALUES (2)");
        }
    }

    private void seedDemoReviews() {
        String[] fiveStarNotes = {
                "Pickup was straightforward and the vehicle was ready on time.",
                "Clean vehicle and a smooth return process.",
                "The rental suited our family trip perfectly.",
                "Easy booking and helpful branch staff.",
                "Reliable vehicle for the full journey.",
                "A comfortable trip from pickup to return.",
                "The vehicle matched the description and drove well.",
                "Quick handover and clear rental details.",
                "A convenient option for our weekend plans."
        };
        List<Map<String, Object>> candidates = jdbc.queryForList("SELECT r.booking_id,r.customer_username " +
                "FROM rentals r LEFT JOIN rental_reviews review ON review.booking_id=r.booking_id " +
                "WHERE r.status='RETURNED' AND r.record_id LIKE 'DEMO-RENT-2026-%' " +
                "AND review.booking_id IS NULL ORDER BY r.record_id LIMIT 100");
        if (candidates.size() != 100) throw new IllegalStateException("Expected 100 unreviewed demo rentals.");
        for (int index = 0; index < candidates.size(); index++) {
            Map<String, Object> rental = candidates.get(index);
            int stars = index % 10 == 9 ? 4 : 5;
            jdbc.update("INSERT INTO rental_reviews(booking_id,customer_username,stars,review_text,source,created_at) VALUES(?,?,?,?,?,?)",
                    rental.get("booking_id"), rental.get("customer_username"), stars,
                    stars == 5 ? fiveStarNotes[index % fiveStarNotes.length]
                            : "Good experience overall, with a little room to improve the pickup time.",
                    "DEMO", Timestamp.valueOf(LocalDateTime.of(2026, 9, 30, 12, 0).plusMinutes(index)));
        }
    }

    private void insertBaseline() throws IOException {
        for (String[] row : rows("db/demo-baseline-fleet.tsv", 12)) {
            if (count("SELECT COUNT(*) FROM vehicles WHERE vehicle_id=?", row[0]) != 0) continue;
            jdbc.update("INSERT INTO vehicles(vehicle_id,vehicle_type,manufacture_year,make,model,fuel_type," +
                            "seat_count,vehicle_subtype,category_code,branch_id,rental_rate,image_path,mileage,status,retired) " +
                            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,0,'AVAILABLE',FALSE)",
                    row[0], row[1], Integer.parseInt(row[2]), row[3], row[4], row[5],
                    Integer.parseInt(row[6]), row[7], row[8], openBranch(row[9]),
                    new BigDecimal(row[10]), row[11]);
        }
    }

    private void expandNewModels() throws IOException {
        for (String[] row : rows("db/new-vehicle-units.tsv", 2)) {
            String source = row[0];
            int units = Integer.parseInt(row[1]);
            if (count("SELECT COUNT(*) FROM vehicles WHERE vehicle_id=?", source) != 1)
                throw new IllegalStateException("Missing new model: " + source);
            for (int n = 2; n <= units; n++) {
                String unitId = source + "-U" + n;
                if (count("SELECT COUNT(*) FROM vehicles WHERE vehicle_id=?", unitId) != 0) continue;
                String branch = openBranch(n == 2 ? "BR-002" : "BR-003");
                jdbc.update("INSERT INTO vehicles(vehicle_id,make,model,manufacture_year,vehicle_type,fuel_type," +
                                "seat_count,vehicle_subtype,category_code,branch_id,rental_rate,mileage,status," +
                                "image_path,image_hash,retired) " +
                                "SELECT ?,make,model,manufacture_year,vehicle_type,fuel_type,seat_count," +
                                "vehicle_subtype,category_code,?,rental_rate,0,'AVAILABLE',image_path,NULL,FALSE " +
                                "FROM vehicles WHERE vehicle_id=?",
                        unitId, branch, source);
            }
        }
    }

    private void insertCustomers() throws IOException {
        for (String[] row : rows("db/demo-customers.tsv", 6)) {
            if (count("SELECT COUNT(*) FROM customers WHERE username=?", row[0]) != 0) continue;
            jdbc.update("INSERT INTO customers(username,full_name,email,phone,license_id,password_hash," +
                            "active,license_verified) VALUES(?,?,?,?,?,?,TRUE,TRUE)",
                    row[0], row[1], row[2], row[3], row[4], row[5]);
        }
    }

    private void insertRentalHistory() throws IOException {
        Map<String, String> branches = new HashMap<>();
        jdbc.queryForList("SELECT vehicle_id,branch_id FROM vehicles").forEach(row ->
                branches.put((String) row.get("vehicle_id"), (String) row.get("branch_id")));
        for (String[] row : rows("db/demo-rentals.tsv", 8)) {
            String bookingId = row[0];
            String rentalId = row[1];
            String branch = branches.get(row[3]);
            if (branch == null) throw new IllegalStateException("Missing rental vehicle: " + row[3]);
            LocalDate start = LocalDate.parse(row[4]);
            LocalDate end = LocalDate.parse(row[5]);
            if (count("SELECT COUNT(*) FROM bookings WHERE transaction_id=?", bookingId) == 0) {
                jdbc.update("INSERT INTO bookings(transaction_id,customer_username,vehicle_id,start_date," +
                                "return_date,status,pickup_branch_id,dropoff_branch_id) " +
                                "VALUES(?,?,?,?,?,'Completed',?,?)",
                        bookingId, row[2], row[3], Date.valueOf(start), Date.valueOf(end), branch, branch);
            }
            if (count("SELECT COUNT(*) FROM rentals WHERE record_id=?", rentalId) == 0) {
                jdbc.update("INSERT INTO rentals(record_id,booking_id,customer_username,vehicle_id," +
                                "scheduled_return_date,handed_over_at,odometer_out,fuel_level_out,condition_out," +
                                "status,returned_at,odometer_in,fuel_level_in,condition_in) " +
                                "VALUES(?,?,?,?,?,?,?,?,?,'RETURNED',?,?,?,?)",
                        rentalId, bookingId, row[2], row[3], Date.valueOf(end),
                        Timestamp.valueOf(start.atTime(9, 0)), Integer.parseInt(row[6]), "Full",
                        "Good condition", Timestamp.valueOf(end.atTime(18, 0)),
                        Integer.parseInt(row[7]), "Full", "Returned in good condition");
            }
        }
    }

    private void recalculateMileage() {
        List<String> ids = jdbc.queryForList("SELECT vehicle_id FROM vehicles WHERE retired=FALSE", String.class);
        for (String id : ids) {
            Double distance = jdbc.queryForObject("SELECT COALESCE(SUM(CASE WHEN status='RETURNED' " +
                    "AND odometer_in IS NOT NULL AND odometer_in>=odometer_out " +
                    "THEN odometer_in-odometer_out ELSE 0 END),0) FROM rentals WHERE vehicle_id=?",
                    Double.class, id);
            jdbc.update("UPDATE vehicles SET mileage=? WHERE vehicle_id=?", distance == null ? 0 : distance, id);
        }
    }

    private String openBranch(String preferred) {
        if (count("SELECT COUNT(*) FROM branches WHERE branch_id=? AND status='OPEN'", preferred) != 0)
            return preferred;
        return jdbc.queryForObject("SELECT branch_id FROM branches WHERE status='OPEN' ORDER BY branch_id LIMIT 1",
                String.class);
    }

    private long count(String sql, Object... args) {
        Long result = jdbc.queryForObject(sql, Long.class, args);
        return result == null ? 0 : result;
    }

    private List<String[]> rows(String path, int size) throws IOException {
        List<String[]> result = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource(path);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] columns = line.split("\t", -1);
                if (columns.length != size) throw new IllegalStateException("Invalid demo row in " + path);
                result.add(columns);
            }
        }
        return result;
    }
}
