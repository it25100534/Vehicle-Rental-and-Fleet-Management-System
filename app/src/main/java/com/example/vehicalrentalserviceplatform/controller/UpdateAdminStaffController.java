package com.example.vehicalrentalserviceplatform.controller;

import com.example.vehicalrentalserviceplatform.service.AdminStaffService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class UpdateAdminStaffController extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userId = request.getParameter("userId");
        String fullName = request.getParameter("fullName");
        String username = request.getParameter("username");
        String role = request.getParameter("role");
        String employeeType = request.getParameter("employeeType");
        String password = request.getParameter("password");

        AdminStaffService service = new AdminStaffService();
        boolean updated = service.updateByUserId(userId, fullName, username, role, employeeType, password, "system-admin");

        if (updated) {
            response.sendRedirect("/admin/admin-dashboard.html?message=Account%20updated%20successfully.");
        } else {
            response.sendRedirect("/admin/admin-dashboard.html?message=Account%20update%20failed.");
        }
    }
}
