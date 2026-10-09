package com.example.vehicalrentalserviceplatform.controller;

import com.example.vehicalrentalserviceplatform.service.AdminStaffService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class RegisterAdminStaffController extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fullName = request.getParameter("fullName");
        String username = request.getParameter("username");
        String role = request.getParameter("role");
        String employeeType = request.getParameter("employeeType");
        String password = request.getParameter("password");

        AdminStaffService service = new AdminStaffService();
        String result = service.register(fullName, username, role, employeeType, password, "system-admin");

        response.sendRedirect("/admin/admin-registration.html?message=" + result.replace(" ", "%20"));
    }
}
