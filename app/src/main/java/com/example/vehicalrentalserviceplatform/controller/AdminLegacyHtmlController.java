package com.example.vehicalrentalserviceplatform.controller;

import com.example.vehicalrentalserviceplatform.service.AdminStaffService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminLegacyHtmlController {

    private final AdminStaffService adminStaffService = new AdminStaffService();

    @GetMapping("/admin-dashboard.html")
    public String dashboard(@RequestParam(required = false) String message, Model model) {
        model.addAttribute("message", message);
        model.addAttribute("admins", adminStaffService.getAllAdmins());
        model.addAttribute("logs", adminStaffService.getAllActivityLogs());
        return "admin-dashboard";
    }

    @GetMapping("/admin-registration.html")
    public String registration(@RequestParam(required = false) String message, Model model) {
        model.addAttribute("message", message);
        return "admin-registration";
    }

    @GetMapping("/admin-activity-log.html")
    public String activityLog(Model model) {
        model.addAttribute("logs", adminStaffService.getAllActivityLogs());
        return "admin-activity-log";
    }

    @GetMapping("/edit-admin-staff.html")
    public String editAdmin(
            @RequestParam String userId,
            @RequestParam String fullName,
            @RequestParam String username,
            @RequestParam String role,
            @RequestParam String employeeType,
            Model model
    ) {
        model.addAttribute("userId", userId);
        model.addAttribute("fullName", fullName);
        model.addAttribute("username", username);
        model.addAttribute("role", role);
        model.addAttribute("employeeType", employeeType);
        return "edit-admin-staff";
    }
}
