package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.service.VehicleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class FleetPageRenderTests {
    @Autowired MockMvc mockMvc;
    @Autowired VehicleService vehicleService;

    @Test
    void offersCurrentVehicleMileageForSelectedHandoverBooking() throws Exception {
        MockHttpSession staff = new MockHttpSession();
        staff.setAttribute("loggedInAdmin", "test-staff");
        staff.setAttribute("role", "STAFF");

        mockMvc.perform(get("/handover").session(staff))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    Map<?, ?> odometers = (Map<?, ?>) result.getModelAndView()
                            .getModel().get("bookingOdometers");
                    assertThat(odometers.get("9ce308ca-c071-446a-a5c0-e7e169c35cc3"))
                            .isEqualTo(vehicleService.getVehicleById("CAR-0001").getMileage());
                });
    }

    @Test
    void rendersBranchAndCategoryAdministrationPages() throws Exception {
        MockHttpSession admin = new MockHttpSession();
        admin.setAttribute("loggedInAdmin", "test-admin");
        admin.setAttribute("role", "ADMIN");

        mockMvc.perform(get("/admin/branches").session(admin))
                .andExpect(status().isOk()).andExpect(view().name("branches"));
        mockMvc.perform(get("/admin/branches/new").session(admin))
                .andExpect(status().isOk()).andExpect(view().name("branch-form"));
        mockMvc.perform(get("/admin/vehicle-categories").session(admin))
                .andExpect(status().isOk()).andExpect(view().name("vehicle-categories"));
        mockMvc.perform(get("/inventory").session(admin))
                .andExpect(status().isOk()).andExpect(view().name("inventory"));
        mockMvc.perform(get("/admin/registration").session(admin))
                .andExpect(status().isOk()).andExpect(view().name("admin-registration"));
        mockMvc.perform(get("/admin-report").session(admin))
                .andExpect(status().isOk()).andExpect(view().name("admin-report"));
        mockMvc.perform(get("/admin/activity-log").param("page", "2").session(admin))
                .andExpect(status().isOk()).andExpect(view().name("admin-activity-log"));
    }

    @Test
    void exposesOpenBranchesAndCategoriesToBookingPages() throws Exception {
        mockMvc.perform(get("/api/branches")).andExpect(status().isOk());
        mockMvc.perform(get("/api/vehicle-categories")).andExpect(status().isOk());
    }

    @Test
    void rendersTheOverdueRentalBoardForStaff() throws Exception {
        MockHttpSession staff = new MockHttpSession();
        staff.setAttribute("loggedInAdmin", "test-staff");
        staff.setAttribute("role", "STAFF");

        mockMvc.perform(get("/handover/overdue").session(staff))
                .andExpect(status().isOk())
                .andExpect(view().name("overdue-rentals"));
        mockMvc.perform(get("/handover/active").session(staff))
                .andExpect(status().isOk())
                .andExpect(view().name("active-rentals"));
    }

    @Test
    void rendersMaintenanceHistoryAndCompliancePagesForStaff() throws Exception {
        MockHttpSession staff = new MockHttpSession();
        staff.setAttribute("loggedInAdmin", "test-staff");
        staff.setAttribute("role", "STAFF");

        mockMvc.perform(get("/maintenance").session(staff))
                .andExpect(status().isOk()).andExpect(view().name("maintenance"));
        mockMvc.perform(get("/maintenance/schedule").session(staff))
                .andExpect(status().isOk()).andExpect(view().name("maintenance-history"));
        mockMvc.perform(get("/maintenance/compliance").session(staff))
                .andExpect(status().isOk()).andExpect(view().name("fleet-compliance"));
    }

    @Test
    void rendersCompletedCustomerBookingAndVehicleHistoryPages() throws Exception {
        MockHttpSession staff = new MockHttpSession();
        staff.setAttribute("loggedInAdmin", "test-staff");
        staff.setAttribute("role", "STAFF");
        mockMvc.perform(get("/admin/dashboard").session(staff))
                .andExpect(status().isOk()).andExpect(view().name("admin-dashboard"));
        mockMvc.perform(get("/admin/customers").session(staff))
                .andExpect(status().isOk()).andExpect(view().name("admin-customers"));
        mockMvc.perform(get("/admin/bookings").session(staff))
                .andExpect(status().isOk()).andExpect(view().name("admin-bookings"));
        mockMvc.perform(get("/admin/bookings").param("page", "2").session(staff))
                .andExpect(status().isOk()).andExpect(view().name("admin-bookings"));
        mockMvc.perform(get("/admin/customers").param("q", "nobody").session(staff))
                .andExpect(status().isOk()).andExpect(view().name("admin-customers"));
        mockMvc.perform(get("/handover").param("page", "2").session(staff))
                .andExpect(status().isOk()).andExpect(view().name("handover"));
        mockMvc.perform(get("/notifications").session(staff))
                .andExpect(status().isOk()).andExpect(view().name("notifications"));
        mockMvc.perform(get("/admin/contact-enquiries").session(staff))
                .andExpect(status().isOk()).andExpect(view().name("contact-enquiries"));
        mockMvc.perform(get("/admin/vehicles/CAR-0001/history").session(staff))
                .andExpect(status().isOk()).andExpect(view().name("vehicle-history"));
    }
}
