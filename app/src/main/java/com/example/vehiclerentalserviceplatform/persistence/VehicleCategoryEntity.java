package com.example.vehiclerentalserviceplatform.persistence;

import com.example.vehiclerentalserviceplatform.fleet.VehicleCategory;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicle_categories")
public class VehicleCategoryEntity {
    @Id private String code;
    private String displayName;
    private boolean active;

    protected VehicleCategoryEntity() {}
    public VehicleCategoryEntity(String code, String displayName, boolean active) {
        this.code=code; this.displayName=displayName; this.active=active;
    }
    public VehicleCategory toDomain() { return new VehicleCategory(code, displayName, active); }
    public void update(String name, boolean enabled) { displayName=name; active=enabled; }
    public void setActive(boolean active) { this.active=active; }
    public String getCode() { return code; }
}
