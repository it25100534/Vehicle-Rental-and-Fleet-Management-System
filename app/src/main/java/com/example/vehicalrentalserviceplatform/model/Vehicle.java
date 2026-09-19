package com.example.vehicalrentalserviceplatform.model;

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