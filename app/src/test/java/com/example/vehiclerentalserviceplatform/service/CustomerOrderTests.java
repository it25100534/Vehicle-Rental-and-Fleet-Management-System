package com.example.vehiclerentalserviceplatform.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomerOrderTests {
    @Test
    void pendingLicenceRequestsLeadWithNewestSubmission() {
        var datasource = new DriverManagerDataSource("jdbc:h2:mem:customer_order_" + java.util.UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(datasource);
        jdbc.execute("CREATE TABLE customers(username VARCHAR(80) PRIMARY KEY,full_name VARCHAR(140),license_id VARCHAR(80),email VARCHAR(160),phone VARCHAR(30),active BOOLEAN,license_verified BOOLEAN)");
        jdbc.execute("CREATE TABLE licence_submissions(username VARCHAR(80) PRIMARY KEY,status VARCHAR(20),submitted_at TIMESTAMP,reviewed_at TIMESTAMP)");
        jdbc.update("INSERT INTO customers VALUES ('alpha','Alpha','A','a@test.com','1',TRUE,TRUE)");
        jdbc.update("INSERT INTO customers VALUES ('zulu','Zulu','Z','z@test.com','2',TRUE,FALSE)");
        jdbc.update("INSERT INTO customers VALUES ('middle','Middle','M','m@test.com','3',TRUE,FALSE)");
        jdbc.update("INSERT INTO licence_submissions VALUES ('zulu','PENDING',TIMESTAMP '2026-10-08 11:00:00',NULL)");
        jdbc.update("INSERT INTO licence_submissions VALUES ('middle','PENDING',TIMESTAMP '2026-10-08 12:00:00',NULL)");
        var operations = new OperationsService(jdbc, null, null, null, null);
        var rows = operations.customers();
        assertEquals("middle", rows.get(0).get("username"));
        assertEquals("zulu", rows.get(1).get("username"));
        assertEquals("alpha", rows.get(2).get("username"));
    }
}
