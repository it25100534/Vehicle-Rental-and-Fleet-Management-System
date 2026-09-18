package com.example.vehicalrentalserviceplatform.controller;

import com.example.vehicalrentalserviceplatform.model.Booking;
import com.example.vehicalrentalserviceplatform.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BookingApiController {

    @Autowired
    private  BookingService bookingService;

    @GetMapping("/api/bookings")
    public List<Booking> getBookings(@RequestParam(value = "customer", required = false) String customer) {

        if (customer != null && !customer.isEmpty()) {
            return bookingService.getBookingsByCustomer(customer);
        }

        return bookingService.getAllBookings();
    }
}