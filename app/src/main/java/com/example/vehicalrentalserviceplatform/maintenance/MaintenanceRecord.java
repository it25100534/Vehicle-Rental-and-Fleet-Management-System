package com.example.vehicalrentalserviceplatform.maintenance;

// MODEL CLASS: Represents one maintenance/service record in the system.
public class MaintenanceRecord {

    // OOP: Encapsulation - record data is kept private and accessed through getters/setters.
    private String recordId;
    private String vehicleId;
    private String vehicleType;
    private String serviceType;
    private String serviceDate;
    private String status;
    private String notes;

    // CONSTRUCTOR: Initializes a complete maintenance record object when loading or creating records.
    public MaintenanceRecord(String recordId, String vehicleId, String vehicleType,
                             String serviceType, String serviceDate, String status, String notes) {
        this.recordId = recordId;
        this.vehicleId = vehicleId;
        this.vehicleType = vehicleType;
        this.serviceType = serviceType;
        this.serviceDate = serviceDate;
        this.status = status;
        this.notes = notes;
    }

    // ACCESSORS: Getters return private values; setters allow controlled updates when needed.

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // FILE FORMAT: Converts this object into one comma-separated line for saving in maintenance.txt.
    @Override
    public String toString() {
        return recordId + "," + vehicleId + "," + vehicleType + "," + serviceType + ","
                + serviceDate + "," + status + "," + notes;
    }

    // OOP/BEHAVIOR: Generates a different maintenance alert based on the stored vehicle type.
    public String getMaintenanceAlert() {
        if (vehicleType.equalsIgnoreCase("Car")) {
            return "Car maintenance alert: Check engine oil, brake system, tyres, battery, and coolant level for Vehicle " + vehicleId + ".";
        } else if (vehicleType.equalsIgnoreCase("Bike")) {
            return "Bike maintenance alert: Check chain, brake pads, tyres, engine oil, and lights for Vehicle " + vehicleId + ".";
        } else if (vehicleType.equalsIgnoreCase("Van")) {
            return "Van maintenance alert: Check engine condition, tyre pressure, brake system, battery, and load safety for Vehicle " + vehicleId + ".";
        } else {
            return "General maintenance alert: Full inspection required for Vehicle " + vehicleId + ".";
        }
    }
}