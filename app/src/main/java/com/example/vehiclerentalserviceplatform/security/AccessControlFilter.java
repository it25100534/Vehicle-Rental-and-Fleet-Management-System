package com.example.vehiclerentalserviceplatform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AccessControlFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String method = request.getMethod();
        HttpSession session = request.getSession(false);

        if (path.equals("/notifications") && !hasCustomer(session) && !hasStaff(session)) {
            reject(request, response, session, false);
            return;
        }

        if (requiresAdmin(path) && !hasAdmin(session)) {
            reject(request, response, session, true);
            return;
        }

        if (requiresStaff(path, method) && !hasStaff(session)) {
            reject(request, response, session, true);
            return;
        }

        if (requiresCustomer(path, method) && !hasCustomer(session)) {
            if ("GET".equals(method) && !path.startsWith("/api/")) {
                response.sendRedirect(request.getContextPath() + "/login?switchAccount=customer");
                return;
            }
            reject(request, response, session, false);
            return;
        }

        if (path.equals("/api/bookings") && !hasCustomer(session) && !hasStaff(session)) {
            reject(request, response, session, false);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresAdmin(String path) {
        return path.startsWith("/admin/branches")
                || path.startsWith("/admin/vehicle-categories")
                || path.equals("/registerAdminStaff")
                || path.equals("/updateAdminStaff")
                || path.equals("/deleteAdminStaff")
                || path.equals("/admin/registration")
                || path.equals("/admin/edit")
                || path.equals("/admin-registration.html")
                || path.equals("/edit-admin-staff.html")
                || path.equals("/admin/admin-registration.html")
                || path.equals("/admin/edit-admin-staff.html");
    }

    private boolean requiresStaff(String path, String method) {
        if (path.startsWith("/api/admin/")) return true;
        if (path.equals("/api/branches/all")) return true;
        if (path.startsWith("/admin/") || path.equals("/admin")
                || path.equals("/admin-report")
                || path.equals("/admin-dashboard.html")
                || path.equals("/admin-activity-log.html")
                || path.equals("/inventory") || path.equals("/vehicleForm")
                || path.startsWith("/maintenance") || path.startsWith("/handover")) {
            return true;
        }

        if (path.startsWith("/api/vehicles")) {
            return !"GET".equals(method);
        }

        if (path.startsWith("/api/invoices")) {
            return !"POST".equals(method);
        }

        return path.equals("/admin/approveBooking") || path.equals("/admin/rejectBooking");
    }

    private boolean requiresCustomer(String path, String method) {
        if (path.startsWith("/api/customer/notifications/")) return true;
        if (path.equals("/api/customer/reviews")) return true;
        if (path.equals("/profile") || path.startsWith("/profile/")
                || path.equals("/reservationHistory") || path.equals("/bookVehicle")
                || path.equals("/editBooking") || path.equals("/checkout")
                || path.equals("/createBooking") || path.equals("/updateBooking")
                || path.equals("/deleteBooking")
                || path.equals("/payment") || path.equals("/payments/history")
                || path.startsWith("/invoices/")) {
            return true;
        }

        return path.equals("/api/invoices") && "POST".equals(method);
    }

    private boolean hasCustomer(HttpSession session) {
        return session != null && SessionAccess.isCustomer(session);
    }

    private boolean hasStaff(HttpSession session) {
        return session != null && SessionAccess.isStaff(session);
    }

    private boolean hasAdmin(HttpSession session) {
        return session != null && SessionAccess.isAdmin(session);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response,
                        HttpSession session, boolean staffArea) throws IOException {
        boolean apiRequest = request.getRequestURI().contains("/api/")
                || "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));

        if (apiRequest) {
            response.sendError(session == null ? HttpServletResponse.SC_UNAUTHORIZED
                    : HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (session == null) {
            response.sendRedirect(request.getContextPath() + (staffArea ? "/admin-login" : "/login"));
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }
}
