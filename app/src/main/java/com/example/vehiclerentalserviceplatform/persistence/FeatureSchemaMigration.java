package com.example.vehiclerentalserviceplatform.persistence;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/** Keeps an already-created MySQL database compatible with the completed feature set. */
@Component("featureSchemaMigration")
public class FeatureSchemaMigration {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public FeatureSchemaMigration(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    @PostConstruct
    void migrate() throws Exception {
        add("branches", "image_path", "VARCHAR(500)");
        add("customers", "license_verified", "BOOLEAN NOT NULL DEFAULT FALSE");
        add("customers", "full_name", "VARCHAR(140)");
        add("staff_accounts", "home_branch_id", "VARCHAR(30)");
        add("vehicles", "image_hash", "VARCHAR(64)");
        add("vehicles", "retired", "BOOLEAN NOT NULL DEFAULT FALSE");
        add("bookings", "pickup_branch_id", "VARCHAR(30)");
        add("bookings", "dropoff_branch_id", "VARCHAR(30)");
        add("bookings", "extras_total", "DECIMAL(12,2) NOT NULL DEFAULT 0");
        add("bookings", "promo_code", "VARCHAR(30)");
        add("bookings", "discount_amount", "DECIMAL(12,2) NOT NULL DEFAULT 0");
        add("bookings", "deposit_amount", "DECIMAL(12,2) NOT NULL DEFAULT 0");
        add("bookings", "alternate_dropoff_charge", "DECIMAL(12,2) NOT NULL DEFAULT 0");
        add("bookings", "cancellation_fee", "DECIMAL(12,2) NOT NULL DEFAULT 0");
        add("bookings", "cancellation_reason", "VARCHAR(255)");
        add("bookings", "cancelled_at", "TIMESTAMP NULL");
        add("payments", "booking_id", "VARCHAR(50)");
        add("payments", "invoice_id", "BIGINT");
        add("payments", "idempotency_key", "VARCHAR(100)");
        add("payments", "status", "VARCHAR(20) NOT NULL DEFAULT 'PAID'");
        add("payments", "failure_reason", "VARCHAR(255)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS contact_enquiries (enquiry_id BIGINT AUTO_INCREMENT PRIMARY KEY, customer_username VARCHAR(80), name VARCHAR(120) NOT NULL, email VARCHAR(160) NOT NULL, subject VARCHAR(160) NOT NULL, message VARCHAR(1000) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'NEW', created_at TIMESTAMP NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS rental_inspection_audit (audit_id BIGINT AUTO_INCREMENT PRIMARY KEY, rental_id VARCHAR(50) NOT NULL, action_name VARCHAR(30) NOT NULL, reason VARCHAR(255) NOT NULL, actor VARCHAR(80) NOT NULL, occurred_at TIMESTAMP NOT NULL)");
        try { jdbc.execute("CREATE UNIQUE INDEX ux_payments_idempotency ON payments(idempotency_key)"); }
        catch (RuntimeException ignored) { /* index already exists */ }
        String product;
        try (Connection connection = dataSource.getConnection()) { product = connection.getMetaData().getDatabaseProductName(); }
        if (product.toLowerCase().contains("mysql")) jdbc.execute("ALTER TABLE payments MODIFY paid_at TIMESTAMP NULL");
        else { try { jdbc.execute("ALTER TABLE payments ALTER COLUMN paid_at SET NULL"); } catch (RuntimeException ignored) {} }
    }

    private void add(String table, String column, String definition) throws Exception {
        if (hasColumn(table, column)) return;
        jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }

    private boolean hasColumn(String table, String column) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            try (ResultSet rs = meta.getColumns(connection.getCatalog(), null, table, column)) {
                if (rs.next()) return true;
            }
            try (ResultSet rs = meta.getColumns(connection.getCatalog(), null, table.toUpperCase(), column.toUpperCase())) {
                return rs.next();
            }
        }
    }
}
