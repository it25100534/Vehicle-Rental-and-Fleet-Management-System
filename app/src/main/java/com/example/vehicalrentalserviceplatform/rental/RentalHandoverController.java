package com.example.vehicalrentalserviceplatform.rental;

import com.example.vehicalrentalserviceplatform.model.Booking;
import com.example.vehicalrentalserviceplatform.model.Vehicle;
import com.example.vehicalrentalserviceplatform.service.BookingService;
import com.example.vehicalrentalserviceplatform.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/handover")
public class RentalHandoverController {
    private final RentalHandoverService rentalService;
    private final BookingService bookingService;
    private final VehicleService vehicleService;

    public RentalHandoverController(RentalHandoverService rentalService,
                                    BookingService bookingService,
                                    VehicleService vehicleService) {
        this.rentalService = rentalService;
        this.bookingService = bookingService;
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public String page(HttpSession session, Model model) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        List<RentalHandover> records = rentalService.getAll();
        List<Booking> eligibleBookings = bookingService.getAllBookings().stream()
                .filter(booking -> isReadyForHandover(booking.getBookingStatus()))
                .filter(booking -> records.stream().noneMatch(record -> record.isActive()
                        && record.getBookingId().equals(booking.getTransactionId())))
                .toList();
        model.addAttribute("records", records);
        model.addAttribute("activeRecords", records.stream().filter(RentalHandover::isActive).toList());
        model.addAttribute("eligibleBookings", eligibleBookings);
        return "handover";
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
            if (vehicleService.getVehicleById(booking.getVehicleId()) == null) {
                throw new IllegalArgumentException("The vehicle assigned to this booking was not found.");
            }
            rentalService.create(bookingId, booking.getCustomerName(), booking.getVehicleId(),
                    LocalDate.parse(booking.getReturnDate()), odometerOut, fuelLevelOut, conditionOut, actor(session));
            setVehicleAvailability(booking.getVehicleId(), false);
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
                                 HttpSession session, RedirectAttributes redirect) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        try {
            RentalHandover record = rentalService.completeReturn(recordId, odometerIn, fuelLevelIn,
                    conditionIn, lateFee, actor(session));
            setVehicleAvailability(record.getVehicleId(), true);
            bookingService.updateBookingStatus(record.getBookingId(), "Returned");
            redirect.addFlashAttribute("message", "Vehicle return recorded successfully.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/handover";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam String recordId, HttpSession session,
                         RedirectAttributes redirect) {
        if (!hasStaffAccess(session)) return "redirect:/admin-login";
        try {
            RentalHandover deleted = rentalService.delete(recordId, actor(session));
            if (deleted.isActive()) {
                setVehicleAvailability(deleted.getVehicleId(), true);
                bookingService.updateBookingStatus(deleted.getBookingId(), "Approved");
            }
            redirect.addFlashAttribute("message", "Rental record deleted.");
        } catch (RuntimeException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/handover";
    }

    private void setVehicleAvailability(String vehicleId, boolean available) {
        Vehicle vehicle = vehicleService.getVehicleById(vehicleId);
        if (vehicle != null) {
            vehicle.setAvailable(available);
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
