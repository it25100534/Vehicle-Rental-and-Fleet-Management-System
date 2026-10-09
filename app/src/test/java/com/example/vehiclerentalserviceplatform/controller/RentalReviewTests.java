package com.example.vehiclerentalserviceplatform.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class RentalReviewTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    @Test void onlyRentalOwnerCanReviewOnceAndHomePercentageUsesStars() throws Exception {
        jdbc.update("INSERT INTO customers(username,license_id,email,phone,password_hash,active,license_verified) VALUES(?,?,?,?,?,TRUE,TRUE)",
                "review-customer", "REVIEW-LIC-1", "review@example.test", "0771234567", "hashed");
        jdbc.update("INSERT INTO bookings(transaction_id,customer_username,vehicle_id,start_date,return_date,status) VALUES(?,?,?,'2026-01-02','2026-01-03','Completed')",
                "REVIEW-BOOK-1", "review-customer", "CAR-0001");
        jdbc.update("INSERT INTO rentals(record_id,booking_id,customer_username,vehicle_id,scheduled_return_date,handed_over_at,odometer_out,fuel_level_out,condition_out,status,returned_at,odometer_in,fuel_level_in,condition_in) VALUES(?,?,?,?,'2026-01-03','2026-01-02 09:00:00',1000,'Full','Good','RETURNED','2026-01-03 18:00:00',1050,'Full','Good')",
                "REVIEW-RENT-1", "REVIEW-BOOK-1", "review-customer", "CAR-0001");

        mvc.perform(post("/api/customer/reviews").sessionAttr("loggedInUser", "other-customer")
                .param("bookingId", "REVIEW-BOOK-1").param("stars", "5"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/customer/reviews").sessionAttr("loggedInUser", "review-customer")
                .param("bookingId", "REVIEW-BOOK-1").param("stars", "4").param("reviewText", "Good trip"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/customer/reviews").sessionAttr("loggedInUser", "review-customer")
                .param("bookingId", "REVIEW-BOOK-1").param("stars", "5"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/customer/reviews").sessionAttr("loggedInUser", "review-customer"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].stars").value(4));
        mvc.perform(get("/api/home/stats"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reviewCount").value(1))
                .andExpect(jsonPath("$.satisfactionPercent").value(80));
    }
}
