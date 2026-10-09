package com.example.vehiclerentalserviceplatform.maintenance;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class MaintenanceRecord {
    private String recordId;
    private String vehicleId;
    private String vehicleType;
    private String serviceType;
    private String serviceDate;
    private String expectedCompletionDate;
    private String completedDate;
    private String provider;
    private double estimatedCost;
    private double actualCost;
    private String status;
    private String notes;

    public MaintenanceRecord(String recordId, String vehicleId, String vehicleType,
                             String serviceType, String serviceDate, String status, String notes) {
        this(recordId, vehicleId, vehicleType, serviceType, serviceDate, serviceDate, "",
                "Not assigned", 0, 0, status, notes);
    }

    public MaintenanceRecord(String recordId, String vehicleId, String vehicleType,
                             String serviceType, String serviceDate, String expectedCompletionDate,
                             String completedDate, String provider, double estimatedCost,
                             double actualCost, String status, String notes) {
        this.recordId = recordId;
        this.vehicleId = vehicleId;
        this.vehicleType = vehicleType;
        this.serviceType = serviceType;
        this.serviceDate = serviceDate;
        this.expectedCompletionDate = expectedCompletionDate;
        this.completedDate = completedDate == null ? "" : completedDate;
        this.provider = provider;
        this.estimatedCost = estimatedCost;
        this.actualCost = actualCost;
        this.status = status;
        this.notes = notes;
    }

    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getServiceDate() { return serviceDate; }
    public void setServiceDate(String serviceDate) { this.serviceDate = serviceDate; }
    public String getExpectedCompletionDate() { return expectedCompletionDate; }
    public void setExpectedCompletionDate(String expectedCompletionDate) { this.expectedCompletionDate = expectedCompletionDate; }
    public String getCompletedDate() { return completedDate; }
    public void setCompletedDate(String completedDate) { this.completedDate = completedDate == null ? "" : completedDate; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public double getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(double estimatedCost) { this.estimatedCost = estimatedCost; }
    public double getActualCost() { return actualCost; }
    public void setActualCost(double actualCost) { this.actualCost = actualCost; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isActive() {
        return "Pending".equalsIgnoreCase(status) || "In Progress".equalsIgnoreCase(status);
    }

    public boolean isDue() {
        return isActive() && !LocalDate.parse(serviceDate).isAfter(LocalDate.now());
    }

    public boolean isDueSoon() {
        if (!isActive()) return false;
        LocalDate date = LocalDate.parse(serviceDate);
        return !date.isBefore(LocalDate.now()) && !date.isAfter(LocalDate.now().plusDays(14));
    }

    public long getDowntimeDays() {
        if ("Cancelled".equalsIgnoreCase(status)) return 0;
        LocalDate start = LocalDate.parse(serviceDate);
        LocalDate end = completedDate == null || completedDate.isBlank()
                ? (isActive() ? LocalDate.now() : LocalDate.parse(expectedCompletionDate))
                : LocalDate.parse(completedDate);
        return Math.max(0, ChronoUnit.DAYS.between(start, end) + 1);
    }

    public String getMaintenanceAlert() {
        String prefix = switch (vehicleType.toUpperCase()) {
            case "CAR" -> "Car";
            case "MOTORCYCLE", "BIKE" -> "Bike";
            case "VAN" -> "Van";
            case "SUV" -> "SUV";
            default -> "Vehicle";
        };
        return prefix + " maintenance: " + serviceType + " for " + vehicleId + " is "
                + status.toLowerCase() + ", scheduled " + serviceDate + " with " + provider + ".";
    }
}
