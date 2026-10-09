package com.example.vehiclerentalserviceplatform.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LicenceSubmissionServiceTests {
    @TempDir Path storage;

    @Test
    void submitsAndReviewsTwoPrivateImages() throws Exception {
        Fixture fixture = new Fixture(storage);
        byte[] png = image();
        fixture.service.submit("driver", file("front", png), file("back", png));
        assertEquals("PENDING", fixture.service.forCustomer("driver").status());
        assertEquals("Driving licence review requested", new NotificationEventService(fixture.jdbc).staffEvents().get(0).title());
        assertFalse(fixture.verified());
        assertTrue(fixture.service.image("driver", "front").resource().exists());
        assertTrue(fixture.service.image("driver", "back").resource().exists());

        fixture.service.review("driver", "reject", "Image unclear", "admin");
        assertEquals("Driving licence needs new images", new NotificationEventService(fixture.jdbc).customerEvents("driver").get(0).title());
        assertEquals("REJECTED", fixture.service.forCustomer("driver").status());
        assertEquals("Image unclear", fixture.service.forCustomer("driver").reason());
        assertFalse(fixture.verified());

        fixture.service.submit("driver", file("front", png), file("back", png));
        fixture.service.review("driver", "approve", null, "admin");
        assertEquals("Driving licence approved", new NotificationEventService(fixture.jdbc).customerEvents("driver").get(0).title());
        assertEquals("APPROVED", fixture.service.forCustomer("driver").status());
        assertTrue(fixture.verified());
        assertThrows(IllegalArgumentException.class,
                () -> fixture.service.submit("driver", file("front", png), file("back", png)));
    }

    @Test
    void rejectsInvalidAndOversizeUploadsBeforeWriting() throws Exception {
        Fixture fixture = new Fixture(storage);
        byte[] png = image();
        assertThrows(IllegalArgumentException.class,
                () -> fixture.service.submit("driver", file("front", "not an image".getBytes()), file("back", png)));
        assertThrows(IllegalArgumentException.class,
                () -> fixture.service.submit("driver", file("front", new byte[(int) LicenceSubmissionService.MAX_IMAGE_BYTES + 1]), file("back", png)));
        assertEquals("NOT_SUBMITTED", fixture.service.forCustomer("driver").status());
        assertThrows(IllegalArgumentException.class, () -> fixture.service.image("driver", "unknown"));
    }

    @Test
    void keepsPreviouslyVerifiedCustomersApprovedWithoutInventingImages() {
        Fixture fixture = new Fixture(storage);
        fixture.jdbc.update("UPDATE customers SET license_verified=TRUE WHERE username='driver'");
        var status = fixture.service.forCustomer("driver");
        assertEquals("APPROVED", status.status());
        assertFalse(status.hasImages());
    }

    private static MockMultipartFile file(String side, byte[] bytes) {
        return new MockMultipartFile(side + "Image", side + ".png", "image/png", bytes);
    }

    private static byte[] image() throws Exception {
        BufferedImage image = new BufferedImage(400, 240, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    private static class Fixture {
        final JdbcTemplate jdbc;
        final LicenceSubmissionService service;

        Fixture(Path storage) {
            var datasource = new DriverManagerDataSource("jdbc:h2:mem:licence_" + java.util.UUID.randomUUID()
                    + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
            jdbc = new JdbcTemplate(datasource);
            jdbc.execute("CREATE TABLE customers(username VARCHAR(80) PRIMARY KEY, active BOOLEAN, license_verified BOOLEAN)");
            jdbc.execute("CREATE TABLE licence_submissions (username VARCHAR(80) PRIMARY KEY, status VARCHAR(20) NOT NULL, front_file VARCHAR(100) NOT NULL, back_file VARCHAR(100) NOT NULL, front_mime VARCHAR(30) NOT NULL, back_mime VARCHAR(30) NOT NULL, reason VARCHAR(255), submitted_at TIMESTAMP NOT NULL, reviewed_at TIMESTAMP NULL, reviewed_by VARCHAR(80) NULL)");
            jdbc.execute("CREATE TABLE activity_log (activity_id BIGINT AUTO_INCREMENT PRIMARY KEY, module_name VARCHAR(80), actor VARCHAR(80), action_name VARCHAR(80), record_reference VARCHAR(80), details VARCHAR(500), occurred_at TIMESTAMP)");
            jdbc.execute("CREATE TABLE notification_events (event_id BIGINT AUTO_INCREMENT PRIMARY KEY, audience VARCHAR(12), recipient_username VARCHAR(80), title VARCHAR(160), message VARCHAR(500), link VARCHAR(255), severity VARCHAR(12), created_at TIMESTAMP)");
            jdbc.update("INSERT INTO customers VALUES ('driver', TRUE, FALSE)");
            service = new LicenceSubmissionService(jdbc, new ActivityLogService(jdbc), new NotificationEventService(jdbc), storage.toString());
        }

        boolean verified() { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT license_verified FROM customers WHERE username='driver'", Boolean.class)); }
    }
}
