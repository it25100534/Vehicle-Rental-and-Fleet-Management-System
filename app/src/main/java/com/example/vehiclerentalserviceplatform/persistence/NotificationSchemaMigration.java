package com.example.vehiclerentalserviceplatform.persistence;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

@Component("notificationSchemaMigration")
@DependsOn("featureSchemaMigration")
public class NotificationSchemaMigration {
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;

    public NotificationSchemaMigration(JdbcTemplate jdbc, DataSource dataSource) {
        this.jdbc = jdbc;
        this.dataSource = dataSource;
    }

    @PostConstruct
    void migrate() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS notification_events ("
                + "event_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "audience VARCHAR(12) NOT NULL, recipient_username VARCHAR(80), "
                + "title VARCHAR(160) NOT NULL, message VARCHAR(500) NOT NULL, "
                + "link VARCHAR(255) NOT NULL, severity VARCHAR(12) NOT NULL, "
                + "created_at TIMESTAMP NOT NULL, read_at TIMESTAMP NULL)");
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            boolean present;
            try (ResultSet columns = meta.getColumns(connection.getCatalog(), null, "notification_events", "read_at")) {
                present = columns.next();
            }
            if (!present) {
                try (ResultSet columns = meta.getColumns(connection.getCatalog(), null, "NOTIFICATION_EVENTS", "READ_AT")) {
                    present = columns.next();
                }
            }
            if (!present) jdbc.execute("ALTER TABLE notification_events ADD COLUMN read_at TIMESTAMP NULL");
        } catch (Exception ex) {
            throw new IllegalStateException("Could not update notification storage.", ex);
        }
    }
}
