package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.model.Admin;
import com.example.vehiclerentalserviceplatform.service.AdminStaffService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminLoginController {

    private final AdminStaffService adminStaffService;

    public AdminLoginController(AdminStaffService adminStaffService) {
        this.adminStaffService = adminStaffService;
    }

    @GetMapping("/admin-login")
    public String showAdminLoginPage(Model model, @RequestParam(required = false) String error) {
        if (error != null) {
            model.addAttribute("error", "Invalid username or password.");
        }
        return "admin-login";
    }

    @PostMapping("/admin-login")
    public String handleAdminLogin(@RequestParam String username,
                                   @RequestParam String password,
                                   HttpServletRequest request) {

        Admin admin = adminStaffService.authenticate(username, password);

        if (admin != null) {
            HttpSession existing = request.getSession(false);
            if (existing != null) existing.invalidate();
            HttpSession session = request.getSession(true);
            session.setAttribute("loggedInAdmin", admin.getUsername());
            session.setAttribute("role", admin.getRole());

            return "redirect:/admin/dashboard"; // Success! Go to dashboard
        } else {
            return "redirect:/admin-login?error=true"; // Failed! Reload with error
        }
    }

    @PostMapping("/admin-logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin-login";
    }
}
