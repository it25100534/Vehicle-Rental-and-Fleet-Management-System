package com.example.vehiclerentalserviceplatform.rental;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
    private double damageFee;
    private double fuelCharge;
    private double mileageCharge;
    private double alternateDropoffCharge;
    private double depositAmount;
    private double refundAmount;

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

    public RentalHandover(String recordId, String bookingId, String customerName,
                          String vehicleId, LocalDate scheduledReturnDate,
                          LocalDateTime handedOverAt, double odometerOut,
                          String fuelLevelOut, String conditionOut, String status,
                          LocalDateTime returnedAt, Double odometerIn,
                          String fuelLevelIn, String conditionIn, double lateFee,
                          double damageFee, double fuelCharge, double mileageCharge,
                          double alternateDropoffCharge, double depositAmount, double refundAmount) {
        this(recordId, bookingId, customerName, vehicleId, scheduledReturnDate, handedOverAt,
                odometerOut, fuelLevelOut, conditionOut, status, returnedAt, odometerIn,
                fuelLevelIn, conditionIn, lateFee);
        this.damageFee = damageFee;
        this.fuelCharge = fuelCharge;
        this.mileageCharge = mileageCharge;
        this.alternateDropoffCharge = alternateDropoffCharge;
        this.depositAmount = depositAmount;
        this.refundAmount = refundAmount;
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
    public double getDamageFee() { return damageFee; }
    public double getFuelCharge() { return fuelCharge; }
    public double getMileageCharge() { return mileageCharge; }
    public double getAlternateDropoffCharge() { return alternateDropoffCharge; }
    public double getDepositAmount() { return depositAmount; }
    public double getRefundAmount() { return refundAmount; }

    public double getReturnAdjustmentTotal() {
        return lateFee + damageFee + fuelCharge + mileageCharge + alternateDropoffCharge
                + depositAmount - refundAmount;
    }

    public void updateHandover(LocalDate scheduledReturnDate, double odometerOut,
                               String fuelLevelOut, String conditionOut) {
        this.scheduledReturnDate = scheduledReturnDate;
        this.odometerOut = odometerOut;
        this.fuelLevelOut = fuelLevelOut;
        this.conditionOut = conditionOut;
    }

    public void completeReturn(LocalDateTime returnedAt, double odometerIn,
                               String fuelLevelIn, String conditionIn, double lateFee) {
        completeReturn(returnedAt, odometerIn, fuelLevelIn, conditionIn, lateFee,
                0, 0, 0, 0, 0, 0);
    }

    public void completeReturn(LocalDateTime returnedAt, double odometerIn,
                               String fuelLevelIn, String conditionIn, double lateFee,
                               double damageFee, double fuelCharge, double mileageCharge,
                               double alternateDropoffCharge, double depositAmount, double refundAmount) {
        this.returnedAt = returnedAt;
        this.odometerIn = odometerIn;
        this.fuelLevelIn = fuelLevelIn;
        this.conditionIn = conditionIn;
        this.lateFee = lateFee;
        this.damageFee = damageFee;
        this.fuelCharge = fuelCharge;
        this.mileageCharge = mileageCharge;
        this.alternateDropoffCharge = alternateDropoffCharge;
        this.depositAmount = depositAmount;
        this.refundAmount = refundAmount;
        this.status = RETURNED;
    }

    public boolean isActive() { return ACTIVE.equals(status); }

    public boolean isOverdue() {
        return isActive() && scheduledReturnDate.isBefore(LocalDate.now());
    }

    public long getDaysOverdue() {
        return isOverdue() ? ChronoUnit.DAYS.between(scheduledReturnDate, LocalDate.now()) : 0;
    }

    public String getRentalState() {
        if (!isActive()) return RETURNED;
        return isOverdue() ? "OVERDUE" : ACTIVE;
    }
}
