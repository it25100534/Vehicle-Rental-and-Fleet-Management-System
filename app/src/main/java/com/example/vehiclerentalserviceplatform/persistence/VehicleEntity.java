package com.example.vehiclerentalserviceplatform.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "vehicles")
public class VehicleEntity {
    @Id @Column(name = "vehicle_id", length = 40) private String vehicleId;
    @Column(nullable = false, length = 80) private String make;
    @Column(nullable = false, length = 80) private String model;
    @Column(name = "manufacture_year", nullable = false) private int manufactureYear;
    @Column(name = "vehicle_type", nullable = false, length = 30) private String vehicleType;
    @Column(name = "fuel_type", nullable = false, length = 30) private String fuelType;
    @Column(name = "seat_count") private Integer seatCount;
    @Column(name = "vehicle_subtype", length = 40) private String vehicleSubtype;
    @Column(name = "category_code", nullable = false, length = 30) private String categoryCode;
    @Column(name = "branch_id", nullable = false, length = 30) private String branchId;
    @Column(name = "rental_rate", nullable = false, precision = 12, scale = 2) private BigDecimal rentalRate;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal mileage;
    @Column(nullable = false, length = 30) private String status;
    @Column(name = "image_path", length = 500) private String imagePath;
    @Column(nullable = false) private boolean retired;

    protected VehicleEntity() {}

    public VehicleEntity(String vehicleId, String make, String model, int manufactureYear, String vehicleType,
                         String fuelType, Integer seatCount, String vehicleSubtype, String categoryCode,
                         String branchId, BigDecimal rentalRate, BigDecimal mileage, String status, String imagePath) {
        this.vehicleId = vehicleId; this.make = make; this.model = model; this.manufactureYear = manufactureYear;
        this.vehicleType = vehicleType; this.fuelType = fuelType; this.seatCount = seatCount;
        this.vehicleSubtype = vehicleSubtype; this.categoryCode = categoryCode; this.branchId = branchId;
        this.rentalRate = rentalRate; this.mileage = mileage; this.status = status; this.imagePath = imagePath;
    }

    public String getVehicleId() { return vehicleId; }
    public String getMake() { return make; }
    public String getModel() { return model; }
    public int getManufactureYear() { return manufactureYear; }
    public String getVehicleType() { return vehicleType; }
    public String getFuelType() { return fuelType; }
    public Integer getSeatCount() { return seatCount; }
    public String getVehicleSubtype() { return vehicleSubtype; }
    public String getCategoryCode() { return categoryCode; }
    public String getBranchId() { return branchId; }
    public BigDecimal getRentalRate() { return rentalRate; }
    public BigDecimal getMileage() { return mileage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getImagePath() { return imagePath; }
    public boolean isRetired() { return retired; }
    public void setRetired(boolean retired) { this.retired = retired; }
}
