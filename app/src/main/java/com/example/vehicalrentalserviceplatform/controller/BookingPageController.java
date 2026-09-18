package com.example.vehicalrentalserviceplatform.controller;

import com.example.vehicalrentalserviceplatform.model.Booking;
import com.example.vehicalrentalserviceplatform.service.BookingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BookingPageController {

    @Autowired
    private BookingService bookingService;

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
                                  HttpSession session, Model model) {

        String currentUser = (String) session.getAttribute("loggedInUser");

        if(currentUser == null){
            return "redirect:/login";
        }

        model.addAttribute("username", currentUser);
        model.addAttribute("vehicleId", vehicleId);

        if("overlap".equals(error)){
            model.addAttribute("errorMessage","Sorry! This vehicle is already booked for the selected dates. Please choose different dates or a different vehicle.");
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

        if (transactionId != null) {
            Booking booking = bookingService.getBookingById(transactionId);
            if (booking != null) {
                model.addAttribute("transactionId", booking.getTransactionId());
                model.addAttribute("vehicleId", booking.getVehicleId());
                model.addAttribute("startDate", booking.getStartDate());
                model.addAttribute("returnDate", booking.getReturnDate());
            }
        }

        if ("overlap".equals(error)) {
            model.addAttribute("errorMessage", "Sorry! This vehicle is already booked for those new dates. Please choose different dates.");
        }

        return "editBooking";
    }
}