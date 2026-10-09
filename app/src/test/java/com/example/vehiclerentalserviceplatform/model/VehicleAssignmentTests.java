package com.example.vehiclerentalserviceplatform.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleAssignmentTests {
    @Test
    void serializesManagedCategoryAndBranchWithoutBreakingCoreFields() {
        Car car = new Car("Toyota", "Corolla", "2024", 9000, "Petrol", 1200,
                true, 5, "car.png", "CAR-0099");
        car.setUsageCategory("BUSINESS");
        car.setCurrentBranch("BR-002");

        assertThat(car.toString()).endsWith(",BUSINESS,BR-002");
        assertThat(car.getUsageCategory()).isEqualTo("BUSINESS");
        assertThat(car.getCurrentBranch()).isEqualTo("BR-002");
    }
}
