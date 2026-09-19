package com.example.vehicalrentalserviceplatform.rental;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RentalHandover {
    public static final String ACTIVE = "ACTIVE";
    public static final String RETURNED = "RETURNED";

    private final String recordId;
    private final String bookingId;
    private final String customerName;
    private final String vehicleId;
    private LocalDate scheduledReturnDate;
    private final LocalDateTime handedOverAt;
    private double odometerOut;
    private String fuelLevelOut;
    private String conditionOut;
    private String status;
    private LocalDateTime returnedAt;
    private Double odometerIn;
    private String fuelLevelIn;
    private String conditionIn;
    private double lateFee;

    public RentalHandover(String recordId, String bookingId, String customerName,
                          String vehicleId, LocalDate scheduledReturnDate,
                          LocalDateTime handedOverAt, double odometerOut,
                          String fuelLevelOut, String conditionOut, String status,
                          LocalDateTime returnedAt, Double odometerIn,
                          String fuelLevelIn, String conditionIn, double lateFee) {
        this.recordId = recordId;
        this.bookingId = bookingId;
        this.customerName = customerName;
        this.vehicleId = vehicleId;
        this.scheduledReturnDate = scheduledReturnDate;
        this.handedOverAt = handedOverAt;
        this.odometerOut = odometerOut;
        this.fuelLevelOut = fuelLevelOut;
        this.conditionOut = conditionOut;
        this.status = status;
        this.returnedAt = returnedAt;
        this.odometerIn = odometerIn;
        this.fuelLevelIn = fuelLevelIn;
        this.conditionIn = conditionIn;
        this.lateFee = lateFee;
    }

    public String getRecordId() { return recordId; }
    public String getBookingId() { return bookingId; }
    public String getCustomerName() { return customerName; }
    public String getVehicleId() { return vehicleId; }
    public LocalDate getScheduledReturnDate() { return scheduledReturnDate; }
    public LocalDateTime getHandedOverAt() { return handedOverAt; }
    public double getOdometerOut() { return odometerOut; }
    public String getFuelLevelOut() { return fuelLevelOut; }
    public String getConditionOut() { return conditionOut; }
    public String getStatus() { return status; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public Double getOdometerIn() { return odometerIn; }
    public String getFuelLevelIn() { return fuelLevelIn; }
    public String getConditionIn() { return conditionIn; }
    public double getLateFee() { return lateFee; }

    public void updateHandover(LocalDate scheduledReturnDate, double odometerOut,
                               String fuelLevelOut, String conditionOut) {
        this.scheduledReturnDate = scheduledReturnDate;
        this.odometerOut = odometerOut;
        this.fuelLevelOut = fuelLevelOut;
        this.conditionOut = conditionOut;
    }

    public void completeReturn(LocalDateTime returnedAt, double odometerIn,
                               String fuelLevelIn, String conditionIn, double lateFee) {
        this.returnedAt = returnedAt;
        this.odometerIn = odometerIn;
        this.fuelLevelIn = fuelLevelIn;
        this.conditionIn = conditionIn;
        this.lateFee = lateFee;
        this.status = RETURNED;
    }

    public boolean isActive() { return ACTIVE.equals(status); }
}
