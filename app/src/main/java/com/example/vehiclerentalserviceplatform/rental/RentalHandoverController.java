package com.example.vehiclerentalserviceplatform.rental;

import com.example.vehiclerentalserviceplatform.model.Booking;
import com.example.vehiclerentalserviceplatform.model.Vehicle;
import com.example.vehiclerentalserviceplatform.service.BookingService;
import com.example.vehiclerentalserviceplatform.service.VehicleService;
import com.example.vehiclerentalserviceplatform.service.BillingService;
import com.example.vehiclerentalserviceplatform.service.OperationsService;
import com.example.vehiclerentalserviceplatform.security.InputValidation;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/handover")
public class RentalHandoverController {
    private final RentalHandoverService rentalService;
    private final BookingService bookingService;
    private final VehicleService vehicleService;
    private final BillingService billingService;
    private final OperationsService operations;

    public RentalHandoverController(RentalHandoverService rentalService,
                                    BookingService bookingService,
                                    VehicleService vehicleService,
                                    BillingService billingService, OperationsService operations) {
        this.rentalService = rentalService;
        this.bookingService = bookingService;
        this.vehicleService = vehicleService;
        this.billingService = billingService;
        this.operations = operations;
    }

    @GetMapping
    public String page(@RequestParam(defaultValue = "1") int page, HttpSession session, Model model) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        List<RentalHandover> records = rentalService.getAll();
        List<Booking> eligibleBookings = bookingService.getAllBookings().stream()
                .filter(booking -> isReadyForHandover(booking.getBookingStatus()))
                .filter(booking -> records.stream().noneMatch(record -> record.isActive()
                        && record.getBookingId().equals(booking.getTransactionId())))
                .toList();
        java.util.Map<String, Double> bookingOdometers = new java.util.HashMap<>();
        for (Booking booking : eligibleBookings) {
            Vehicle vehicle = vehicleService.getVehicleById(booking.getVehicleId());
            if (vehicle != null) bookingOdometers.put(booking.getTransactionId(), vehicle.getMileage());
        }
        var ordered = new java.util.ArrayList<>(records);
        ordered.sort(java.util.Comparator.comparing(RentalHandover::isActive).reversed()
                .thenComparing(RentalHandover::getHandedOverAt, java.util.Comparator.reverseOrder()));
        int pages = Math.max(1, (ordered.size() + 19) / 20);
        int current = Math.max(1, Math.min(page, pages));
        int start = (current - 1) * 20;
        model.addAttribute("records", ordered.subList(start, Math.min(start + 20, ordered.size())));
        model.addAttribute("recordCount", records.size());
        model.addAttribute("page", current);
        model.addAttribute("pages", pages);
        model.addAttribute("activeRecords", records.stream().filter(RentalHandover::isActive).toList());
        model.addAttribute("overdueRecords", records.stream().filter(RentalHandover::isOverdue).toList());
        model.addAttribute("returnedRecords", records.stream().filter(record -> !record.isActive()).toList());
        model.addAttribute("eligibleBookings", eligibleBookings);
        model.addAttribute("bookingOdometers", bookingOdometers);
        return "handover";
    }

    @GetMapping("/overdue")
    public String overdue(HttpSession session, Model model) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        List<RentalHandover> records = rentalService.getAll().stream()
                .filter(RentalHandover::isOverdue).toList();
        model.addAttribute("records", records);
        return "overdue-rentals";
    }

    @GetMapping("/active")
    public String active(HttpSession session, Model model) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        List<RentalHandover> records = rentalService.getAll().stream()
                .filter(RentalHandover::isActive).toList();
        model.addAttribute("records", records);
        return "active-rentals";
    }

    @GetMapping("/{recordId}")
    public String inspection(@PathVariable String recordId, HttpSession session, Model model) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        RentalHandover record = rentalService.findById(recordId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rental record was not found"));
        model.addAttribute("record", record);
        model.addAttribute("booking", bookingService.getBookingById(record.getBookingId()));
        model.addAttribute("vehicle", vehicleService.getVehicleById(record.getVehicleId()));
        return "rental-inspection";
    }

    @PostMapping("/create")
    public String create(@RequestParam String bookingId,
                         @RequestParam double odometerOut,
                         @RequestParam String fuelLevelOut,
                         @RequestParam(defaultValue = "") String conditionOut,
                         HttpSession session, RedirectAttributes redirect) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        try {
            Booking booking = bookingService.getBookingById(bookingId);
            if (booking == null) throw new IllegalArgumentException("The selected booking was not found.");
            if (!isReadyForHandover(booking.getBookingStatus())) {
                throw new IllegalArgumentException("Only approved or paid bookings can be handed over.");
            }
            Vehicle vehicle = vehicleService.getVehicleById(booking.getVehicleId());
            if (vehicle == null) {
                throw new IllegalArgumentException("The vehicle assigned to this booking was not found.");
            }
            if (!vehicle.isAvailable()) {
                throw new IllegalArgumentException("The vehicle is currently unavailable.");
            }
            if (odometerOut < vehicle.getMileage()) {
                throw new IllegalArgumentException("Outgoing odometer cannot be lower than the vehicle's recorded mileage.");
            }
            rentalService.create(bookingId, booking.getCustomerName(), booking.getVehicleId(),
                    LocalDate.parse(booking.getReturnDate()), odometerOut, fuelLevelOut, conditionOut, actor(session));
            updateVehicleState(booking.getVehicleId(), false, odometerOut);
            bookingService.updateBookingStatus(bookingId, "Active");
            redirect.addFlashAttribute("message", "Vehicle handover recorded successfully.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/handover";
    }

    @PostMapping("/update")
    public String update(@RequestParam String recordId,
                         @RequestParam String scheduledReturnDate,
                         @RequestParam double odometerOut,
                         @RequestParam String fuelLevelOut,
                         @RequestParam(defaultValue = "") String conditionOut,
                         HttpSession session, RedirectAttributes redirect) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        try {
            rentalService.update(recordId, LocalDate.parse(scheduledReturnDate), odometerOut,
                    fuelLevelOut, conditionOut, actor(session));
            redirect.addFlashAttribute("message", "Handover details updated.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/handover";
    }

    @PostMapping("/return")
    public String completeReturn(@RequestParam String recordId,
                                 @RequestParam double odometerIn,
                                 @RequestParam String fuelLevelIn,
                                 @RequestParam(defaultValue = "") String conditionIn,
                                 @RequestParam(defaultValue = "0") double lateFee,
                                 @RequestParam(defaultValue = "0") double damageFee,
                                 @RequestParam(defaultValue = "0") double fuelCharge,
                                 @RequestParam(defaultValue = "0") double mileageCharge,
                                 @RequestParam(defaultValue = "0") double alternateDropoffCharge,
                                 @RequestParam(defaultValue = "0") double depositAmount,
                                 @RequestParam(defaultValue = "0") double refundAmount,
                                 HttpSession session, RedirectAttributes redirect) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        try {
            RentalHandover current = rentalService.findById(recordId)
                    .orElseThrow(() -> new IllegalArgumentException("Rental record was not found."));
            Booking booking = bookingService.getBookingById(current.getBookingId());
            if (booking == null) throw new IllegalArgumentException("The linked booking was not found.");
            if (!current.isActive()) throw new IllegalArgumentException("This rental has already been returned.");
            InputValidation.requireReturn(odometerIn, fuelLevelIn, conditionIn, lateFee);
            if (odometerIn < current.getOdometerOut()) {
                throw new IllegalArgumentException("Return odometer cannot be lower than handover odometer.");
            }
            boolean invoiceLinked = billingService.findInvoiceForBooking(current.getBookingId(),
                    current.getCustomerName(), current.getVehicleId()).isPresent();
            if (invoiceLinked) {
                billingService.applyReturnAdjustments(current.getBookingId(), current.getCustomerName(),
                        current.getVehicleId(), lateFee, damageFee, fuelCharge, mileageCharge,
                        alternateDropoffCharge, depositAmount, refundAmount);
            }
            RentalHandover record = rentalService.completeReturn(recordId, odometerIn, fuelLevelIn,
                    conditionIn, lateFee, damageFee, fuelCharge, mileageCharge,
                    alternateDropoffCharge, depositAmount, refundAmount, actor(session));
            updateVehicleState(record.getVehicleId(), true, odometerIn);
            bookingService.updateBookingStatus(record.getBookingId(), "Returned");
            redirect.addFlashAttribute("message", invoiceLinked
                    ? "Vehicle return recorded and Billing updated successfully."
                    : "Vehicle return recorded. Charges are saved with the rental; no paid invoice is linked yet.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/handover";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam String recordId, @RequestParam(defaultValue="Incorrect inspection entry") String reason, HttpSession session,
                         RedirectAttributes redirect) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        try {
            operations.voidInspection(recordId, reason, actor(session));
            redirect.addFlashAttribute("message", "Inspection voided and retained in the audit trail.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/handover";
    }

    private void updateVehicleState(String vehicleId, boolean available, Double mileage) {
        Vehicle vehicle = vehicleService.getVehicleById(vehicleId);
        if (vehicle != null) {
            vehicle.setAvailable(available);
            if (mileage != null) {
                vehicle.setMileage(mileage);
            }
            vehicleService.updateVehicle(vehicle);
        }
    }

    private boolean hasStaffAccess(HttpSession session) {
        String role = (String) session.getAttribute("role");
        return "ADMIN".equals(role) || "STAFF".equals(role);
    }

    private boolean isReadyForHandover(String status) {
        return "Approved".equalsIgnoreCase(status) || "Paid".equalsIgnoreCase(status);
    }

    private String actor(HttpSession session) {
        Object username = session.getAttribute("loggedInAdmin");
        return username == null ? "system-user" : username.toString();
    }
}
