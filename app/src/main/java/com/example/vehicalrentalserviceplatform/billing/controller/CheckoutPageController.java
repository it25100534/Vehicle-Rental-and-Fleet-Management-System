package com.example.vehicalrentalserviceplatform.billing.controller;

import com.example.vehicalrentalserviceplatform.model.Booking;
import com.example.vehicalrentalserviceplatform.model.Vehicle;
import com.example.vehicalrentalserviceplatform.service.BookingService;
import com.example.vehicalrentalserviceplatform.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
public class CheckoutPageController {

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private BookingService bookingService;

    private boolean isNotLoggedIn(HttpSession session) {
        return session.getAttribute("loggedInUser") == null;
    }

    @GetMapping("/checkout")
    public String showCheckout(
            @RequestParam(required = false) String transactionId, // Catching the Transaction ID instead!
            HttpSession session,
            Model model) {

        if (isNotLoggedIn(session)) {
            return "redirect:/login";
        }

        String customerName = (String) session.getAttribute("loggedInUser");
        model.addAttribute("customerName", customerName);

        model.addAttribute("transactionId", transactionId);

        if (transactionId != null && !transactionId.isEmpty()) {

            Booking currentBooking = null;
            List<Booking> allBookings = bookingService.getAllBookings();
            for (Booking b : allBookings) {
                if (b.getTransactionId().equals(transactionId)) {
                    currentBooking = b;
                    break;
                }
            }

            if (currentBooking != null) {
                model.addAttribute("vehicleId", currentBooking.getVehicleId());

                long days = 1;
                try {
                    LocalDate start = LocalDate.parse(currentBooking.getStartDate());
                    LocalDate end = LocalDate.parse(currentBooking.getReturnDate());
                    days = ChronoUnit.DAYS.between(start, end);
                    if (days < 1) days = 1; // Minimum 1 day charge
                } catch (Exception ignored) {
                }
                model.addAttribute("rentalDays", days);

                Vehicle vehicle = vehicleService.getVehicleById(currentBooking.getVehicleId());
                if (vehicle != null) {
                    model.addAttribute("vehicleName", vehicle.getMake() + " " + vehicle.getModel());
                    model.addAttribute("amountPerDay", vehicle.getRentalRate());
                } else {
                    model.addAttribute("vehicleName", "Vehicle Not Found");
                    model.addAttribute("amountPerDay", 0.0);
                }
            } else {
                model.addAttribute("vehicleId", "Unknown");
                model.addAttribute("vehicleName", "Booking Not Found");
                model.addAttribute("amountPerDay", 0.0);
                model.addAttribute("rentalDays", 1);
            }
        } else {
            model.addAttribute("vehicleId", "");
            model.addAttribute("vehicleName", "");
            model.addAttribute("amountPerDay", 0.0);
            model.addAttribute("rentalDays", 1);
        }

        return "checkout";
    }
}