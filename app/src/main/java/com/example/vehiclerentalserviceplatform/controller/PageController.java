package com.example.vehiclerentalserviceplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;
import com.example.vehiclerentalserviceplatform.security.SessionAccess;

@Controller
public class PageController {

    @GetMapping("/admin-report")
    public String adminReport(HttpSession session) {
        if (!SessionAccess.isStaff(session)) {
            return "redirect:/admin-login";
        }
        return "admin-report";
    }
}
