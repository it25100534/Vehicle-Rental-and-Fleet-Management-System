package com.example.vehiclerentalserviceplatform.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class RentalReviewService {
    public record Review(String bookingId, int stars, String reviewText) {}

    private final JdbcTemplate jdbc;

    public RentalReviewService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public List<Review> forCustomer(String username) {
        return jdbc.query("SELECT booking_id,stars,review_text FROM rental_reviews WHERE lower(customer_username)=lower(?) ORDER BY created_at DESC",
                (rs, row) -> new Review(rs.getString(1), rs.getInt(2), rs.getString(3)), username);
    }

    @Transactional
    public void submit(String username, String bookingId, int stars, String text) {
        if (username == null || username.isBlank() || bookingId == null || bookingId.isBlank())
            throw new IllegalArgumentException("Choose a completed rental to review.");
        if (stars < 1 || stars > 5) throw new IllegalArgumentException("Choose between one and five stars.");
        String review = text == null ? "" : text.strip();
        if (review.length() > 1000) throw new IllegalArgumentException("Keep your review under 1000 characters.");
        Integer eligible = jdbc.queryForObject("SELECT COUNT(*) FROM rentals WHERE booking_id=? AND lower(customer_username)=lower(?) AND status='RETURNED'",
                Integer.class, bookingId, username);
        if (eligible == null || eligible != 1) throw new IllegalArgumentException("This rental is not ready for a review.");
        Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM rental_reviews WHERE booking_id=?", Integer.class, bookingId);
        if (existing != null && existing > 0) throw new IllegalArgumentException("This rental has already been reviewed.");
        jdbc.update("INSERT INTO rental_reviews(booking_id,customer_username,stars,review_text,source,created_at) VALUES(?,?,?,?,?,?)",
                bookingId, username, stars, review, "CUSTOMER", Timestamp.from(Instant.now()));
    }
}
