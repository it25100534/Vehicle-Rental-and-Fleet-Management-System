package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.model.Vehicle;
import com.example.vehiclerentalserviceplatform.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BugRegressionTests {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired VehicleService vehicles;

    @Test
    void bookingPageShowsLicenceVerificationFailure() throws Exception {
        mockMvc.perform(get("/bookVehicle")
                        .param("id", "CAR-0001")
                        .param("error", "licence")
                        .sessionAttr("loggedInUser", "test11"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("driving licence must be verified")))
                .andExpect(content().string(containsString("/profile#licence-verification")));
    }

    @Test
    void customerCannotMarkBookingPaidWithoutCheckout() throws Exception {
        mockMvc.perform(post("/markAsPaid")
                        .param("transactionId", "SUV-0001")
                        .sessionAttr("loggedInUser", "test11"))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerCannotReadAnotherCustomersQuote() throws Exception {
        jdbc.update("insert into bookings(transaction_id,customer_username,vehicle_id,start_date,return_date,status) values(?,?,?,?,?,'Approved')",
                "BUG-QUOTE-001", "test11", "CAR-0001", Date.valueOf(LocalDate.now().plusDays(10)),
                Date.valueOf(LocalDate.now().plusDays(12)));

        mockMvc.perform(get("/api/bookings/BUG-QUOTE-001/quote")
                        .sessionAttr("loggedInUser", "another-customer"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidContactEnquiryReturnsToFormWithMessage() throws Exception {
        mockMvc.perform(post("/contact")
                        .param("name", "A")
                        .param("email", "not-an-email")
                        .param("subject", "x")
                        .param("message", "short"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/contact?error=*") );
    }

    @Test
    void retiredVehicleCannotBeReactivatedByEditing() {
        assertThat(vehicles.deleteVehicle("CAR-0001")).isTrue();
        Vehicle retired = vehicles.getVehicleById("CAR-0001");
        assertThat(vehicles.updateVehicle(retired)).isTrue();
        assertThat(vehicles.getVehicleById("CAR-0001").getOperationalStatus()).isEqualTo("RETIRED");
    }

    @Test
    void adminDashboardNoLongerLabelsEmployeeTypeAsPassword() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .sessionAttr("loggedInAdmin", "test-admin")
                        .sessionAttr("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Employee Type")))
                .andExpect(content().string(not(containsString("<th>Password</th>"))));
    }

    @Test
    void adminCustomersPageRendersWithLicenceStatus() throws Exception {
        mockMvc.perform(get("/admin/customers")
                        .sessionAttr("loggedInAdmin", "test-admin")
                        .sessionAttr("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Licence status")));
    }

    @Test
    void maintenanceDeletionIsNotAvailableThroughGet() throws Exception {
        mockMvc.perform(get("/maintenance/delete/REC-NOT-USED")
                        .sessionAttr("loggedInAdmin", "test-staff")
                        .sessionAttr("role", "STAFF"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void bookedVehicleCannotBeRetired() throws Exception {
        jdbc.update("insert into bookings(transaction_id,customer_username,vehicle_id,start_date,return_date,status) values(?,?,?,?,?,'Approved')",
                "BUG-RETIRE-001", "test11", "CAR-0001", Date.valueOf(LocalDate.now()),
                Date.valueOf(LocalDate.now().plusDays(2)));

        mockMvc.perform(delete("/api/vehicles/delete/CAR-0001")
                        .sessionAttr("loggedInAdmin", "test-admin")
                        .sessionAttr("role", "ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("cannot be retired")));
        assertThat(vehicles.getVehicleById("CAR-0001").getOperationalStatus()).isNotEqualTo("RETIRED");
    }

    @Test
    void deletingUnknownVehicleReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/vehicles/delete/DOES-NOT-EXIST")
                        .sessionAttr("loggedInAdmin", "test-admin")
                        .sessionAttr("role", "ADMIN"))
                .andExpect(status().isNotFound());
    }
}
