package com.example.vehiclerentalserviceplatform.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
public class HomeDataController {
    private final JdbcTemplate jdbc;
    public HomeDataController(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @GetMapping("/api/home/stats")
    public ResponseEntity<Map<String, Long>> stats() {
        long reviews = count("SELECT COUNT(*) FROM rental_reviews");
        long stars = count("SELECT COALESCE(SUM(stars),0) FROM rental_reviews");
        Map<String, Long> totals = new LinkedHashMap<>();
        totals.put("vehicles", count("SELECT COUNT(*) FROM vehicles WHERE retired=FALSE"));
        totals.put("branches", count("SELECT COUNT(*) FROM branches WHERE status='OPEN'"));
        totals.put("rentals", count("SELECT COUNT(*) FROM rentals WHERE status='RETURNED'"));
        totals.put("reviewCount", reviews);
        totals.put("demoReviewCount", count("SELECT COUNT(*) FROM rental_reviews WHERE source='DEMO'"));
        totals.put("satisfactionPercent", reviews == 0 ? 0 : Math.round(100.0 * stars / (5.0 * reviews)));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(totals);
    }
    private long count(String sql) { Long value = jdbc.queryForObject(sql, Long.class); return value == null ? 0 : value; }
    @GetMapping("/api/home/media")
    public java.util.List<String> media() {
        return java.util.List.of("founder-arosha-kamalhewa.webp", "benefit-daily-front.webp", "benefit-daily-side.webp",
                        "benefit-family-trip.webp", "benefit-branch-route.webp").stream()
                .filter(name -> new org.springframework.core.io.ClassPathResource("static/images/home-redesign/" + name).exists())
                .toList();
    }
}
