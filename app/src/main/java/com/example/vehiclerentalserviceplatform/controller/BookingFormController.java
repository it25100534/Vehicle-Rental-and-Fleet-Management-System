package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.model.Booking;
import com.example.vehiclerentalserviceplatform.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.example.vehiclerentalserviceplatform.security.SessionAccess;
import com.example.vehiclerentalserviceplatform.security.InputValidation;
import com.example.vehiclerentalserviceplatform.service.VehicleService;
import com.example.vehiclerentalserviceplatform.fleet.BranchService;
import com.example.vehiclerentalserviceplatform.service.OperationsService;
import com.example.vehiclerentalserviceplatform.model.Vehicle;
import java.util.Comparator;

@Controller
public class BookingFormController {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private BranchService branchService;
    @Autowired private OperationsService operations;

    @PostMapping("/createBooking")
    public String createBooking(
            @RequestParam("customerUsername") String name,
            @RequestParam("vehicleId") String vehicleId,
            @RequestParam("startDate") String startDate,
            @RequestParam("returnDate") String returnDate,
            @RequestParam("pickupBranch") String pickupBranch,
            @RequestParam("dropoffBranch") String dropoffBranch,
            @RequestParam(value="extrasTotal", defaultValue="0") double extrasTotal,
            @RequestParam(value="promoCode", required=false) String promoCode,
            HttpSession session) {

        name = requireCustomer(session);
        if (!operations.isLicenceVerified(name)) {
            return "redirect:/bookVehicle?id=" + vehicleId + "&error=licence";
        }

        var vehicle = vehicleService.getVehicleById(vehicleId);
        if (vehicle == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle is not available for booking");
        }

        try {
            InputValidation.requireBookingDates(startDate, returnDate);
        } catch (IllegalArgumentException ex) {
            return "redirect:/bookVehicle?id=" + vehicleId + "&error=invalidDates";
        }
        if (!branchService.isOpen(pickupBranch) || !branchService.isOpen(dropoffBranch)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select valid pickup and drop-off branches");
        }
        if (java.util.Set.of(0d, 1500d, 2500d, 4000d).stream().noneMatch(value -> Double.compare(value, extrasTotal) == 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a valid optional extras package");
        }

        String requestedUnitId = vehicleId;
        String modelId = requestedUnitId.replaceFirst("-U[23]$", "");
        String assignedUnitId = vehicleService.getAllVehicles().stream()
                .filter(candidate -> candidate.getVehicleId().replaceFirst("-U[23]$", "").equals(modelId))
                .filter(Vehicle::isAvailable)
                .filter(candidate -> branchService.isOpen(candidate.getCurrentBranch()))
                .filter(candidate -> bookingService.isVehicleAvailable(candidate.getVehicleId(), startDate, returnDate))
                .sorted(Comparator
                        .comparingInt((Vehicle candidate) -> candidate.getCurrentBranch().equalsIgnoreCase(pickupBranch) ? 0 : 1)
                        .thenComparingInt(candidate -> candidate.getVehicleId().equals(requestedUnitId) ? 0 : 1)
                        .thenComparing(Vehicle::getVehicleId))
                .map(Vehicle::getVehicleId)
                .findFirst().orElse(null);
        if (assignedUnitId == null) {
            return "redirect:/bookVehicle?id=" + vehicleId + "&error=overlap";
        }

        Booking newBooking = new Booking(name, assignedUnitId, startDate, returnDate, "Pending");
        boolean isSuccess =  bookingService.createBooking(newBooking);

        if(!isSuccess){
            return "redirect:/bookVehicle?id=" + vehicleId + "&error=overlap";
        }
        operations.enrichBooking(newBooking.getTransactionId(), pickupBranch, dropoffBranch, extrasTotal, promoCode);

        return "redirect:/reservationHistory";
    }

    @PostMapping("/updateBooking")
    public String updateBooking(
            @RequestParam("transactionId") String transactionId,
            @RequestParam("vehicleId") String newVehicleId,
            @RequestParam("startDate") String newStartDate,
            @RequestParam("returnDate") String newReturnDate,
            HttpSession session) {

        Booking existing = bookingService.getBookingById(transactionId);
        requireOwnership(existing, session);
        if (!"Pending".equalsIgnoreCase(existing.getBookingStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending bookings can be edited");
        }
        if (!existing.getVehicleId().equals(newVehicleId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The vehicle cannot be changed during booking edit");
        }
        try {
            InputValidation.requireBookingDates(newStartDate, newReturnDate);
        } catch (IllegalArgumentException ex) {
            return "redirect:/editBooking?id=" + transactionId + "&error=invalidDates";
        }
        String secureStatus = (existing != null) ? existing.getBookingStatus() : "Pending";

        boolean isSuccess = bookingService.updateBooking(transactionId,newVehicleId,newStartDate,newReturnDate,secureStatus);

        if(!isSuccess){
            return "redirect:/editBooking?id=" + transactionId + "&vehicle=" + newVehicleId + "&start=" + newStartDate + "&end=" + newReturnDate + "&error=overlap";
        }
        return "redirect:/reservationHistory";
    }

    @PostMapping("/deleteBooking")
    public String deleteBooking(@RequestParam("transactionId") String targetID, HttpSession session) {

        requireOwnership(bookingService.getBookingById(targetID), session);

        operations.cancelBooking(targetID, requireCustomer(session), "Cancelled by customer", requireCustomer(session));

        return "redirect:/reservationHistory";
    }

    @PostMapping("/admin/approveBooking")
    public String approveBooking(@RequestParam("transactionId") String transactionId) {
        boolean updated = bookingService.updateBookingStatus(transactionId, "Approved");
        return "redirect:/admin/dashboard" + (updated ? "" : "?message=Invalid%20booking%20status%20change.");
    }

    @PostMapping("/admin/rejectBooking")
    public String rejectBooking(@RequestParam("transactionId") String transactionId) {
        boolean updated = bookingService.updateBookingStatus(transactionId, "Rejected");
        return "redirect:/admin/dashboard" + (updated ? "" : "?message=Invalid%20booking%20status%20change.");
    }

    private String requireCustomer(HttpSession session) {
        String customer = SessionAccess.customer(session);
        if (customer == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return customer;
    }

    private void requireOwnership(Booking booking, HttpSession session) {
        String customer = requireCustomer(session);
        if (booking == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        if (!customer.equalsIgnoreCase(booking.getCustomerName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This booking belongs to another customer");
        }
    }


}
