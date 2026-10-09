package com.example.vehiclerentalserviceplatform.persistence;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Adds the private image review workflow without changing existing verified accounts. */
@Component("licenceSubmissionSchemaMigration")
@DependsOn("featureSchemaMigration")
public class LicenceSubmissionSchemaMigration {
    private final JdbcTemplate jdbc;

    public LicenceSubmissionSchemaMigration(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @PostConstruct
    void migrate() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS licence_submissions ("
                + "username VARCHAR(80) PRIMARY KEY, "
                + "status VARCHAR(20) NOT NULL, "
                + "front_file VARCHAR(100) NOT NULL, "
                + "back_file VARCHAR(100) NOT NULL, "
                + "front_mime VARCHAR(30) NOT NULL, "
                + "back_mime VARCHAR(30) NOT NULL, "
                + "reason VARCHAR(255), "
                + "submitted_at TIMESTAMP NOT NULL, "
                + "reviewed_at TIMESTAMP NULL, "
                + "reviewed_by VARCHAR(80) NULL)");
    }
}
