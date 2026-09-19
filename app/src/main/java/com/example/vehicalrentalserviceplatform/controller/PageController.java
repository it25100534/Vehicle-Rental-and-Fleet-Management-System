package com.example.vehicalrentalserviceplatform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/admin-report")
    public String adminReport() {
        return "admin-report";
    }
}