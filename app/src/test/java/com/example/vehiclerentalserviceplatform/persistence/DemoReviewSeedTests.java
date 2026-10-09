package com.example.vehiclerentalserviceplatform.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "driveease.demo.seed=true",
        "spring.datasource.url=jdbc:h2:mem:driveease_review_seed;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
class DemoReviewSeedTests {
    @Autowired JdbcTemplate jdbc;

    @Test void createsOneHundredDemoReviewsAtNinetyEightPercent() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rental_reviews WHERE source='DEMO'", Integer.class)).isEqualTo(100);
        assertThat(jdbc.queryForObject("SELECT SUM(stars) FROM rental_reviews WHERE source='DEMO'", Integer.class)).isEqualTo(490);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM rental_reviews review JOIN rentals rental ON rental.booking_id=review.booking_id WHERE review.source='DEMO' AND rental.status='RETURNED'", Integer.class)).isEqualTo(100);
    }
}
