package com.example.vehiclerentalserviceplatform.persistence;

import com.example.vehiclerentalserviceplatform.fleet.BranchService;
import com.example.vehiclerentalserviceplatform.fleet.VehicleCategoryService;
import com.example.vehiclerentalserviceplatform.customer.service.CustomerFileService;
import com.example.vehiclerentalserviceplatform.security.PasswordHash;
import com.example.vehiclerentalserviceplatform.service.VehicleService;
import com.example.vehiclerentalserviceplatform.service.BookingService;
import com.example.vehiclerentalserviceplatform.model.Booking;
import com.example.vehiclerentalserviceplatform.model.Car;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DatabaseFoundationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired BranchService branches;
    @Autowired VehicleCategoryService categories;
    @Autowired CustomerFileService customers;
    @Autowired CustomerRepository customerRepository;
    @Autowired StaffAccountRepository staffRepository;
    @Autowired VehicleService vehicles;
    @Autowired BookingService bookings;
    @Autowired BookingRepository bookingRepository;

    @Test
    void createsTheFrozenSchema() {
        Integer count = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public'", Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(11);
    }

    @Test
    void importsAndPersistsFleetConfiguration() {
        assertThat(branches.find("BR-001")).isNotNull();
        branches.save("BR-099", "Database Test", "99 Test Road Colombo", "0112223344");
        assertThat(jdbc.queryForObject("select count(*) from branches where branch_id='BR-099'", Integer.class)).isOne();
        assertThat(categories.isActive("DAILY")).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from vehicle_categories", Integer.class)).isGreaterThanOrEqualTo(6);
        branches.delete("BR-099");
    }

    @Test
    void importsCustomerAndStaffAccountsIntoSql() {
        assertThat(customerRepository.count()).isGreaterThanOrEqualTo(2);
        assertThat(staffRepository.findAllByActiveTrueOrderByFullNameAsc()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(customers.loginCustomer("Aros", "1234")).isTrue();
        String storedPassword = customerRepository.findByUsernameIgnoreCase("Aros").orElseThrow().getPasswordHash();
        assertThat(PasswordHash.isEncoded(storedPassword)).isTrue();
    }

    @Test
    void importsAndPersistsVehicleInventory() {
        jdbc.update("delete from vehicles where vehicle_id='CAR-9999'");
        assertThat(vehicles.getAllVehicles()).hasSize(150);
        assertThat(vehicles.getVehicleById("CAR-0001").getType()).isEqualTo("CAR");
        assertThat(vehicles.getVehicleById("SUV-0001").getUsageCategory()).isEqualTo("FAMILY");
        Car temporary = new Car("Test", "Vehicle", "2025", 5000, "Petrol", 20,
                true, 5, "test.png", "CAR-9999");
        temporary.setUsageCategory("DAILY");
        temporary.setCurrentBranch("BR-001");
        vehicles.addVehicle(temporary);
        assertThat(jdbc.queryForObject("select count(*) from vehicles where vehicle_id='CAR-9999'", Integer.class)).isOne();
        vehicles.deleteVehicle("CAR-9999");
        assertThat(jdbc.queryForObject("select retired from vehicles where vehicle_id='CAR-9999'", Boolean.class)).isTrue();
    }

    @Test
    void importsAndPersistsBookingsWithoutUsingTheTextFile() {
        assertThat(bookingRepository.count()).isGreaterThanOrEqualTo(6);
        Booking temporary = new Booking("test11", "CAR-0002", "2099-01-10", "2099-01-12", "Pending");
        assertThat(bookings.createBooking(temporary)).isTrue();
        assertThat(bookings.isVehicleAvailable("CAR-0002", "2099-01-11", "2099-01-12")).isFalse();
        bookings.deleteBooking(temporary.getTransactionId());
        assertThat(bookings.getBookingById(temporary.getTransactionId())).isNull();
        assertThat(bookings.isVehicleAvailable("CAR-0002", "2099-01-11", "2099-01-12")).isTrue();
    }
}
