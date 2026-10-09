package com.example.vehiclerentalserviceplatform.fleet;

public class VehicleCategory {
    private final String code;
    private String name;
    private boolean active;

    public VehicleCategory(String code, String name, boolean active) {
        this.code = code;
        this.name = name;
        this.active = active;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public void setName(String name) { this.name = name; }
    public void setActive(boolean active) { this.active = active; }
    @Override public String toString() { return code + "," + name + "," + active; }
}
