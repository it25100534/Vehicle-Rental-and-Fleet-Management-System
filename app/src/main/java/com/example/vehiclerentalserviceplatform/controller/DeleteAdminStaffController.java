package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.service.AdminStaffService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class DeleteAdminStaffController extends HttpServlet {
    private final AdminStaffService service;

    public DeleteAdminStaffController(AdminStaffService service) {
        this.service = service;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userId = request.getParameter("userId");

        Object actor = request.getSession().getAttribute("loggedInAdmin");
        boolean deleted = service.deleteByUserId(userId,
                actor == null ? "system-admin" : actor.toString());

        if (deleted) {
            response.sendRedirect("/admin/admin-dashboard.html?message=Account%20deleted%20successfully.");
        } else {
            response.sendRedirect("/admin/admin-dashboard.html?message=Account%20not%20found.");
        }
    }
}
