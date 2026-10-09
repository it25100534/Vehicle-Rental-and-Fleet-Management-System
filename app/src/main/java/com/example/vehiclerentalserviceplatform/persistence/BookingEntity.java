package com.example.vehiclerentalserviceplatform.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "bookings")
public class BookingEntity {
    @Id
    @Column(name = "transaction_id", length = 50)
    private String transactionId;

    @Column(name = "customer_username", nullable = false, length = 80)
    private String customerUsername;

    @Column(name = "vehicle_id", nullable = false, length = 40)
    private String vehicleId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "return_date", nullable = false)
    private LocalDate returnDate;

    @Column(nullable = false, length = 30)
    private String status;

    protected BookingEntity() {}

    public BookingEntity(String transactionId, String customerUsername, String vehicleId,
                         LocalDate startDate, LocalDate returnDate, String status) {
        this.transactionId = transactionId;
        this.customerUsername = customerUsername;
        this.vehicleId = vehicleId;
        this.startDate = startDate;
        this.returnDate = returnDate;
        this.status = status;
    }

    public String getTransactionId() { return transactionId; }
    public String getCustomerUsername() { return customerUsername; }
    public String getVehicleId() { return vehicleId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public String getStatus() { return status; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public void setStatus(String status) { this.status = status; }
}
