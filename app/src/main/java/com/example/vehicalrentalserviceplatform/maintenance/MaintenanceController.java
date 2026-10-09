package com.example.vehicalrentalserviceplatform.maintenance;

import com.example.vehicalrentalserviceplatform.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.UUID;

// CONTROLLER LAYER: Handles maintenance page requests and connects the UI with the maintenance backend logic.
@Controller

// READ: Loads the maintenance page, checks admin/staff access, and sends records + vehicles to the Thymeleaf view.
@RequestMapping("/maintenance")
public class MaintenanceController {

    // REL: Dependency with VehicleService - used to load existing vehicles into the maintenance form.
    @Autowired
    private VehicleService vehicleService;

    @GetMapping
    public String maintenancePage(HttpSession session, Model model) {

        String role = (String) session.getAttribute("role");
        if (role == null || (!role.equals("ADMIN") && !role.equals("STAFF"))) {
            return "redirect:/admin-login"; // Kick them out if not authorized
        }

        List<MaintenanceRecord> records = MaintenanceFileHandler.loadAllRecords();
        model.addAttribute("records", records);

        model.addAttribute("vehicles",vehicleService.getAllVehicles());

        return "maintenance";

    }

    // CREATE: Receives form data, creates a MaintenanceRecord object, and saves it through the file handler.
    @PostMapping("/add")
    public String addRecord(@RequestParam String vehicleId,
                            @RequestParam String vehicleType,
                            @RequestParam String serviceType,
                            @RequestParam String serviceDate,
                            @RequestParam String notes) {

        String recordId = "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        MaintenanceRecord record = new MaintenanceRecord(
                recordId,
                vehicleId,
                vehicleType,
                serviceType,
                serviceDate,
                "Pending",
                notes
        );

        MaintenanceFileHandler.addRecord(record);

        return "redirect:/maintenance";
    }

    // UPDATE: Receives record ID + new status, then updates the matching maintenance record.
    @PostMapping("/update")
    public String updateStatus(@RequestParam String recordId,
                               @RequestParam String status) {

        MaintenanceFileHandler.updateStatus(recordId, status);

        return "redirect:/maintenance";
    }

    // DELETE - Remove a maintenance record
    @GetMapping("/delete/{recordId}")
    public String deleteRecord(@PathVariable String recordId) {

        MaintenanceFileHandler.deleteRecord(recordId);

        return "redirect:/maintenance";
    }

    // ALERT: Polymorphism demonstration - Show vehicle type based maintenance alert
    @GetMapping("/alert/{recordId}")
    @ResponseBody
    public String getAlert(@PathVariable String recordId) {

        MaintenanceRecord record = MaintenanceFileHandler.findRecordById(recordId);

        if (record != null) {
            return record.getMaintenanceAlert();
        }

        return "Record not found.";
    }
}