package com.example.vehiclerentalserviceplatform.service;

import com.example.vehiclerentalserviceplatform.maintenance.MaintenanceFileHandler;
import com.example.vehiclerentalserviceplatform.model.Booking;
import com.example.vehiclerentalserviceplatform.persistence.BookingEntity;
import com.example.vehiclerentalserviceplatform.persistence.BookingRepository;
import com.example.vehiclerentalserviceplatform.security.InputValidation;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Service
@DependsOn({"customerFileService", "vehicleService", "bookingSchemaMigration"})
public class BookingService {

    private static final String LEGACY_FILE_PATH = "booking.txt";
    private final BookingRepository repository;
    private final MaintenanceFileHandler maintenance;
    private final NotificationEventService notifications;

    public BookingService(BookingRepository repository, MaintenanceFileHandler maintenance,
                          NotificationEventService notifications) {
        this.repository = repository;
        this.maintenance = maintenance;
        this.notifications = notifications;
        importLegacyBookingsWhenEmpty();
    }

    public boolean isVehicleAvailable(String vehicleId, String requestedStartDate, String requestedReturnDate) {
        InputValidation.requireBookingDates(requestedStartDate, requestedReturnDate);
        LocalDate requestedStart = LocalDate.parse(requestedStartDate);
        LocalDate requestedReturn = LocalDate.parse(requestedReturnDate);

        if (maintenance.hasBookingConflict(vehicleId, requestedStart, requestedReturn)) return false;

        return getAllBookings().stream()
                .filter(booking -> booking.getVehicleId().equalsIgnoreCase(vehicleId))
                .filter(booking -> blocksVehicle(booking.getBookingStatus()))
                .noneMatch(booking -> overlaps(requestedStart, requestedReturn,
                        LocalDate.parse(booking.getStartDate()), LocalDate.parse(booking.getReturnDate())));
    }

    public boolean isVehicleAvailableForUpdate(String transactionId, String vehicleId,
                                               String requestedStartDate, String requestedReturnDate) {
        InputValidation.requireBookingDates(requestedStartDate, requestedReturnDate);
        LocalDate requestedStart = LocalDate.parse(requestedStartDate);
        LocalDate requestedReturn = LocalDate.parse(requestedReturnDate);

        if (maintenance.hasBookingConflict(vehicleId, requestedStart, requestedReturn)) return false;

        return getAllBookings().stream()
                .filter(booking -> !booking.getTransactionId().equals(transactionId))
                .filter(booking -> booking.getVehicleId().equalsIgnoreCase(vehicleId))
                .filter(booking -> blocksVehicle(booking.getBookingStatus()))
                .noneMatch(booking -> overlaps(requestedStart, requestedReturn,
                        LocalDate.parse(booking.getStartDate()), LocalDate.parse(booking.getReturnDate())));
    }

    @Transactional
    public boolean createBooking(Booking booking) {
        InputValidation.requireBookingDates(booking.getStartDate(), booking.getReturnDate());
        if (!isVehicleAvailable(booking.getVehicleId(), booking.getStartDate(), booking.getReturnDate())) return false;
        repository.save(toEntity(booking));
        notifications.staff("New booking request", booking.getCustomerName() + " requested " + booking.getVehicleId()
                + " for " + booking.getStartDate() + " to " + booking.getReturnDate() + ".", "/admin/bookings?status=Pending");
        return true;
    }

    public List<Booking> getAllBookings() {
        return repository.findAll().stream().map(this::toBooking).toList();
    }

    public List<Booking> getBookingsByCustomer(String customerName) {
        return repository.findByCustomerUsernameIgnoreCase(customerName).stream().map(this::toBooking).toList();
    }

    public String getCurrentVehicleBookingStatus(String vehicleId) {
        LocalDate today = LocalDate.now();
        String result = null;
        for (Booking booking : getAllBookings()) {
            if (!booking.getVehicleId().equalsIgnoreCase(vehicleId)
                    || !blocksVehicle(booking.getBookingStatus())) continue;
            LocalDate start = LocalDate.parse(booking.getStartDate());
            LocalDate end = LocalDate.parse(booking.getReturnDate());
            if (!today.isBefore(start) && !today.isAfter(end)) {
                if ("Active".equalsIgnoreCase(booking.getBookingStatus())) return "RENTED";
                result = "BOOKED";
            }
        }
        return result;
    }

