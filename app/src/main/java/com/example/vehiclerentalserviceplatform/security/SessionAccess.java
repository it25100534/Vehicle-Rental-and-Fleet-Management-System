package com.example.vehiclerentalserviceplatform.security;

import jakarta.servlet.http.HttpSession;

public final class SessionAccess {

    private SessionAccess() {
    }

    public static String customer(HttpSession session) {
        if (session == null) return null;
        Object username = session.getAttribute("loggedInUser");
        return username instanceof String ? (String) username : null;
    }

    public static boolean isCustomer(HttpSession session) {
        return customer(session) != null;
    }

    public static boolean isStaff(HttpSession session) {
        if (session == null) return false;
        Object role = session.getAttribute("role");
        return "ADMIN".equals(role) || "STAFF".equals(role);
    }

    public static boolean isAdmin(HttpSession session) {
        return session != null && "ADMIN".equals(session.getAttribute("role"));
    }
}
