package com.example.vehicalrentalserviceplatform.model;

public class Van extends Vehicle{
    private int numberOfSeats;
    private String driveTrain; //whether van is awd 2wd or 4wd

    public Van(String make, String model, String year, double rentalRate,
               String fuelType, double mileage, boolean isAvailable,
               int numberOfSeats, String driveTrain, String vehicleImageFileName, String vehicleId) {
        super(make, model, year, rentalRate, fuelType, mileage, isAvailable, vehicleImageFileName, vehicleId);
        this.numberOfSeats = numberOfSeats;
        setDriveTrain(driveTrain);
    }

    public int getNumberOfSeats() {
        return numberOfSeats;
    }

    public String getDriveTrain() {
        return driveTrain;
    }


    public void setNumberOfSeats(int numberOfSeats) {
        this.numberOfSeats = numberOfSeats;
    }

    public void setDriveTrain(String driveTrain) {
        if (driveTrain.equalsIgnoreCase("4WD")) {
            this.driveTrain = "4WD";
        } else if (driveTrain.equalsIgnoreCase("AWD")) {
            this.driveTrain = "AWD";
        } else {
            this.driveTrain = "2WD"; // Defaults to 2WD for any case combination of Standard or any invalid input to prevent errors
        }
    }
    @Override
    public String toString() {
        return "VAN," + super.toString() + "," + numberOfSeats + "," + driveTrain;
    }
}
