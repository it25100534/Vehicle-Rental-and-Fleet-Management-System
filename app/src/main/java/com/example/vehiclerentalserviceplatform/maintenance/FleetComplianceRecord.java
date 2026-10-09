package com.example.vehiclerentalserviceplatform.maintenance;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class FleetComplianceRecord {
    private final String vehicleId;
    private final String insuranceExpiry;
    private final String licenceExpiry;
    private final String emissionExpiry;

    public FleetComplianceRecord(String vehicleId, String insuranceExpiry,
                                 String licenceExpiry, String emissionExpiry) {
        this.vehicleId = vehicleId;
        this.insuranceExpiry = insuranceExpiry;
        this.licenceExpiry = licenceExpiry;
        this.emissionExpiry = emissionExpiry;
    }

    public String getVehicleId() { return vehicleId; }
    public String getInsuranceExpiry() { return insuranceExpiry; }
    public String getLicenceExpiry() { return licenceExpiry; }
    public String getEmissionExpiry() { return emissionExpiry; }

    public long getNearestDaysRemaining() {
        LocalDate today = LocalDate.now();
        return Math.min(days(today, insuranceExpiry), Math.min(days(today, licenceExpiry), days(today, emissionExpiry)));
    }

    public String getState() {
        long days = getNearestDaysRemaining();
        if (days < 0) return "EXPIRED";
        if (days <= 30) return "DUE SOON";
        return "VALID";
    }

    private long days(LocalDate today, String value) {
        return ChronoUnit.DAYS.between(today, LocalDate.parse(value));
    }
}
