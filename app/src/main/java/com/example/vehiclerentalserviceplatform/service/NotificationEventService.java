package com.example.vehiclerentalserviceplatform.service;

import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@DependsOn("notificationSchemaMigration")
public class NotificationEventService {
    public record Event(String title, String message, String link, String severity, Instant createdAt) {}

    private final JdbcTemplate jdbc;

    public NotificationEventService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void staff(String title, String message, String link) {
        save("STAFF", null, title, message, link, "warning");
    }

    public void customer(String username, String title, String message, String link, String severity) {
        if (username != null && !username.isBlank()) save("CUSTOMER", username, title, message, link, severity);
    }

    private void save(String audience, String username, String title, String message, String link, String severity) {
        jdbc.update("INSERT INTO notification_events(audience,recipient_username,title,message,link,severity,created_at) VALUES(?,?,?,?,?,?,?)",
                audience, username, title, message, link, severity, Timestamp.from(Instant.now()));
    }

    public List<Event> staffEvents() {
        return jdbc.query("SELECT title,message,link,severity,created_at FROM notification_events WHERE audience='STAFF' ORDER BY event_id DESC LIMIT 100",
                (rs, row) -> new Event(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getTimestamp(5).toInstant()));
    }

    public List<Event> customerEvents(String username) {
        if (username == null) return List.of();
        return jdbc.query("SELECT title,message,link,severity,created_at FROM notification_events WHERE audience='CUSTOMER' AND lower(recipient_username)=lower(?) ORDER BY event_id DESC LIMIT 100",
                (rs, row) -> new Event(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getTimestamp(5).toInstant()), username);
    }

    public int unreadCustomerCount(String username) {
        if (username == null) return 0;
        Integer count = jdbc.queryForObject("SELECT count(*) FROM notification_events WHERE audience='CUSTOMER' AND lower(recipient_username)=lower(?) AND read_at IS NULL",
                Integer.class, username);
        return count == null ? 0 : count;
    }

    public void markCustomerRead(String username) {
        if (username != null) jdbc.update("UPDATE notification_events SET read_at=? WHERE audience='CUSTOMER' AND lower(recipient_username)=lower(?) AND read_at IS NULL",
                Timestamp.from(Instant.now()), username);
    }

    public int pendingStaffCount() {
        return pendingByType().values().stream().mapToInt(Integer::intValue).sum();
    }

    public Map<String, Integer> pendingByType() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("licence", jdbc.queryForObject("SELECT count(*) FROM licence_submissions WHERE status='PENDING'", Integer.class));
        counts.put("booking", jdbc.queryForObject("SELECT count(*) FROM bookings WHERE status='Pending'", Integer.class));
        counts.put("enquiry", jdbc.queryForObject("SELECT count(*) FROM contact_enquiries WHERE status='NEW'", Integer.class));
        return counts;
    }
}
