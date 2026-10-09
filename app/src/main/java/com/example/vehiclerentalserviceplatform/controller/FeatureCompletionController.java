package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.fleet.BranchService;
import com.example.vehiclerentalserviceplatform.security.SessionAccess;
import com.example.vehiclerentalserviceplatform.service.OperationsService;
import com.example.vehiclerentalserviceplatform.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class FeatureCompletionController {
    private final OperationsService operations;
    private final BranchService branches;
    private final VehicleService vehicles;

    public FeatureCompletionController(OperationsService operations, BranchService branches, VehicleService vehicles) {
        this.operations = operations; this.branches = branches; this.vehicles = vehicles;
    }

    @GetMapping("/admin/customers")
    String customers(@RequestParam(defaultValue = "1") int page,
                     @RequestParam(required = false) String q, HttpSession session, Model model) {
        requireStaff(session);
        String search = q == null ? "" : q.trim();
        var matches = operations.customers().stream().filter(c -> search.isBlank() ||
                (String.valueOf(c.get("full_name")) + " " + c.get("username") + " " +
                        c.get("email") + " " + c.get("license_id"))
                        .toLowerCase(java.util.Locale.ROOT).contains(search.toLowerCase(java.util.Locale.ROOT))).toList();
        addPage(model, "customers", matches, page, 20);
        model.addAttribute("search", search);
        return "admin-customers";
    }
    @PostMapping("/admin/customers/{username}/licence")
    String licence(@PathVariable String username, @RequestParam boolean verified,
                   @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "") String q,
                   HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        requireStaff(session);
        operations.setLicenceVerified(username, verified, staff(session));
        redirect.addAttribute("page", page).addAttribute("q", q);
        return "redirect:/admin/customers";
    }
    @PostMapping("/admin/customers/{username}/status")
    String customerStatus(@PathVariable String username, @RequestParam boolean active,
                          @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "") String q,
                          HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        requireStaff(session);
        operations.setCustomerActive(username, active, staff(session));
        redirect.addAttribute("page", page).addAttribute("q", q);
        return "redirect:/admin/customers";
    }

    @GetMapping("/admin/bookings")
    String bookingQueue(@RequestParam(required=false) String status, @RequestParam(required=false) String branch,
                        @RequestParam(defaultValue = "1") int page,
                        HttpSession session, Model model) {
        requireStaff(session);
        addPage(model, "bookings", operations.bookingQueue(status, branch), page, 20);
        model.addAttribute("vehicles", vehicles.getAllVehicles()); model.addAttribute("branches", branches.getAll());
        model.addAttribute("statusFilter", status); model.addAttribute("branchFilter", branch); return "admin-bookings";
    }
    @PostMapping("/admin/bookings/{id}/reassign")
    String reassign(@PathVariable String id, @RequestParam String vehicleId,
                    @RequestParam(defaultValue = "1") int page,
                    @RequestParam(defaultValue = "") String status,
                    @RequestParam(defaultValue = "") String branch,
                    HttpSession session, org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
        requireStaff(session); operations.reassignVehicle(id, vehicleId, staff(session));
        redirect.addAttribute("page", page).addAttribute("status", status).addAttribute("branch", branch);
        return "redirect:/admin/bookings";
    }
    @PostMapping("/bookings/{id}/cancel")
    String cancel(@PathVariable String id, @RequestParam String reason, HttpSession session) {
        String customer = SessionAccess.customer(session); if(customer==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        double fee=operations.cancelBooking(id, customer, reason, customer); return "redirect:/reservationHistory?cancelFee="+fee;
    }

    @GetMapping("/api/bookings/{id}/quote") @ResponseBody
    OperationsService.Quote quote(@PathVariable String id, HttpSession session) {
        if (SessionAccess.isStaff(session)) return operations.quote(id);
        String customer = SessionAccess.customer(session);
        if (customer == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try { return operations.quoteForCustomer(id, customer); }
        catch (SecurityException ex) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage()); }
    }

    @GetMapping("/admin/vehicles/{id}/history")
    String vehicleHistory(@PathVariable String id, HttpSession session, Model model) {
        requireStaff(session); model.addAttribute("vehicle", vehicles.getVehicleById(id));
        model.addAttribute("events", operations.vehicleTimeline(id)); return "vehicle-history";
    }

    @PostMapping("/contact")
    String contact(@RequestParam String name,@RequestParam String email,@RequestParam String subject,
                   @RequestParam String message,HttpSession session) {
        try {
            operations.saveEnquiry(SessionAccess.customer(session),name,email,subject,message);
            return "redirect:/contact?sent=true";
        } catch (IllegalArgumentException ex) {
            return "redirect:/contact?error=" + java.net.URLEncoder.encode(ex.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    @GetMapping("/admin/contact-enquiries")
    String enquiries(HttpSession session, Model model) { requireStaff(session); model.addAttribute("enquiries",operations.enquiries()); return "contact-enquiries"; }
    @PostMapping("/admin/contact-enquiries/{id}/resolve")
    String resolve(@PathVariable long id,HttpSession session){requireStaff(session);operations.resolveEnquiry(id,staff(session));return "redirect:/admin/contact-enquiries";}

    @PostMapping("/admin/staff/{id}/branch")
    String staffBranch(@PathVariable String id,@RequestParam String branch,HttpSession session){requireStaff(session);operations.assignStaffBranch(id,branch,staff(session));return "redirect:/admin/dashboard";}

    @PostMapping("/admin/inspections/{id}/void")
    String voidInspection(@PathVariable String id,@RequestParam String reason,HttpSession session){requireStaff(session);operations.voidInspection(id,reason,staff(session));return "redirect:/handover";}

    private void requireStaff(HttpSession s){if(!SessionAccess.isStaff(s))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
    private String staff(HttpSession s){Object actor=s.getAttribute("loggedInAdmin");return actor instanceof String ? (String)actor : "system";}

    private void addPage(Model model, String key, java.util.List<?> items, int requested, int pageSize) {
        int pages = Math.max(1, (items.size() + pageSize - 1) / pageSize);
        int current = Math.max(1, Math.min(requested, pages));
        int start = (current - 1) * pageSize;
        model.addAttribute(key, items.subList(start, Math.min(start + pageSize, items.size())));
        model.addAttribute("page", current);
        model.addAttribute("pages", pages);
        model.addAttribute("total", items.size());
        model.addAttribute("from", items.isEmpty() ? 0 : start + 1);
        model.addAttribute("to", Math.min(start + pageSize, items.size()));
    }
}
