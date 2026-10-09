package com.example.vehiclerentalserviceplatform.maintenance;

import com.example.vehiclerentalserviceplatform.model.Vehicle;
import com.example.vehiclerentalserviceplatform.security.InputValidation;
import com.example.vehiclerentalserviceplatform.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/maintenance")
public class MaintenanceController {
    private final VehicleService vehicleService;
    private final MaintenanceFileHandler maintenance;
    private final FleetComplianceFileHandler complianceStore;

    public MaintenanceController(VehicleService vehicleService, MaintenanceFileHandler maintenance,
                                 FleetComplianceFileHandler complianceStore) {
        this.vehicleService = vehicleService;
        this.maintenance = maintenance;
        this.complianceStore = complianceStore;
    }

    @GetMapping
    public String maintenancePage(HttpSession session, Model model) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        List<MaintenanceRecord> records = maintenance.loadAllRecords();
        syncVehicleAvailability(records);
        model.addAttribute("records", records);
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        model.addAttribute("activeCount", records.stream().filter(MaintenanceRecord::isActive).count());
        model.addAttribute("dueCount", records.stream().filter(MaintenanceRecord::isDue).count());
        model.addAttribute("totalCost", records.stream().mapToDouble(MaintenanceRecord::getActualCost).sum());
        return "maintenance";
    }

    @GetMapping("/schedule")
    public String schedule(HttpSession session, Model model) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        List<MaintenanceRecord> records = maintenance.loadAllRecords();
        model.addAttribute("records", records);
        model.addAttribute("upcoming", records.stream().filter(MaintenanceRecord::isActive).toList());
        model.addAttribute("completed", records.stream()
                .filter(record -> "Completed".equalsIgnoreCase(record.getStatus())).toList());
        model.addAttribute("totalCost", records.stream().mapToDouble(MaintenanceRecord::getActualCost).sum());
        model.addAttribute("downtime", records.stream().mapToLong(MaintenanceRecord::getDowntimeDays).sum());
        return "maintenance-history";
    }

    @GetMapping("/compliance")
    public String compliance(HttpSession session, Model model) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        List<FleetComplianceRecord> records = complianceStore.loadAll();
        model.addAttribute("records", records);
        model.addAttribute("vehicles", vehicleService.getAllVehicles());
        model.addAttribute("alertCount", records.stream().filter(record -> !"VALID".equals(record.getState())).count());
        return "fleet-compliance";
    }

    @PostMapping("/compliance/save")
    public String saveCompliance(@RequestParam String vehicleId,
                                 @RequestParam String insuranceExpiry,
                                 @RequestParam String licenceExpiry,
                                 @RequestParam String emissionExpiry,
                                 HttpSession session, RedirectAttributes redirect) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        if (vehicleService.getVehicleById(vehicleId) == null) {
            redirect.addFlashAttribute("error", "Select a valid vehicle.");
            return "redirect:/maintenance/compliance";
        }
        try {
            LocalDate.parse(insuranceExpiry);
            LocalDate.parse(licenceExpiry);
            LocalDate.parse(emissionExpiry);
            complianceStore.save(new FleetComplianceRecord(
                    vehicleId, insuranceExpiry, licenceExpiry, emissionExpiry));
            redirect.addFlashAttribute("message", "Compliance dates saved.");
        } catch (DateTimeParseException ex) {
            redirect.addFlashAttribute("error", "All compliance dates must be valid.");
        }
        return "redirect:/maintenance/compliance";
    }

    @PostMapping("/add")
    public String addRecord(@RequestParam String vehicleId,
                            @RequestParam String serviceType,
                            @RequestParam String serviceDate,
                            @RequestParam String expectedCompletionDate,
                            @RequestParam String provider,
                            @RequestParam(defaultValue = "0") double estimatedCost,
                            @RequestParam(defaultValue = "") String notes,
                            HttpSession session, RedirectAttributes redirect) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        Vehicle vehicle = vehicleService.getVehicleById(vehicleId);
        if (vehicle == null) {
            redirect.addFlashAttribute("error", "Select a valid vehicle.");
            return "redirect:/maintenance";
        }
        try {
            InputValidation.requireMaintenanceDetails(serviceType, serviceDate, expectedCompletionDate,
                    provider, estimatedCost, notes);
            boolean duplicate = maintenance.loadAllRecords().stream().anyMatch(record ->
                    record.isActive() && record.getVehicleId().equalsIgnoreCase(vehicleId)
                            && record.getServiceDate().equals(serviceDate)
                            && record.getServiceType().equalsIgnoreCase(serviceType));
            if (duplicate) throw new IllegalArgumentException("This vehicle already has the same active service scheduled on that date.");

            MaintenanceRecord record = new MaintenanceRecord(newId(), vehicleId, vehicle.getType(), serviceType,
                    serviceDate, expectedCompletionDate, "", provider.trim(), estimatedCost, 0,
                    "Pending", notes == null ? "" : notes.trim());
            maintenance.addRecord(record);
            if (record.isDue()) updateVehicleAvailability(vehicleId, false);
            redirect.addFlashAttribute("message", "Maintenance job scheduled.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/maintenance";
    }

    @PostMapping("/update")
    public String updateStatus(@RequestParam String recordId,
                               @RequestParam String status,
                               @RequestParam(defaultValue = "0") double actualCost,
                               @RequestParam(defaultValue = "") String completedDate,
                               HttpSession session, RedirectAttributes redirect) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        MaintenanceRecord record = maintenance.findRecordById(recordId);
        if (record == null) {
            redirect.addFlashAttribute("error", "Maintenance record was not found.");
            return "redirect:/maintenance";
        }
        if (!InputValidation.isMaintenanceStatus(status)
                || !InputValidation.isForwardMaintenanceTransition(record.getStatus(), status)) {
            redirect.addFlashAttribute("error", "Maintenance status cannot move backwards or use an unknown value.");
            return "redirect:/maintenance";
        }
        if (!Double.isFinite(actualCost) || actualCost < 0 || actualCost > 10_000_000) {
            redirect.addFlashAttribute("error", "Actual cost must be between 0 and 10,000,000.");
            return "redirect:/maintenance";
        }
        try {
            if (!completedDate.isBlank()) LocalDate.parse(completedDate);
            MaintenanceRecord updated = maintenance.updateStatus(recordId, status, actualCost, completedDate);
            updateVehicleAvailability(updated.getVehicleId(),
                    "Completed".equals(status) || "Cancelled".equals(status));
            redirect.addFlashAttribute("message", "Maintenance job updated.");
        } catch (DateTimeParseException ex) {
            redirect.addFlashAttribute("error", "Completion date must be valid.");
        }
        return "redirect:/maintenance";
    }

    @PostMapping("/duplicate/{recordId}")
    public String duplicate(@PathVariable String recordId, HttpSession session, RedirectAttributes redirect) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        MaintenanceRecord source = maintenance.findRecordById(recordId);
        if (source == null) {
            redirect.addFlashAttribute("error", "Maintenance record was not found.");
        } else {
            LocalDate nextDate = LocalDate.parse(source.getServiceDate()).plusMonths(6);
            long duration = Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(
                    LocalDate.parse(source.getServiceDate()), LocalDate.parse(source.getExpectedCompletionDate())));
            maintenance.addRecord(new MaintenanceRecord(newId(), source.getVehicleId(),
                    source.getVehicleType(), source.getServiceType(), nextDate.toString(),
                    nextDate.plusDays(duration).toString(), "", source.getProvider(),
                    source.getEstimatedCost(), 0, "Pending", source.getNotes()));
            redirect.addFlashAttribute("message", "A follow-up maintenance job was scheduled six months later.");
        }
        return "redirect:/maintenance/schedule";
    }

    @PostMapping("/delete/{recordId}")
    public String deleteRecord(@PathVariable String recordId, HttpSession session, RedirectAttributes redirect) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        MaintenanceRecord record = maintenance.findRecordById(recordId);
        if (record == null) redirect.addFlashAttribute("error", "Maintenance record was not found.");
        else if (!"Pending".equals(record.getStatus())) redirect.addFlashAttribute("error", "Only pending jobs can be deleted. Cancel jobs that have started so history is retained.");
        else {
            maintenance.deleteRecord(recordId);
            redirect.addFlashAttribute("message", "Pending maintenance job deleted.");
        }
        return "redirect:/maintenance";
    }

    @GetMapping("/alert/{recordId}")
    @ResponseBody
    public String getAlert(@PathVariable String recordId) {
        MaintenanceRecord record = maintenance.findRecordById(recordId);
        return record == null ? "Record not found." : record.getMaintenanceAlert();
    }

    private void syncVehicleAvailability(List<MaintenanceRecord> records) {
        for (Vehicle vehicle : vehicleService.getAllVehicles()) {
            boolean blocked = records.stream().anyMatch(record ->
                    record.getVehicleId().equalsIgnoreCase(vehicle.getVehicleId())
                            && ("In Progress".equals(record.getStatus()) || record.isDue()));
            if (blocked && vehicle.isAvailable()) updateVehicleAvailability(vehicle.getVehicleId(), false);
        }
    }

    private void updateVehicleAvailability(String vehicleId, boolean available) {
        Vehicle vehicle = vehicleService.getVehicleById(vehicleId);
        if (vehicle != null && vehicle.isAvailable() != available) {
            vehicle.setAvailable(available);
            vehicleService.updateVehicle(vehicle);
        }
    }

    private boolean hasStaffAccess(HttpSession session) {
        String role = (String) session.getAttribute("role");
        return "ADMIN".equals(role) || "STAFF".equals(role);
    }

    private String newId() {
        return "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
