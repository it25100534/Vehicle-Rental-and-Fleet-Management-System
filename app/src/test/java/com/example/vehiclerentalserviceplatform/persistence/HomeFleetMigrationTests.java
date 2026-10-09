package com.example.vehiclerentalserviceplatform.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.assertj.core.api.Assertions.assertThat;

class HomeFleetMigrationTests {
    @Test void restoresDefaultsOnceAndPreservesBookingsAndCustomAssignments() {
        JdbcTemplate jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:home_migration;MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE branches(branch_id VARCHAR PRIMARY KEY,status VARCHAR,image_path VARCHAR)");
        jdbc.execute("INSERT INTO branches VALUES('BR-001','OPEN',NULL),('BR-002','OPEN',NULL),('BR-003','OPEN',NULL)");
        jdbc.execute("CREATE TABLE vehicle_categories(code VARCHAR PRIMARY KEY,active BOOLEAN)");
        jdbc.execute("INSERT INTO vehicle_categories VALUES('DAILY',TRUE),('BUSINESS',TRUE),('FAMILY',TRUE),('WEDDING',TRUE)");
        jdbc.execute("CREATE TABLE vehicles(vehicle_id VARCHAR PRIMARY KEY,category_code VARCHAR,branch_id VARCHAR,vehicle_type VARCHAR,status VARCHAR,retired BOOLEAN)");
        jdbc.execute("INSERT INTO vehicles VALUES('CAR-0003','DAILY','BR-001','CAR','AVAILABLE',FALSE),('CAR-0004','DAILY','BR-001','CAR','AVAILABLE',FALSE),('CAR-0006','WEDDING','BR-002','CAR','AVAILABLE',FALSE)");
        jdbc.execute("CREATE TABLE bookings(vehicle_id VARCHAR,status VARCHAR)");
        jdbc.execute("INSERT INTO bookings VALUES('CAR-0004','Approved')");
        jdbc.execute("CREATE TABLE maintenance_records(vehicle_id VARCHAR,status VARCHAR)");
        HomeFleetMigration migration=new HomeFleetMigration(jdbc);
        migration.run(null);
        assertThat(jdbc.queryForObject("SELECT category_code FROM vehicles WHERE vehicle_id='CAR-0003'",String.class)).isEqualTo("BUSINESS");
        assertThat(jdbc.queryForObject("SELECT branch_id FROM vehicles WHERE vehicle_id='CAR-0003'",String.class)).isEqualTo("BR-003");
        assertThat(jdbc.queryForObject("SELECT branch_id FROM vehicles WHERE vehicle_id='CAR-0004'",String.class)).isEqualTo("BR-001");
        assertThat(jdbc.queryForObject("SELECT category_code FROM vehicles WHERE vehicle_id='CAR-0006'",String.class)).isEqualTo("WEDDING");
        jdbc.update("UPDATE vehicles SET branch_id='BR-002',category_code='FAMILY' WHERE vehicle_id='CAR-0003'");
        migration.run(null);
        assertThat(jdbc.queryForObject("SELECT category_code FROM vehicles WHERE vehicle_id='CAR-0003'",String.class)).isEqualTo("FAMILY");
        assertThat(jdbc.queryForObject("SELECT branch_id FROM vehicles WHERE vehicle_id='CAR-0003'",String.class)).isEqualTo("BR-002");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM home_fleet_backup",Long.class)).isEqualTo(3);
    }
}
