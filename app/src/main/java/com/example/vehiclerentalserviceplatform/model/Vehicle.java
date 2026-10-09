package com.example.vehiclerentalserviceplatform.model;

import java.util.UUID;

public class Vehicle {
    private String vehicleId;
    private String make;
    private String model;
    private String year;
    private String vehicleImageFileName;
    private String fuelType;
    private double rentalRate;
    private double mileage;
    private boolean isAvailable;
    private String usageCategory = "DAILY";
    private String currentBranch = "BR-001";
    private String operationalStatus = "AVAILABLE";

    //New Vehicle constructor

    public Vehicle(String make, String model, String year, double rentalRate,
                   String fuelType, double mileage, boolean isAvailable,
                   String vehicleImageFileName, String vehicleId) {
        this.vehicleId = vehicleId;
        this.make = make;
        this.model = model;
        this.year = year;
        this.vehicleImageFileName = vehicleImageFileName;
        this.rentalRate = rentalRate;
        this.mileage=mileage;
        this.isAvailable=isAvailable;
        setFuelType(fuelType);
    }

    //Getters

    public String getVehicleId() {
        return vehicleId;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public String getVehicleImageFileName() {
        return vehicleImageFileName;
    }

    public String getYear() {
        return year;
    }

    public double getRentalRate() {
        return rentalRate;
    }

    public double getMileage() {
        return mileage;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public String getFuelType() {
        return fuelType;
    }

    public String getUsageCategory() { return usageCategory; }

    public String getCurrentBranch() { return currentBranch; }

    public String getOperationalStatus() { return operationalStatus; }

    //Setters

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setVehicleImageFileName(String vehicleImageFileName) {
        this.vehicleImageFileName = vehicleImageFileName;
    }

    public void setRentalRate(double rentalRate) {
        this.rentalRate = rentalRate;
    }

    public void setMileage(double mileage) {
        this.mileage = mileage;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    public void setFuelType(String fuelType) {
        if (fuelType.equalsIgnoreCase("Diesel")) {
            this.fuelType = "Diesel";
        } else {
            this.fuelType = "Petrol"; // Defaults to Petrol for any case combination of petrol or any invalid input to prevent errors
        }
    }

    public void setUsageCategory(String usageCategory) {
        this.usageCategory = usageCategory == null || usageCategory.isBlank()
                ? defaultUsageCategory() : usageCategory.trim().toUpperCase();
    }

    public void setCurrentBranch(String currentBranch) {
        this.currentBranch = currentBranch == null || currentBranch.isBlank()
                ? "BR-001" : currentBranch.trim().toUpperCase();
    }

    public void setOperationalStatus(String operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    public String defaultUsageCategory() {
        return switch (getType()) {
            case "SUV" -> "FAMILY";
            case "VAN" -> "TRANSPORT";
            default -> "DAILY";
        };
    }

    public String getType() {
        return this.getClass().getSimpleName().toUpperCase();
    }

    @Override
    public String toString() {
        return vehicleId + "," + year + "," + make + "," + model + "," +
                fuelType + "," + mileage + "," + vehicleImageFileName + "," +
                rentalRate + "," + isAvailable;
    }
}
