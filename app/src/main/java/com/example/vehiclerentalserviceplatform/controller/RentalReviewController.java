package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.security.SessionAccess;
import com.example.vehiclerentalserviceplatform.service.RentalReviewService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
public class RentalReviewController {
    private final RentalReviewService reviews;

    public RentalReviewController(RentalReviewService reviews) { this.reviews = reviews; }

    @GetMapping("/api/customer/reviews")
    public List<RentalReviewService.Review> ownReviews(HttpSession session) {
        return reviews.forCustomer(customer(session));
    }

    @PostMapping("/api/customer/reviews")
    public Map<String, String> addReview(HttpSession session, @RequestParam String bookingId,
                                          @RequestParam int stars, @RequestParam(required = false) String reviewText) {
        try {
            reviews.submit(customer(session), bookingId, stars, reviewText);
            return Map.of("message", "Thank you for reviewing your rental.");
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error.getMessage());
        }
    }

    private String customer(HttpSession session) {
        String username = SessionAccess.customer(session);
        if (username == null) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return username;
    }
}
