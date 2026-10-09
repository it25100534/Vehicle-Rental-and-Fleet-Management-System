package com.example.vehiclerentalserviceplatform.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import static org.assertj.core.api.Assertions.assertThat;

class HomePreviewSeedTests {
    @Test void seedsOnceAndDoesNotRestoreDeletedBranchesOrOverwriteEdits() {
        var source=new DriverManagerDataSource("jdbc:h2:mem:home_preview_seed;MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        new ResourceDatabasePopulator(new ClassPathResource("db/schema.sql")).execute(source);
        var seed=new ResourceDatabasePopulator(new ClassPathResource("db/home-preview-seed.sql"));
        seed.execute(source);
        var jdbc=new JdbcTemplate(source);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vehicles",Long.class)).isEqualTo(45);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vehicles WHERE branch_id='BR-003'",Long.class)).isEqualTo(15);
        jdbc.update("UPDATE vehicles SET branch_id='BR-002' WHERE branch_id='BR-003'");
        jdbc.update("DELETE FROM branches WHERE branch_id='BR-003'");
        jdbc.update("UPDATE vehicles SET category_code='FAMILY' WHERE vehicle_id='CAR-0001'");
        seed.execute(source);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM branches",Long.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT category_code FROM vehicles WHERE vehicle_id='CAR-0001'",String.class)).isEqualTo("FAMILY");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM home_preview_initialized",Long.class)).isEqualTo(1);
    }
}
