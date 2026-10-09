package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.model.Booking;
import com.example.vehiclerentalserviceplatform.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.example.vehiclerentalserviceplatform.security.SessionAccess;

@RestController
public class BookingApiController {

    @Autowired
    private  BookingService bookingService;

    @GetMapping("/api/bookings")
    public List<Booking> getBookings(@RequestParam(value = "customer", required = false) String customer,
                                     HttpSession session) {

        if (SessionAccess.isStaff(session)) {
            if (customer != null && !customer.isBlank()) {
                return bookingService.getBookingsByCustomer(customer);
            }
            return bookingService.getAllBookings();
        }

        String signedInCustomer = SessionAccess.customer(session);
        if (signedInCustomer == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        return bookingService.getBookingsByCustomer(signedInCustomer);
    }
}
