package com.example.vehiclerentalserviceplatform.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Component;

import java.sql.DatabaseMetaData;
import java.util.List;

@Component("bookingSchemaMigration")
public class BookingSchemaMigration {
    private static final String CURRENT_CONSTRAINT = "chk_bookings_status";
    private final JdbcTemplate jdbcTemplate;

    public BookingSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        removeLegacyH2StatusConstraint();
    }

    private void removeLegacyH2StatusConstraint() {
        String product = jdbcTemplate.execute((ConnectionCallback<String>) connection -> {
            DatabaseMetaData metadata = connection.getMetaData();
            return metadata.getDatabaseProductName();
        });
        if (product == null || !product.toLowerCase().contains("h2")) return;

        List<String> constraints = jdbcTemplate.queryForList("""
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_name = 'bookings' AND constraint_type = 'CHECK'
                """, String.class);
        constraints.stream()
                .filter(name -> !CURRENT_CONSTRAINT.equalsIgnoreCase(name))
                .filter(name -> name.matches("[A-Za-z0-9_]+"))
                .forEach(name -> jdbcTemplate.execute(
                        "ALTER TABLE bookings DROP CONSTRAINT \"" + name + "\""));
    }
}
