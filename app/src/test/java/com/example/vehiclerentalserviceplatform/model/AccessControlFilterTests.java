package com.example.vehiclerentalserviceplatform.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccessControlFilterTests {

    private final AccessControlFilter filter = new AccessControlFilter();

    @Test
    void redirectsAnonymousAdminPageRequestsToAdminLogin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/dashboard");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(302, response.getStatus());
        assertEquals("/admin-login", response.getRedirectedUrl());
    }

    @Test
    void permitsStaffToOpenOperationalAdminPages() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/dashboard");
        request.getSession().setAttribute("role", "STAFF");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    void blocksStaffFromAdminAccountManagement() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/registration");
        request.getSession().setAttribute("role", "STAFF");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
    }

    @Test
    void returnsUnauthorizedForAnonymousProtectedApiRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/bookings");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void permitsSignedInCustomerToCreateInvoice() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/invoices");
        request.getSession().setAttribute("loggedInUser", "customer-one");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(200, response.getStatus());
    }

    @Test
    void customerProfileFromStaffSessionOffersCustomerSignIn() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/profile");
        request.getSession().setAttribute("role", "ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(302, response.getStatus());
        assertEquals("/login?switchAccount=customer", response.getRedirectedUrl());
    }

    @Test
    void customerFormPostRemainsProtectedFromStaffSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/profile/licence");
        request.getSession().setAttribute("role", "ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(403, response.getStatus());
    }
}
