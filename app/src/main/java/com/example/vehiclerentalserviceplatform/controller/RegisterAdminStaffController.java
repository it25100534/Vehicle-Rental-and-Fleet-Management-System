package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.service.AdminStaffService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class RegisterAdminStaffController extends HttpServlet {
    private final AdminStaffService service;

    public RegisterAdminStaffController(AdminStaffService service) {
        this.service = service;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fullName = request.getParameter("fullName");
        String username = request.getParameter("username");
        String role = request.getParameter("role");
        String employeeType = request.getParameter("employeeType");
        String password = request.getParameter("password");
        String homeBranch = request.getParameter("homeBranch");

        Object actor = request.getSession().getAttribute("loggedInAdmin");
        String result = service.register(fullName, username, role, employeeType, password, homeBranch,
                actor == null ? "system-admin" : actor.toString());

        response.sendRedirect("/admin/admin-registration.html?message=" + result.replace(" ", "%20"));
    }
}
