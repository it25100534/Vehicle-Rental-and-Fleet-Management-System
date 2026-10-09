package com.example.vehiclerentalserviceplatform.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.example.vehiclerentalserviceplatform.service.NotificationEventService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@SpringBootTest
@AutoConfigureMockMvc
class BillingPageRenderTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired private NotificationEventService notificationEvents;

    @Test @Transactional
    void customerBellCountClearsAfterOpeningNotifications() throws Exception {
        notificationEvents.customer("test11", "Licence approved", "Approved", "/profile", "success");
        mockMvc.perform(get("/api/customer/notifications/unread-count").sessionAttr("loggedInUser", "test11"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        mockMvc.perform(get("/notifications").sessionAttr("loggedInUser", "test11")).andExpect(status().isOk());
        mockMvc.perform(get("/api/customer/notifications/unread-count").sessionAttr("loggedInUser", "test11"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void customerCanOpenOwnPaymentHistoryPage() throws Exception {
        mockMvc.perform(get("/payments/history").sessionAttr("loggedInUser", "test11"))
                .andExpect(status().isOk())
                .andExpect(view().name("payment-history"))
                .andExpect(content().string(containsString("class=\"catalog-body\"")));
    }

    @Test
    void staffCanOpenRevenueReport() throws Exception {
        mockMvc.perform(get("/admin/reports").sessionAttr("role", "STAFF"))
                .andExpect(status().isOk())
                .andExpect(view().name("revenue-report"));
    }

    @Test
    void anonymousCustomerHistoryRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/payments/history"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void customerCanOpenRoleAwareNotifications() throws Exception {
        mockMvc.perform(get("/notifications").sessionAttr("loggedInUser", "test11"))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications"))
                .andExpect(content().string(containsString("customer-notification-shell")))
                .andExpect(content().string(containsString("driveease-header")))
                .andExpect(content().string(not(containsString("sidebar-brand"))));
    }

    @Test
    void staffCanOpenOperationalNotifications() throws Exception {
        mockMvc.perform(get("/notifications").sessionAttr("role", "STAFF"))
                .andExpect(status().isOk())
                .andExpect(view().name("notifications"))
                .andExpect(content().string(containsString("staff-notification-shell")))
                .andExpect(content().string(containsString("sidebar-brand")))
                .andExpect(content().string(not(containsString("driveease-header"))));
    }
}
