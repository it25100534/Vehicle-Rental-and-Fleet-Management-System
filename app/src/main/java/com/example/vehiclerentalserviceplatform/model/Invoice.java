package com.example.vehiclerentalserviceplatform.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Invoice {

    private Long id;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String transactionId;

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

    private Double damageFee;
    private Double fuelCharge;
    private Double mileageCharge;
    private Double alternateDropoffCharge;
    private Double depositAmount;
    private Double refundAmount;
    private String branchId;
    private LocalDateTime voidedAt;
    private String voidReason;
    private LocalDateTime refundedAt;
    private String refundReason;

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
        this.damageFee = 0.0;
        this.fuelCharge = 0.0;
        this.mileageCharge = 0.0;
        this.alternateDropoffCharge = 0.0;
        this.depositAmount = 0.0;
        this.refundAmount = 0.0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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

    public Double getDamageFee() { return damageFee; }
    public void setDamageFee(Double damageFee) { this.damageFee = damageFee; }
    public Double getFuelCharge() { return fuelCharge; }
    public void setFuelCharge(Double fuelCharge) { this.fuelCharge = fuelCharge; }
    public Double getMileageCharge() { return mileageCharge; }
    public void setMileageCharge(Double mileageCharge) { this.mileageCharge = mileageCharge; }
    public Double getAlternateDropoffCharge() { return alternateDropoffCharge; }
    public void setAlternateDropoffCharge(Double alternateDropoffCharge) { this.alternateDropoffCharge = alternateDropoffCharge; }
    public Double getDepositAmount() { return depositAmount; }
    public void setDepositAmount(Double depositAmount) { this.depositAmount = depositAmount; }
    public Double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(Double refundAmount) { this.refundAmount = refundAmount; }
    public String getBranchId() { return branchId; }
    public void setBranchId(String branchId) { this.branchId = branchId; }
    public LocalDateTime getVoidedAt() { return voidedAt; }
    public void setVoidedAt(LocalDateTime voidedAt) { this.voidedAt = voidedAt; }
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public void setRefundedAt(LocalDateTime refundedAt) { this.refundedAt = refundedAt; }
    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

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
        double total = getSubtotal() - safeValue(discount) + safeValue(lateFee)
                + safeValue(damageFee) + safeValue(fuelCharge) + safeValue(mileageCharge)
                + safeValue(alternateDropoffCharge) + safeValue(depositAmount) - safeValue(refundAmount);
        return Math.max(total, 0.0);
    }

    public void applyDiscount(double discountAmount) {
        this.discount = discountAmount;
    }

    public void applyLateFee(double lateFeeAmount) {
        this.lateFee = lateFeeAmount;
    }

    public double getAmountBeforeRefund() {
        return getTotalAmount() + safeValue(refundAmount);
    }

    public double getRecognizedRevenue() {
        return status == InvoiceStatus.VOID ? 0.0 : getTotalAmount();
    }

    public void applyReturnAdjustments(double lateFee, double damageFee, double fuelCharge,
                                       double mileageCharge, double alternateDropoffCharge,
                                       double depositAmount, double refundAmount) {
        this.lateFee = lateFee;
        this.damageFee = damageFee;
        this.fuelCharge = fuelCharge;
        this.mileageCharge = mileageCharge;
        this.alternateDropoffCharge = alternateDropoffCharge;
        this.depositAmount = depositAmount;
        this.refundAmount = refundAmount;
    }

    public void markPaid() {
        this.status = InvoiceStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    public void voidInvoice(String reason) {
        if (status == InvoiceStatus.VOID) {
            throw new IllegalStateException("Invoice is already voided.");
        }
        if (status == InvoiceStatus.REFUNDED) {
            throw new IllegalStateException("A fully refunded invoice cannot be voided.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A void reason is required.");
        }
        status = InvoiceStatus.VOID;
        voidedAt = LocalDateTime.now();
        voidReason = reason.trim();
    }

    public void recordRefund(double amount, String reason) {
        if (status == InvoiceStatus.VOID) {
            throw new IllegalStateException("A voided invoice cannot be refunded.");
        }
        double paidAmount = getAmountBeforeRefund();
        double newRefundTotal = safeValue(refundAmount) + amount;
        if (!Double.isFinite(amount) || amount <= 0 || newRefundTotal > paidAmount) {
            throw new IllegalArgumentException("Refund must be greater than zero and cannot exceed the paid amount.");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A refund reason is required.");
        }
        refundAmount = newRefundTotal;
        refundReason = reason.trim();
        refundedAt = LocalDateTime.now();
        status = newRefundTotal >= paidAmount ? InvoiceStatus.REFUNDED : InvoiceStatus.PARTIALLY_REFUNDED;
    }

    private double safeValue(Double value) {
        return value == null ? 0.0 : value;
    }
}
