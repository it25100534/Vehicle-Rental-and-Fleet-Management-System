package com.example.vehicalrentalserviceplatform.model;

import java.util.UUID;

public class Booking {

    private String transactionId;
    private String customerName;
    private String vehicleId;
    private String startDate;
    private String returnDate;
    private String bookingStatus;

    //new booking
    public Booking(String customerName, String vehicleId, String startDate, String returnDate, String bookingStatus) {

        this.transactionId = UUID.randomUUID().toString();
        this.customerName = customerName;
        this.vehicleId = vehicleId;
        this.startDate = startDate;
        this.returnDate = returnDate;
        this.bookingStatus = bookingStatus;

    }

    //existing booking
    public Booking(String transactionId, String customerName, String vehicleId, String startDate, String returnDate, String bookingStatus) {

        this.transactionId = transactionId;
        this.customerName = customerName;
        this.vehicleId = vehicleId;
        this.startDate = startDate;
        this.returnDate = returnDate;
        this.bookingStatus = bookingStatus;
    }

    public String getTransactionId() {
        return transactionId;
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

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(String returnDate) {
        this.returnDate = returnDate;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    @Override
    public String toString() {
        return transactionId + "," + customerName + "," + vehicleId + "," + startDate + "," + returnDate + "," + bookingStatus;
    }
}