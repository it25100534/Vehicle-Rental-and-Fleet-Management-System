package com.example.vehiclerentalserviceplatform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@DependsOn("licenceSubmissionSchemaMigration")
public class LicenceSubmissionService {
    public static final long MAX_IMAGE_BYTES = 3L * 1024 * 1024;
    public record Submission(String status, String reason, boolean hasImages) {}
    public record Image(Resource resource, String mime) {}

    private final JdbcTemplate jdbc;
    private final ActivityLogService activity;
    private final NotificationEventService notifications;
    private final Path storage;

    public LicenceSubmissionService(JdbcTemplate jdbc, ActivityLogService activity, NotificationEventService notifications,
                                    @Value("${driveease.licence.upload-dir:./data/licence-uploads}") String storagePath) {
        this.jdbc = jdbc;
        this.activity = activity;
        this.notifications = notifications;
        this.storage = Path.of(storagePath).toAbsolutePath().normalize();
    }

    public Submission forCustomer(String username) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT status, reason FROM licence_submissions WHERE username=?", username);
        if (!rows.isEmpty()) {
            Map<String, Object> row = rows.get(0);
            return new Submission(String.valueOf(row.get("status")), (String) row.get("reason"), true);
        }
        List<Boolean> verifiedRows = jdbc.query("SELECT license_verified FROM customers WHERE username=?",
                (rs, rowNum) -> rs.getBoolean(1), username);
        return new Submission(!verifiedRows.isEmpty() && Boolean.TRUE.equals(verifiedRows.get(0))
                ? "APPROVED" : "NOT_SUBMITTED", null, false);
    }

    @Transactional
    public void submit(String username, MultipartFile front, MultipartFile back) {
        Integer customerCount = jdbc.queryForObject("SELECT count(*) FROM customers WHERE username=? AND active=TRUE", Integer.class, username);
        if (customerCount == null || customerCount != 1) throw new IllegalArgumentException("Your customer account is unavailable.");
        Submission current = forCustomer(username);
        if ("PENDING".equals(current.status())) throw new IllegalArgumentException("Your licence is already awaiting review.");
        if ("APPROVED".equals(current.status())) throw new IllegalArgumentException("Your licence is already verified.");
        ValidImage frontImage = validate(front, "front");
        ValidImage backImage = validate(back, "back");
        try { Files.createDirectories(storage); }
        catch (IOException ex) { throw new IllegalStateException("Unable to store licence images.", ex); }
        String frontName = UUID.randomUUID() + frontImage.extension();
        String backName = UUID.randomUUID() + backImage.extension();
        Path frontPath = storage.resolve(frontName);
        Path backPath = storage.resolve(backName);
        try {
            Files.write(frontPath, frontImage.bytes(), StandardOpenOption.CREATE_NEW);
            Files.write(backPath, backImage.bytes(), StandardOpenOption.CREATE_NEW);
            List<Map<String, Object>> previous = jdbc.queryForList("SELECT front_file, back_file FROM licence_submissions WHERE username=?", username);
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCommit() {
                        if (!previous.isEmpty()) {
                            remove((String) previous.get(0).get("front_file"));
                            remove((String) previous.get(0).get("back_file"));
                        }
                    }
                    @Override public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) { remove(frontName); remove(backName); }
                    }
                });
            }
            Timestamp now = Timestamp.from(Instant.now());
            if (previous.isEmpty()) {
                jdbc.update("INSERT INTO licence_submissions (username,status,front_file,back_file,front_mime,back_mime,reason,submitted_at,reviewed_at,reviewed_by) VALUES (?,?,?,?,?,?,NULL,?,NULL,NULL)",
                        username, "PENDING", frontName, backName, frontImage.mime(), backImage.mime(), now);
            } else {
                jdbc.update("UPDATE licence_submissions SET status='PENDING',front_file=?,back_file=?,front_mime=?,back_mime=?,reason=NULL,submitted_at=?,reviewed_at=NULL,reviewed_by=NULL WHERE username=?",
                        frontName, backName, frontImage.mime(), backImage.mime(), now, username);
            }
            jdbc.update("UPDATE customers SET license_verified=FALSE WHERE username=?", username);
            activity.log("CUSTOMERS", username, "SUBMIT_LICENSE", username, "Licence images submitted for review");
            notifications.staff("Driving licence review requested", username + " submitted front and back licence images.",
                    "/admin/customers?q=" + java.net.URLEncoder.encode(username, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ex) {
            remove(frontName); remove(backName);
            if (ex instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Unable to store licence images.", ex);
        }
    }

    @Transactional
    public void review(String username, String decision, String reason, String actor) {
        boolean approve = "approve".equalsIgnoreCase(decision);
        if (!approve && !"reject".equalsIgnoreCase(decision)) throw new IllegalArgumentException("Choose approve or reject.");
        String checkedReason = approve ? null : validateReason(reason);
        int count = jdbc.update("UPDATE licence_submissions SET status=?,reason=?,reviewed_at=?,reviewed_by=? WHERE username=? AND status='PENDING'",
                approve ? "APPROVED" : "REJECTED", checkedReason, Timestamp.from(Instant.now()), actor, username);
        if (count != 1) throw new IllegalArgumentException("This licence is no longer awaiting review.");
        jdbc.update("UPDATE customers SET license_verified=? WHERE username=?", approve, username);
        activity.log("CUSTOMERS", actor, approve ? "VERIFY_LICENSE" : "REJECT_LICENSE", username,
                approve ? "Licence images approved" : "Licence images rejected: " + checkedReason);
        notifications.customer(username, approve ? "Driving licence approved" : "Driving licence needs new images",
                approve ? "Your licence is verified. You can now book a vehicle." : "Reason: " + checkedReason + ". Upload new images to try again.",
                "/profile#licence-verification", approve ? "success" : "warning");
    }

    public Image image(String username, String side) {
        if (!"front".equals(side) && !"back".equals(side)) throw new IllegalArgumentException("Invalid licence image side.");
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT " + side + "_file AS file_name," + side + "_mime AS mime FROM licence_submissions WHERE username=?", username);
        if (rows.isEmpty()) throw new IllegalArgumentException("No licence images found.");
        String filename = (String) rows.get(0).get("file_name");
        Path path = storage.resolve(filename).normalize();
        if (!path.getParent().equals(storage) || !Files.isRegularFile(path)) throw new IllegalArgumentException("Licence image unavailable.");
        return new Image(new FileSystemResource(path), (String) rows.get(0).get("mime"));
    }

    private ValidImage validate(MultipartFile file, String side) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Upload the " + side + " image of your driving licence.");
        if (file.getSize() > MAX_IMAGE_BYTES) throw new IllegalArgumentException("Each image must be 3 MB or less.");
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > MAX_IMAGE_BYTES) throw new IllegalArgumentException("Each image must be 3 MB or less.");
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                if (input == null) throw new IllegalArgumentException("Upload a valid JPEG or PNG image.");
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IllegalArgumentException("Upload a valid JPEG or PNG image.");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input);
                    String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 300 || height < 200 || (long) width * height > 30_000_000L)
                        throw new IllegalArgumentException("Use a clear, full-size image under 30 megapixels.");
                    return switch (format) {
                        case "jpeg", "jpg" -> new ValidImage(bytes, ".jpg", "image/jpeg");
                        case "png" -> new ValidImage(bytes, ".png", "image/png");
                        default -> throw new IllegalArgumentException("Upload a valid JPEG or PNG image.");
                    };
                } finally { reader.dispose(); }
            }
        } catch (IOException ex) { throw new IllegalArgumentException("The " + side + " image could not be read."); }
    }

    private String validateReason(String reason) {
        if (reason == null) throw new IllegalArgumentException("Choose a rejection reason.");
        String value = reason.trim();
        if (!List.of("Image unclear", "Document incomplete", "Details do not match profile", "Licence expired", "Invalid document").contains(value))
            throw new IllegalArgumentException("Choose a valid rejection reason.");
        return value;
    }

    private void remove(String filename) {
        if (filename == null) return;
        Path path = storage.resolve(filename).normalize();
        if (!path.getParent().equals(storage)) return;
        try { Files.deleteIfExists(path); } catch (IOException ignored) { }
    }

    private record ValidImage(byte[] bytes, String extension, String mime) {}
}
