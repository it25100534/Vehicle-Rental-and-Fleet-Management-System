package com.example.vehicalrentalserviceplatform.model;

public class Car extends Vehicle{
    private int numberOfSeats;
    private String carType; //whether a hatchback or a sedan

    public Car(String make, String model, String year, double rentalRate,
               String fuelType, double mileage, boolean isAvailable,
               int numberOfSeats, String vehicleImageFileName, String vehicleId) {
        super(make, model, year, rentalRate, fuelType, mileage, isAvailable, vehicleImageFileName, vehicleId);
        this.numberOfSeats = numberOfSeats;
    }


    public String getCarType() {
        return carType;
    }

    public int getNumberOfSeats() {
        return numberOfSeats;
    }

    public void setNumberOfSeats(int numberOfSeats) {
        this.numberOfSeats = numberOfSeats;
    }

    public void setCarType(String carType) {
        if (carType.equalsIgnoreCase("Hatchback")) {
            this.carType = "Hatchback";
        } else {
            this.carType = "Sedan"; // Defaults to Sedan for any case combination of Standard or any invalid input to prevent errors
        }
    }
    @Override
    public String toString() {
        return "CAR," + super.toString() + "," + numberOfSeats + "," + carType;
    }
}
