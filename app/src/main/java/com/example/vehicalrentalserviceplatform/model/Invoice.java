package com.example.vehicalrentalserviceplatform.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public class Invoice {

    private Long id;

    @NotBlank(message = "Customer name is required.")
    private String customerName;

    @NotBlank(message = "Vehicle ID is required.")
    private String vehicleId;

    private String vehicleName;

    @NotNull(message = "Rental days are required.")
    @Positive(message = "Rental days must be greater than 0.")
    private Integer rentalDays;

    @NotNull(message = "Amount per day is required.")
    @Positive(message = "Amount per day must be greater than 0.")
    private Double amountPerDay;

    @NotNull(message = "Discount is required.")
    @DecimalMin(value = "0.0", message = "Discount cannot be negative.")
    private Double discount;

    @NotNull(message = "Late fee is required.")
    @DecimalMin(value = "0.0", message = "Late fee cannot be negative.")
    private Double lateFee;

    @NotNull(message = "Invoice status is required.")
    private InvoiceStatus status;

    @NotNull(message = "Payment method is required.")
    private PaymentMethod paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    public Invoice() {
        this.status = InvoiceStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.discount = 0.0;
        this.lateFee = 0.0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public Integer getRentalDays() {
        return rentalDays;
    }

    public void setRentalDays(Integer rentalDays) {
        this.rentalDays = rentalDays;
    }

    public Double getAmountPerDay() {
        return amountPerDay;
    }

    public void setAmountPerDay(Double amountPerDay) {
        this.amountPerDay = amountPerDay;
    }

    public Double getDiscount() {
        return discount;
    }

    public void setDiscount(Double discount) {
        this.discount = discount;
    }

    public Double getLateFee() {
        return lateFee;
    }

    public void setLateFee(Double lateFee) {
        this.lateFee = lateFee;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public double getSubtotal() {
        if (rentalDays == null || amountPerDay == null) {
            return 0.0;
        }
        return rentalDays * amountPerDay;
    }

    public double getTotalAmount() {
        double total = getSubtotal() - safeValue(discount) + safeValue(lateFee);
        return Math.max(total, 0.0);
    }

    public void applyDiscount(double discountAmount) {
        this.discount = discountAmount;
    }

    public void applyLateFee(double lateFeeAmount) {
        this.lateFee = lateFeeAmount;
    }

    public void markPaid() {
        this.status = InvoiceStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    private double safeValue(Double value) {
        return value == null ? 0.0 : value;
    }
}
