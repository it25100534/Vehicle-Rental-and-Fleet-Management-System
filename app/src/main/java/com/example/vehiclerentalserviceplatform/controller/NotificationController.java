package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.maintenance.FleetComplianceFileHandler;
import com.example.vehiclerentalserviceplatform.maintenance.MaintenanceFileHandler;
import com.example.vehiclerentalserviceplatform.model.Invoice;
import com.example.vehiclerentalserviceplatform.security.SessionAccess;
import com.example.vehiclerentalserviceplatform.service.BillingService;
import com.example.vehiclerentalserviceplatform.service.NotificationEventService;
import com.example.vehiclerentalserviceplatform.rental.RentalHandoverService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

@Controller
public class NotificationController {

    private final BillingService billingService;
    private final RentalHandoverService rentalService;
    private final MaintenanceFileHandler maintenance;
    private final FleetComplianceFileHandler compliance;
    private final NotificationEventService events;

    public NotificationController(BillingService billingService,
                                  RentalHandoverService rentalService, MaintenanceFileHandler maintenance,
                                  FleetComplianceFileHandler compliance, NotificationEventService events) {
        this.billingService = billingService;
        this.rentalService = rentalService;
        this.maintenance = maintenance;
        this.compliance = compliance;
        this.events = events;
    }

    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        boolean staff = SessionAccess.isStaff(session);
        String customer = SessionAccess.customer(session);
        List<NotificationItem> items = staff ? staffNotifications() : customerNotifications(customer);
        if (!staff) events.markCustomerRead(customer);
        model.addAttribute("staffView", staff);
        model.addAttribute("items", items);
        model.addAttribute("urgentCount", items.stream().filter(item -> "urgent".equals(item.severity())).count());
        model.addAttribute("pendingCount", staff ? events.pendingStaffCount() : 0);
        return "notifications";
    }

    @GetMapping("/api/customer/notifications/unread-count")
    @ResponseBody
    public java.util.Map<String, Integer> unreadCustomerCount(HttpSession session) {
        String username = SessionAccess.customer(session);
        if (username == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        return java.util.Map.of("count", events.unreadCustomerCount(username));
    }

    @GetMapping("/api/admin/notifications/pending-count")
    @ResponseBody
    public java.util.Map<String, Integer> pendingCount(HttpSession session) {
        if (!SessionAccess.isStaff(session)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);
        return java.util.Map.of("count", events.pendingStaffCount());
    }

    private List<NotificationItem> customerNotifications(String customer) {
        List<NotificationItem> items = new ArrayList<>(events.customerEvents(customer).stream()
                .map(event -> new NotificationItem(event.title(), event.message(), event.link(), event.severity())).toList());
        for (Invoice invoice : billingService.getInvoicesForCustomer(customer)) {
            String severity = switch (invoice.getStatus()) {
                case VOID -> "urgent";
                case REFUNDED, PARTIALLY_REFUNDED -> "warning";
                default -> "success";
            };
            items.add(new NotificationItem("Invoice " + invoice.getStatus(), "Invoice #" + invoice.getId()
                    + " · Rs. " + String.format("%.2f", invoice.getTotalAmount()),
                    "/invoices/" + invoice.getId(), severity));
        }
        return items;
    }

    private List<NotificationItem> staffNotifications() {
        List<NotificationItem> items = new ArrayList<>();
        events.pendingByType().forEach((type, count) -> {
            if (count > 0) {
                String link = switch (type) {
                    case "licence" -> "/admin/customers";
                    case "booking" -> "/admin/bookings?status=Pending";
                    default -> "/admin/contact-enquiries";
                };
                String title = switch (type) {
                    case "licence" -> "Driving licence reviews waiting";
                    case "booking" -> "Booking requests waiting";
                    default -> "Contact enquiries waiting";
                };
                items.add(new NotificationItem(title, count + " awaiting staff action", link, "urgent"));
            }
        });
        items.addAll(events.staffEvents().stream()
                .map(event -> new NotificationItem(event.title(), event.message(), event.link(), event.severity())).toList());
        rentalService.getAll().stream().filter(record -> record.isOverdue()).forEach(record ->
                items.add(new NotificationItem("Overdue rental", record.getVehicleId() + " is "
                        + record.getDaysOverdue() + " day(s) overdue", "/handover/overdue", "urgent")));
        maintenance.loadAllRecords().stream()
                .filter(record -> record.isDue() || record.isDueSoon()).forEach(record ->
                        items.add(new NotificationItem(record.isDue() ? "Maintenance due" : "Maintenance upcoming",
                                record.getMaintenanceAlert(), "/maintenance/schedule",
                                record.isDue() ? "urgent" : "warning")));
        compliance.loadAll().stream().filter(record -> !"VALID".equals(record.getState()))
                .forEach(record -> items.add(new NotificationItem("Compliance " + record.getState(),
                        record.getVehicleId() + " · nearest renewal in " + record.getNearestDaysRemaining() + " day(s)",
                        "/maintenance/compliance", "EXPIRED".equals(record.getState()) ? "urgent" : "warning")));
        billingService.getAllInvoices().stream()
                .filter(invoice -> invoice.getStatus().name().contains("REFUND") || "VOID".equals(invoice.getStatus().name()))
                .forEach(invoice -> items.add(new NotificationItem("Billing " + invoice.getStatus(),
                        "Invoice #" + invoice.getId() + " · " + invoice.getCustomerName(),
                        "/admin/invoices/" + invoice.getId(), "warning")));
        return items;
    }

    public record NotificationItem(String title, String message, String link, String severity) {}
}
