package com.example.vehiclerentalserviceplatform.persistence;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Adds approved image-backed models after the legacy fleet has finished loading. */
@Component
public class NewVehicleBatchImport {
    private final JdbcTemplate jdbc;

    public NewVehicleBatchImport(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void importModels() throws IOException {
        ClassPathResource manifest = new ClassPathResource("db/new-vehicle-batch.tsv");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(manifest.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] fields = line.split("\t", -1);
                if (fields.length != 14) throw new IllegalStateException("Invalid new-vehicle row: " + line);
                String id = fields[0];
                String image = imageName(fields);
                if (!new ClassPathResource("static/images/" + image).exists()) {
                    throw new IllegalStateException("Missing image for " + id + ": " + image);
                }
                Integer present = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM vehicles WHERE vehicle_id=?", Integer.class, id);
                if (present != null && present > 0) continue;

                String branch = fields[9];
                Integer branchOpen = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM branches WHERE branch_id=? AND status='OPEN'", Integer.class, branch);
                if (branchOpen == null || branchOpen == 0) {
                    branch = jdbc.queryForObject(
                            "SELECT branch_id FROM branches WHERE status='OPEN' ORDER BY branch_id LIMIT 1",
                            String.class);
                }
                jdbc.update("INSERT INTO vehicles (vehicle_id, make, model, manufacture_year, vehicle_type, " +
                                "fuel_type, seat_count, vehicle_subtype, category_code, branch_id, rental_rate, " +
                                "mileage, status, image_path, image_hash, retired) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?,0,'AVAILABLE',?,NULL,FALSE)",
                        id, fields[3], fields[4], Integer.parseInt(fields[2]), fields[1], fields[5],
                        Integer.parseInt(fields[6]), fields[7], fields[8], branch,
                        Integer.parseInt(fields[10]), image);
            }
        }
    }

    private String imageName(String[] fields) {
        String slug = (fields[3] + "-" + fields[4]).toLowerCase(Locale.ROOT)
                .replace(":", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        return fields[0] + "_" + slug + "-" + fields[2] + ".png";
    }
}
