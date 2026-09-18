package com.example.vehicalrentalserviceplatform.controller; // Update to your package

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.servlet.http.HttpSession;

@Controller
public class ViewController {

    private boolean isNotAuthorized(HttpSession session) {
        String role = (String) session.getAttribute("role");
        return role == null || (!role.equals("ADMIN") && !role.equals("STAFF"));
    }

    private boolean isNotLoggedIn(HttpSession session) {
        return session.getAttribute("loggedInUser") == null;
    }

    @GetMapping("/inventory")
    public String showInventory(HttpSession session) {
        if (isNotAuthorized(session))
            return "redirect:/admin-login";
        return "inventory";
    }

    @GetMapping("/catalog")
    public String showCatalog() {
        return "catalog";
    }

    @GetMapping("/vehicleForm")
    public String showVehicleForm(HttpSession session) {
        if(isNotAuthorized(session))
            return "redirect:/admin-login";
        return "vehicleForm";
    }

}