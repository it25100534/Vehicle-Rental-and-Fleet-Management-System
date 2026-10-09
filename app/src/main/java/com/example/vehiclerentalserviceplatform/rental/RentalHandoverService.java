package com.example.vehiclerentalserviceplatform.rental;

import com.example.vehiclerentalserviceplatform.security.InputValidation;
import com.example.vehiclerentalserviceplatform.service.ActivityLogService;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RentalHandoverService {
    private static final Path LEGACY_FILE=Path.of("rentals.txt").toAbsolutePath();
    private final JdbcTemplate jdbc;
    private final ActivityLogService activity;
    public RentalHandoverService(JdbcTemplate jdbc, ActivityLogService activity){this.jdbc=jdbc;this.activity=activity;}

    @PostConstruct void importLegacyWhenEmpty(){
        Long count=jdbc.queryForObject("select count(*) from rentals",Long.class);
        if(count==null||count!=0||Files.notExists(LEGACY_FILE))return;
        try{for(String line:Files.readAllLines(LEGACY_FILE, StandardCharsets.UTF_8))if(!line.isBlank())insert(parse(line));}
        catch(Exception ex){throw new IllegalStateException("Could not import rental records.",ex);}
    }

    public List<RentalHandover> getAll(){
        return jdbc.query("select * from rentals order by handed_over_at desc",(rs,n)->new RentalHandover(
                rs.getString("record_id"),rs.getString("booking_id"),rs.getString("customer_username"),
                rs.getString("vehicle_id"),rs.getDate("scheduled_return_date").toLocalDate(),
                rs.getTimestamp("handed_over_at").toLocalDateTime(),rs.getDouble("odometer_out"),
                rs.getString("fuel_level_out"),rs.getString("condition_out"),rs.getString("status"),
                rs.getTimestamp("returned_at")==null?null:rs.getTimestamp("returned_at").toLocalDateTime(),
                rs.getObject("odometer_in")==null?null:rs.getDouble("odometer_in"),rs.getString("fuel_level_in"),
                rs.getString("condition_in"),rs.getDouble("late_fee"),rs.getDouble("damage_fee"),
                rs.getDouble("fuel_charge"),rs.getDouble("mileage_charge"),rs.getDouble("alternate_dropoff_charge"),
                rs.getDouble("deposit_amount"),rs.getDouble("refund_amount")));
    }
    public Optional<RentalHandover> findById(String id){return getAll().stream().filter(r->r.getRecordId().equals(id)).findFirst();}

    public RentalHandover create(String bookingId,String customer,String vehicleId,LocalDate returnDate,
                                 double odometer,String fuel,String condition,String actor){
        requireText(bookingId,"Booking is required.");requireText(customer,"Customer name is required.");requireText(vehicleId,"Vehicle is required.");
        InputValidation.requireHandover(returnDate,odometer,fuel,condition);
        if(returnDate.isBefore(LocalDate.now()))throw new IllegalArgumentException("Scheduled return date cannot be in the past.");
        boolean duplicate=getAll().stream().anyMatch(r->r.isActive()&&(r.getBookingId().equals(bookingId)||r.getVehicleId().equals(vehicleId)));
        if(duplicate)throw new IllegalArgumentException("This booking or vehicle already has an active handover.");
        RentalHandover r=new RentalHandover("RENT-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(),clean(bookingId),clean(customer),clean(vehicleId),returnDate,LocalDateTime.now(),odometer,clean(fuel),clean(condition),RentalHandover.ACTIVE,null,null,"","",0,0,0,0,0,0,0);
        insert(r); activity.log("RENTAL",actor,"CREATE",r.getRecordId(),"Handed over vehicle "+r.getVehicleId());return r;
    }

    public RentalHandover update(String id,LocalDate returnDate,double odometer,String fuel,String condition,String actor){
        InputValidation.requireHandover(returnDate,odometer,fuel,condition);RentalHandover r=require(id);
        if(!r.isActive())throw new IllegalArgumentException("Only active handovers can be updated.");
        if(returnDate.isBefore(r.getHandedOverAt().toLocalDate()))throw new IllegalArgumentException("Scheduled return date cannot be before the handover date.");
        jdbc.update("update rentals set scheduled_return_date=?,odometer_out=?,fuel_level_out=?,condition_out=? where record_id=?",returnDate,odometer,clean(fuel),clean(condition),id);
        activity.log("RENTAL",actor,"UPDATE",id,"Updated handover");return require(id);
    }

    public RentalHandover completeReturn(String id,double odo,String fuel,String condition,double late,String actor){
        return completeReturn(id,odo,fuel,condition,late,0,0,0,0,0,0,actor);
    }
    public RentalHandover completeReturn(String id,double odo,String fuel,String condition,double late,double damage,double fuelCharge,double mileage,double dropoff,double deposit,double refund,String actor){
        InputValidation.requireReturn(odo,fuel,condition,late);validate(damage,"Damage fee cannot be negative.");validate(fuelCharge,"Fuel charge cannot be negative.");validate(mileage,"Mileage charge cannot be negative.");validate(dropoff,"Alternate drop-off charge cannot be negative.");validate(deposit,"Deposit amount cannot be negative.");validate(refund,"Refund amount cannot be negative.");
        RentalHandover r=require(id);if(!r.isActive())throw new IllegalArgumentException("This rental has already been returned.");if(odo<r.getOdometerOut())throw new IllegalArgumentException("Return odometer cannot be lower than handover odometer.");
        jdbc.update("update rentals set status='RETURNED',returned_at=?,odometer_in=?,fuel_level_in=?,condition_in=?,late_fee=?,damage_fee=?,fuel_charge=?,mileage_charge=?,alternate_dropoff_charge=?,deposit_amount=?,refund_amount=? where record_id=?",Timestamp.valueOf(LocalDateTime.now()),odo,clean(fuel),clean(condition),late,damage,fuelCharge,mileage,dropoff,deposit,refund,id);
        activity.log("RENTAL",actor,"RETURN",id,"Returned vehicle "+r.getVehicleId());return require(id);
    }
    public RentalHandover delete(String id,String actor){RentalHandover r=require(id);jdbc.update("delete from rentals where record_id=?",id);activity.log("RENTAL",actor,"DELETE",id,"Deleted rental record");return r;}
    private RentalHandover require(String id){return findById(id).orElseThrow(()->new IllegalArgumentException("Rental record was not found."));}
    private void insert(RentalHandover r){jdbc.update("insert into rentals(record_id,booking_id,customer_username,vehicle_id,scheduled_return_date,handed_over_at,odometer_out,fuel_level_out,condition_out,status,returned_at,odometer_in,fuel_level_in,condition_in,late_fee,damage_fee,fuel_charge,mileage_charge,alternate_dropoff_charge,deposit_amount,refund_amount) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",r.getRecordId(),r.getBookingId(),r.getCustomerName(),r.getVehicleId(),Date.valueOf(r.getScheduledReturnDate()),Timestamp.valueOf(r.getHandedOverAt()),r.getOdometerOut(),r.getFuelLevelOut(),r.getConditionOut(),r.getStatus(),r.getReturnedAt()==null?null:Timestamp.valueOf(r.getReturnedAt()),r.getOdometerIn(),r.getFuelLevelIn(),r.getConditionIn(),r.getLateFee(),r.getDamageFee(),r.getFuelCharge(),r.getMileageCharge(),r.getAlternateDropoffCharge(),r.getDepositAmount(),r.getRefundAmount());}
    private RentalHandover parse(String line){String[]p=line.split("\\|",-1);if(p.length!=15&&p.length!=21)throw new IllegalStateException("Invalid rental record: "+line);return new RentalHandover(p[0],p[1],p[2],p[3],LocalDate.parse(p[4]),LocalDateTime.parse(p[5]),Double.parseDouble(p[6]),p[7],p[8],p[9],p[10].isBlank()?null:LocalDateTime.parse(p[10]),p[11].isBlank()?null:Double.parseDouble(p[11]),p[12],p[13],Double.parseDouble(p[14]),number(p,15),number(p,16),number(p,17),number(p,18),number(p,19),number(p,20));}
    private double number(String[]p,int i){return i<p.length&&!p[i].isBlank()?Double.parseDouble(p[i]):0;}
    private void requireText(String v,String m){if(v==null||v.isBlank())throw new IllegalArgumentException(m);}
    private void validate(double v,String m){if(!Double.isFinite(v)||v<0)throw new IllegalArgumentException(m);}
    private String clean(String v){return v==null?"":v.replace('|','/').replace('\n',' ').replace('\r',' ').trim();}
}
