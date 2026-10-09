package com.example.vehiclerentalserviceplatform.rental;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RentalHandoverServiceTests {
    @Autowired RentalHandoverService service;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void prepareSqlRecords() {
        jdbc.update("delete from rentals where booking_id like 'BOOK-TEST-%'");
        if (jdbc.queryForObject("select count(*) from customers where username='RENTAL-TEST'", Integer.class) == 0)
            jdbc.update("insert into customers(username,license_id,email,phone,password_hash,active) values('RENTAL-TEST','LIC-RENTAL-TEST','rental-test@example.com','0770000000','hash',true)");
        if (jdbc.queryForObject("select count(*) from vehicles where vehicle_id='CAR-TEST-1'", Integer.class) == 0)
            jdbc.update("insert into vehicles(vehicle_id,make,model,manufacture_year,vehicle_type,fuel_type,seat_count,vehicle_subtype,category_code,branch_id,rental_rate,mileage,status,image_path) values('CAR-TEST-1','Test','One',2025,'CAR','Petrol',5,null,'DAILY','BR-001',1000,0,'AVAILABLE','')");
        if (jdbc.queryForObject("select count(*) from vehicles where vehicle_id='CAR-TEST-9'", Integer.class) == 0)
            jdbc.update("insert into vehicles(vehicle_id,make,model,manufacture_year,vehicle_type,fuel_type,seat_count,vehicle_subtype,category_code,branch_id,rental_rate,mileage,status,image_path) values('CAR-TEST-9','Test','Nine',2025,'CAR','Petrol',5,null,'DAILY','BR-001',1000,0,'AVAILABLE','')");
        for (int n=1;n<=4;n++) {
            String id="BOOK-TEST-"+n;
            if(jdbc.queryForObject("select count(*) from bookings where transaction_id=?",Integer.class,id)==0)
                jdbc.update("insert into bookings(transaction_id,customer_username,vehicle_id,start_date,return_date,status) values(?,?,?,CURRENT_DATE,DATEADD('DAY',10,CURRENT_DATE),'Approved')",id,"RENTAL-TEST",n==4?"CAR-TEST-9":"CAR-TEST-1");
        }
    }

    @Test
    void completesTheFullHandoverCrudFlow() {
        RentalHandover created = service.create("BOOK-TEST-1", "RENTAL-TEST", "CAR-TEST-1",
                LocalDate.now().plusDays(3), 12500, "FULL", "No visible damage", "tester");
        assertTrue(service.findById(created.getRecordId()).isPresent());
        assertTrue(created.isActive());

        RentalHandover updated = service.update(created.getRecordId(), LocalDate.now().plusDays(4),
                12510, "THREE_QUARTERS", "Small mark on rear bumper", "tester");
        assertEquals(12510, updated.getOdometerOut());

        RentalHandover returned = service.completeReturn(created.getRecordId(), 12725,
                "HALF", "Returned with existing mark", 500, "tester");
        assertFalse(returned.isActive());
        assertEquals(RentalHandover.RETURNED, returned.getStatus());
        assertEquals(12725, returned.getOdometerIn());

        service.delete(created.getRecordId(), "tester");
        assertTrue(service.findById(created.getRecordId()).isEmpty());
    }

    @Test
    void rejectsDuplicateActiveHandoverAndInvalidReturnMileage() {
        RentalHandover created = service.create("BOOK-TEST-2", "RENTAL-TEST", "CAR-TEST-1",
                LocalDate.now().plusDays(3), 5000, "FULL", "Good", "tester");

        assertThrows(IllegalArgumentException.class, () -> service.create("BOOK-TEST-3", "RENTAL-TEST", "CAR-TEST-1",
                LocalDate.now().plusDays(2), 100, "FULL", "Good", "tester"));
        assertThrows(IllegalArgumentException.class, () -> service.completeReturn(created.getRecordId(), 4999,
                "FULL", "Good", 0, "tester"));
    }

    @Test
    void identifiesActiveOverdueAndReturnedRentalStates() {
        RentalHandover overdue = new RentalHandover(
                "RENT-OVERDUE", "BOOK-OVERDUE", "Test Customer", "CAR-1",
                LocalDate.now().minusDays(2), LocalDateTime.now().minusDays(5),
                5000, "FULL", "Good", RentalHandover.ACTIVE,
                null, null, "", "", 0);

        assertTrue(overdue.isOverdue());
        assertEquals(2, overdue.getDaysOverdue());
        assertEquals("OVERDUE", overdue.getRentalState());

        overdue.completeReturn(LocalDateTime.now(), 5100, "HALF", "Good", 1500);

        assertFalse(overdue.isOverdue());
        assertEquals(0, overdue.getDaysOverdue());
        assertEquals(RentalHandover.RETURNED, overdue.getRentalState());
    }

    @Test
    void persistsSeparateReturnChargesAndRefunds() {
        RentalHandover created = service.create("BOOK-TEST-4", "RENTAL-TEST", "CAR-TEST-9",
                LocalDate.now().plusDays(1), 1000, "FULL", "Good", "tester");

        service.completeReturn(created.getRecordId(), 1100, "HALF", "Scratch", 500,
                2000, 750, 300, 400, 5000, 1500, "tester");

        RentalHandover saved = service.findById(created.getRecordId()).orElseThrow();
        assertEquals(500, saved.getLateFee());
        assertEquals(2000, saved.getDamageFee());
        assertEquals(750, saved.getFuelCharge());
        assertEquals(300, saved.getMileageCharge());
        assertEquals(400, saved.getAlternateDropoffCharge());
        assertEquals(5000, saved.getDepositAmount());
        assertEquals(1500, saved.getRefundAmount());
        assertEquals(7450, saved.getReturnAdjustmentTotal());
    }

}
