package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.model.Booking;
import com.example.vehiclerentalserviceplatform.service.BookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.example.vehiclerentalserviceplatform.service.VehicleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.example.vehiclerentalserviceplatform.fleet.BranchService;

@Controller
public class BookingPageController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private BranchService branchService;

    @GetMapping("/reservationHistory")
    public String showReservationHistory(HttpSession session, Model model) {
        String currentUser = (String) session.getAttribute("loggedInUser");

        if (currentUser == null) {
            return "redirect:/login";
        }

        model.addAttribute("username", currentUser);
        return "reservationHistory";
    }

    @GetMapping("/bookVehicle")
    public String showBookVehicle(@RequestParam(name = "id",required = false)String vehicleId,
                                  @RequestParam(name = "error",required = false)String error,
                                  @RequestParam(name = "startDate", required = false) String startDate,
                                  @RequestParam(name = "returnDate", required = false) String returnDate,
                                  HttpSession session, Model model) {

        String currentUser = (String) session.getAttribute("loggedInUser");

        if(currentUser == null){
            return "redirect:/login";
        }

        model.addAttribute("username", currentUser);
        var vehicle = vehicleId == null ? null : vehicleService.getVehicleById(vehicleId);
        if (vehicle == null || !vehicle.isAvailable()
                || !branchService.isOpen(vehicle.getCurrentBranch())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select an available vehicle before booking");
        }
        model.addAttribute("vehicleId", vehicleId);
        model.addAttribute("startDate", startDate);
        model.addAttribute("returnDate", returnDate);
        model.addAttribute("vehicle", vehicle);
        model.addAttribute("branches", branchService.getOpenBranches());

        if("overlap".equals(error)){
            model.addAttribute("errorMessage","This vehicle is booked or scheduled for maintenance on the selected dates. Please choose different dates or another vehicle.");
        } else if ("invalidDates".equals(error)) {
            model.addAttribute("errorMessage", "Start date cannot be in the past, and return date must be after the start date.");
        } else if ("licence".equals(error)) {
            model.addAttribute("errorMessage", "Your driving licence must be verified by staff before you can request a rental.");
            model.addAttribute("licenceError", true);
        }

        return "bookVehicle";

    }

    @GetMapping("/editBooking")
    public String showEditBooking(@RequestParam(name = "id", required = false) String transactionId,
                                  @RequestParam(name = "error", required = false) String error,
                                  HttpSession session, Model model) {

        String currentUsername = (String) session.getAttribute("loggedInUser");

        if (currentUsername == null) {
            return "redirect:/login";
        }

        if (transactionId == null || transactionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a booking to edit");
        }
        Booking booking = bookingService.getBookingById(transactionId);
        if (booking == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        if (!currentUsername.equalsIgnoreCase(booking.getCustomerName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This booking belongs to another customer");
        }
        if (!"Pending".equalsIgnoreCase(booking.getBookingStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending bookings can be edited");
        }
        model.addAttribute("transactionId", booking.getTransactionId());
        model.addAttribute("vehicleId", booking.getVehicleId());
        model.addAttribute("startDate", booking.getStartDate());
        model.addAttribute("returnDate", booking.getReturnDate());

        if ("overlap".equals(error)) {
            model.addAttribute("errorMessage", "This vehicle is booked or scheduled for maintenance on those dates. Please choose different dates.");
        } else if ("invalidDates".equals(error)) {
            model.addAttribute("errorMessage", "Start date cannot be in the past, and return date must be after the start date.");
        } else if ("licence".equals(error)) {
            model.addAttribute("errorMessage", "Your driving licence must be verified by staff before you can request a rental.");
            model.addAttribute("licenceError", true);
        }

        return "editBooking";
    }
}
