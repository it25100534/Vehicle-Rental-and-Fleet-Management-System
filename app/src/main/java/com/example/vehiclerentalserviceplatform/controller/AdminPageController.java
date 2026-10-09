package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.service.AdminStaffService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.vehiclerentalserviceplatform.service.BookingService;
import jakarta.servlet.http.HttpSession;
import com.example.vehiclerentalserviceplatform.maintenance.MaintenanceFileHandler;
import com.example.vehiclerentalserviceplatform.maintenance.MaintenanceRecord;
import java.util.List;
import com.example.vehiclerentalserviceplatform.fleet.BranchService;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    private final AdminStaffService adminStaffService;
    private final BookingService bookingService;
    private final MaintenanceFileHandler maintenance;
    private final BranchService branches;

    public AdminPageController(AdminStaffService adminStaffService, BookingService bookingService,
                               MaintenanceFileHandler maintenance, BranchService branches) {
        this.adminStaffService = adminStaffService;
        this.bookingService = bookingService;
        this.maintenance = maintenance;
        this.branches = branches;
    }

    private boolean isNotAuthorized(HttpSession session) {
        String role = (String) session.getAttribute("role");
        return role == null || (!role.equals("ADMIN") && !role.equals("STAFF"));
    }

    @GetMapping
    public String adminHome() {
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, @RequestParam(required = false) String message, Model model) {
        if(isNotAuthorized(session)) return "redirect:/admin-login";

        List<MaintenanceRecord> allMaintenance = maintenance.loadAllRecords();
        long pendingCount = allMaintenance.stream()
                .filter(record -> "Pending".equalsIgnoreCase(record.getStatus()))
                .count();

        model.addAttribute("message", message);
        model.addAttribute("admins", adminStaffService.getAllAdmins());
        model.addAttribute("logs", adminStaffService.getAllActivityLogs());
        var allBookings = new java.util.ArrayList<>(bookingService.getAllBookings());
        allBookings.sort((left, right) -> right.getStartDate().compareTo(left.getStartDate()));
        model.addAttribute("bookingCount", allBookings.size());
        model.addAttribute("allBookings", allBookings.subList(0, Math.min(10, allBookings.size())));
        model.addAttribute("pendingMaintenanceCount", pendingCount);
        model.addAttribute("branches", branches.getAll());
        return "admin-dashboard";
    }

    @GetMapping("/registration")
    public String registration(HttpSession session, @RequestParam(required = false) String message, Model model) {

        if (isNotAuthorized(session)) return "redirect:/admin-login";
        model.addAttribute("message", message);
        model.addAttribute("branches", branches.getOpenBranches());
        return "admin-registration";
    }

    @GetMapping("/activity-log")
    public String activityLog(HttpSession session, @RequestParam(defaultValue = "1") int page, Model model) {
        if (isNotAuthorized(session)) return "redirect:/admin-login";

        var allLogs = adminStaffService.getAllActivityLogs();
        int pageSize = 25;
        int pages = Math.max(1, (allLogs.size() + pageSize - 1) / pageSize);
        int currentPage = Math.max(1, Math.min(page, pages));
        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allLogs.size());
        model.addAttribute("logs", allLogs.subList(fromIndex, toIndex));
        model.addAttribute("total", allLogs.size());
        model.addAttribute("page", currentPage);
        model.addAttribute("pages", pages);
        model.addAttribute("from", allLogs.isEmpty() ? 0 : fromIndex + 1);
        model.addAttribute("to", toIndex);
        return "admin-activity-log";
    }

    @GetMapping("/edit")
    public String editAdmin(
            HttpSession session,
            @RequestParam String userId,
            @RequestParam String fullName,
            @RequestParam String username,
            @RequestParam String role,
            @RequestParam String employeeType,
            Model model
    ) {
        if (isNotAuthorized(session)) return "redirect:/admin-login";

        model.addAttribute("userId", userId);
        model.addAttribute("fullName", fullName);
        model.addAttribute("username", username);
        model.addAttribute("role", role);
        model.addAttribute("employeeType", employeeType);
        return "edit-admin-staff";
    }

    @GetMapping("/admin-dashboard.html")
    public String dashboardHtmlFromAdminPrefix(@RequestParam(required = false) String message) {
        if (message == null || message.isBlank()) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/admin/dashboard?message=" + message;
    }

    @GetMapping("/admin-registration.html")
    public String registrationHtmlFromAdminPrefix(@RequestParam(required = false) String message) {
        if (message == null || message.isBlank()) {
            return "redirect:/admin/registration";
        }
        return "redirect:/admin/registration?message=" + message;
    }

    @GetMapping("/admin-activity-log.html")
    public String activityLogHtmlFromAdminPrefix() {
        return "redirect:/admin/activity-log";
    }

    @GetMapping("/report")
    public String adminReport(HttpSession session) {
    if (isNotAuthorized(session)) return "redirect:/admin-login";

        return "admin-report";
    }

    @GetMapping("/edit-admin-staff.html")
    public String editHtmlFromAdminPrefix(
            @RequestParam String userId,
            @RequestParam String fullName,
            @RequestParam String username,
            @RequestParam String role,
            @RequestParam String employeeType
    ) {
        return "redirect:/admin/edit?userId=" + userId
                + "&fullName=" + fullName
                + "&username=" + username
                + "&role=" + role
                + "&employeeType=" + employeeType;
    }
}
