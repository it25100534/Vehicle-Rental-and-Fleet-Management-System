package com.example.vehicalrentalserviceplatform.controller;

import com.example.vehicalrentalserviceplatform.service.AdminStaffService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class DeleteAdminStaffController extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userId = request.getParameter("userId");

        AdminStaffService service = new AdminStaffService();
        boolean deleted = service.deleteByUserId(userId, "system-admin");

        if (deleted) {
            response.sendRedirect("/admin/admin-dashboard.html?message=Account%20deleted%20successfully.");
        } else {
            response.sendRedirect("/admin/admin-dashboard.html?message=Account%20not%20found.");
        }
    }
}
