package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.service.ActivityLogService;
import com.example.vehiclerentalserviceplatform.service.LicenceSubmissionService;
import com.example.vehiclerentalserviceplatform.service.NotificationEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LicenceSubmissionControllerTests {
    @TempDir Path storage;

    @Test
    void onlyStaffCanReadPrivateLicenceImages() throws Exception {
        var datasource = new DriverManagerDataSource("jdbc:h2:mem:licence_access_" + java.util.UUID.randomUUID()
                + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(datasource);
        jdbc.execute("CREATE TABLE customers(username VARCHAR(80) PRIMARY KEY, active BOOLEAN, license_verified BOOLEAN)");
        jdbc.execute("CREATE TABLE licence_submissions (username VARCHAR(80) PRIMARY KEY, status VARCHAR(20) NOT NULL, front_file VARCHAR(100) NOT NULL, back_file VARCHAR(100) NOT NULL, front_mime VARCHAR(30) NOT NULL, back_mime VARCHAR(30) NOT NULL, reason VARCHAR(255), submitted_at TIMESTAMP NOT NULL, reviewed_at TIMESTAMP NULL, reviewed_by VARCHAR(80) NULL)");
        jdbc.execute("CREATE TABLE activity_log (activity_id BIGINT AUTO_INCREMENT PRIMARY KEY, module_name VARCHAR(80), actor VARCHAR(80), action_name VARCHAR(80), record_reference VARCHAR(80), details VARCHAR(500), occurred_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE notification_events (event_id BIGINT AUTO_INCREMENT PRIMARY KEY, audience VARCHAR(12), recipient_username VARCHAR(80), title VARCHAR(160), message VARCHAR(500), link VARCHAR(255), severity VARCHAR(12), created_at TIMESTAMP)");
        jdbc.update("INSERT INTO customers VALUES ('driver', TRUE, FALSE)");
        LicenceSubmissionService licences = new LicenceSubmissionService(jdbc, new ActivityLogService(jdbc), new NotificationEventService(jdbc), storage.toString());
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(400, 240, BufferedImage.TYPE_INT_RGB), "png", bytes);
        var image = new MockMultipartFile("frontImage", "licence.png", "image/png", bytes.toByteArray());
        licences.submit("driver", image, image);
        var mvc = MockMvcBuilders.standaloneSetup(new LicenceSubmissionController(licences)).build();

        MockHttpSession customer = new MockHttpSession();
        customer.setAttribute("loggedInUser", "driver");
        mvc.perform(get("/admin/customers/driver/licence/front/image").session(customer))
                .andExpect(status().isForbidden());

        MockHttpSession staff = new MockHttpSession();
        staff.setAttribute("role", "STAFF");
        mvc.perform(get("/admin/customers/driver/licence/front/image").session(staff))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().contentType("image/png"));
    }
}
