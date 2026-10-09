package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.model.Invoice;
import jakarta.validation.Valid;
import com.example.vehiclerentalserviceplatform.service.BillingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import jakarta.servlet.http.HttpSession;
import com.example.vehiclerentalserviceplatform.security.SessionAccess;
import com.example.vehiclerentalserviceplatform.model.Booking;
import com.example.vehiclerentalserviceplatform.model.Vehicle;
import com.example.vehiclerentalserviceplatform.service.BookingService;
import com.example.vehiclerentalserviceplatform.service.VehicleService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import com.example.vehiclerentalserviceplatform.service.OperationsService;

@RestController
@RequestMapping("/api/invoices")
public class BillingController {

    private final BillingService billingService;
    private final BookingService bookingService;
    private final VehicleService vehicleService;
    private final OperationsService operations;

    public BillingController(BillingService billingService, BookingService bookingService,
                             VehicleService vehicleService, OperationsService operations) {
        this.billingService = billingService;
        this.bookingService = bookingService;
        this.vehicleService = vehicleService;
        this.operations = operations;
    }

    @GetMapping
    public List<Invoice> allInvoices() {
        return billingService.getAllInvoices();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invoice> getInvoice(@PathVariable Long id) {
        return billingService.getInvoiceById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Invoice> createInvoice(@Valid @RequestBody Invoice invoice, HttpSession session,
                                                 @RequestHeader(value="Idempotency-Key", required=false) String idempotencyKey) {
        String customer = SessionAccess.customer(session);
        if (customer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Booking booking = bookingService.getBookingById(invoice.getTransactionId());
        if (booking == null) {
            throw new IllegalArgumentException("A valid booking is required for payment.");
        }
        if (!customer.equalsIgnoreCase(booking.getCustomerName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!"Approved".equalsIgnoreCase(booking.getBookingStatus())) {
            throw new IllegalArgumentException("Booking must be approved before payment.");
        }
        if (billingService.getInvoiceByTransactionId(booking.getTransactionId()).isPresent()) {
            throw new IllegalStateException("This booking has already been paid.");
        }
        String key = (idempotencyKey == null || idempotencyKey.isBlank()) ? "booking:" + booking.getTransactionId() : idempotencyKey;
        if (operations.paymentKeyExists(key)) throw new IllegalStateException("This payment request was already processed.");

        Vehicle vehicle = vehicleService.getVehicleById(booking.getVehicleId());
        if (vehicle == null) {
            throw new IllegalArgumentException("The booked vehicle no longer exists.");
        }

        applyServerInvoiceData(invoice, customer, booking, vehicle);
        OperationsService.Quote quote = operations.quote(booking.getTransactionId());
        invoice.setAmountPerDay((quote.rentalSubtotal() + quote.extras()) / invoice.getRentalDays());
        invoice.setDiscount(quote.discount()); invoice.setDepositAmount(quote.deposit());
        invoice.setAlternateDropoffCharge(quote.alternateDropoff());
        try {
            Invoice created = billingService.createInvoice(invoice);
            operations.recordPaymentAttempt(booking.getTransactionId(), created.getId(), key, quote.total(), invoice.getPaymentMethod().getType(), true, null);
            bookingService.updateBookingStatus(booking.getTransactionId(), "Paid");
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException ex) {
            operations.recordPaymentAttempt(booking.getTransactionId(), null, key, quote.total(), invoice.getPaymentMethod()==null?"UNKNOWN":invoice.getPaymentMethod().getType(), false, ex.getMessage());
            throw ex;
        }
    }

    static void applyServerInvoiceData(Invoice invoice, String customer, Booking booking, Vehicle vehicle) {
        long rentalDays = ChronoUnit.DAYS.between(
                LocalDate.parse(booking.getStartDate()), LocalDate.parse(booking.getReturnDate()));
        if (rentalDays < 1 || rentalDays > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Booking dates produce an invalid rental duration.");
        }

        invoice.setCustomerName(customer);
        invoice.setVehicleId(vehicle.getVehicleId());
        invoice.setVehicleName(vehicle.getMake() + " " + vehicle.getModel());
        invoice.setRentalDays((int) rentalDays);
        invoice.setAmountPerDay(vehicle.getRentalRate());
        invoice.setBranchId(vehicle.getCurrentBranch());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Invoice> updateInvoice(@PathVariable Long id, @Valid @RequestBody Invoice invoice) {
        Invoice updated = billingService.updateInvoice(id, invoice);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }
}