    @Transactional
    public boolean updateBooking(String transactionId, String newVehicleId, String newStartDate,
                                 String newReturnDate, String newStatus) {
        InputValidation.requireBookingDates(newStartDate, newReturnDate);
        if (!isVehicleAvailableForUpdate(transactionId, newVehicleId, newStartDate, newReturnDate)) return false;

        BookingEntity entity = repository.findById(transactionId).orElse(null);
        if (entity == null) return false;
        entity.setVehicleId(newVehicleId);
        entity.setStartDate(LocalDate.parse(newStartDate));
        entity.setReturnDate(LocalDate.parse(newReturnDate));
        entity.setStatus(newStatus);
        repository.save(entity);
        notifications.staff("Booking request updated", entity.getCustomerUsername() + " changed booking " + transactionId + ".",
                "/admin/bookings?status=Pending");
        return true;
    }

    @Transactional
    public boolean updateBookingStatus(String transactionId, String newStatus) {
        BookingEntity entity = repository.findById(transactionId).orElse(null);
        if (entity == null || !isAllowedStatusTransition(entity.getStatus(), newStatus)) return false;
        String previousStatus = entity.getStatus();
        entity.setStatus(newStatus);
        repository.save(entity);
        if (!previousStatus.equalsIgnoreCase(newStatus)
                && ("Approved".equalsIgnoreCase(newStatus) || "Rejected".equalsIgnoreCase(newStatus))) {
            notifications.customer(entity.getCustomerUsername(), "Booking " + newStatus,
                    entity.getVehicleId() + " · " + entity.getStartDate() + " to " + entity.getReturnDate(),
                    "/reservationHistory", "Approved".equalsIgnoreCase(newStatus) ? "success" : "warning");
        }
        return true;
    }

    public void deleteBooking(String transactionId) {
        repository.findById(transactionId)
                .filter(entity -> "Pending".equalsIgnoreCase(entity.getStatus()))
                .ifPresent(repository::delete);
    }

    public Booking getBookingById(String transactionId) {
        return repository.findById(transactionId).map(this::toBooking).orElse(null);
    }

    private boolean blocksVehicle(String status) {
        return status != null
                && !status.equalsIgnoreCase("Cancelled")
                && !status.equalsIgnoreCase("Rejected")
                && !status.equalsIgnoreCase("Returned")
                && !status.equalsIgnoreCase("Completed");
    }

    private boolean isAllowedStatusTransition(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) return false;
        if (currentStatus.equalsIgnoreCase(newStatus)) return true;
        return switch (currentStatus.toLowerCase()) {
            case "pending" -> newStatus.equalsIgnoreCase("Approved") || newStatus.equalsIgnoreCase("Rejected");
            case "approved" -> newStatus.equalsIgnoreCase("Paid") || newStatus.equalsIgnoreCase("Active");
            case "paid" -> newStatus.equalsIgnoreCase("Active");
            case "active" -> newStatus.equalsIgnoreCase("Returned") || newStatus.equalsIgnoreCase("Approved");
            default -> false;
        };
    }

    private boolean overlaps(LocalDate firstStart, LocalDate firstReturn,
                             LocalDate secondStart, LocalDate secondReturn) {
        return !firstStart.isAfter(secondReturn) && !firstReturn.isBefore(secondStart);
    }

    private BookingEntity toEntity(Booking booking) {
        return new BookingEntity(booking.getTransactionId(), booking.getCustomerName(), booking.getVehicleId(),
                LocalDate.parse(booking.getStartDate()), LocalDate.parse(booking.getReturnDate()),
                booking.getBookingStatus());
    }

    private Booking toBooking(BookingEntity entity) {
        return new Booking(entity.getTransactionId(), entity.getCustomerUsername(), entity.getVehicleId(),
                entity.getStartDate().toString(), entity.getReturnDate().toString(), entity.getStatus());
    }

    private void importLegacyBookingsWhenEmpty() {
        File file = new File(LEGACY_FILE_PATH);
        if (repository.count() != 0 || !file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",", -1);
                if (data.length != 6) continue;
                repository.save(new BookingEntity(data[0], data[1], data[2],
                        LocalDate.parse(data[3]), LocalDate.parse(data[4]), data[5]));
            }
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("Could not import legacy bookings.", ex);
        }
    }
}
