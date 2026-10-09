package com.example.vehiclerentalserviceplatform.maintenance;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

@Service
public class FleetComplianceFileHandler {
    private static final Path LEGACY_FILE=Path.of("fleet-compliance.txt").toAbsolutePath();
    private final JdbcTemplate jdbc;
    public FleetComplianceFileHandler(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @PostConstruct void importLegacyWhenEmpty(){
        Long count=jdbc.queryForObject("select count(*) from fleet_compliance",Long.class);
        if(count==null||count!=0||Files.notExists(LEGACY_FILE))return;
        try{for(String line:Files.readAllLines(LEGACY_FILE, StandardCharsets.UTF_8)){String[] p=line.split("\\|",-1);if(p.length==4)save(new FleetComplianceRecord(p[0],p[1],p[2],p[3]));}}
        catch(Exception ex){throw new IllegalStateException("Could not import fleet compliance records.",ex);}
    }
    public List<FleetComplianceRecord> loadAll(){return jdbc.query("select * from fleet_compliance order by vehicle_id",(rs,n)->new FleetComplianceRecord(rs.getString("vehicle_id"),rs.getDate("insurance_expiry").toLocalDate().toString(),rs.getDate("licence_expiry").toLocalDate().toString(),rs.getDate("emission_expiry").toLocalDate().toString()));}
    public void save(FleetComplianceRecord r){
        int changed=jdbc.update("update fleet_compliance set insurance_expiry=?,licence_expiry=?,emission_expiry=? where vehicle_id=?",LocalDate.parse(r.getInsuranceExpiry()),LocalDate.parse(r.getLicenceExpiry()),LocalDate.parse(r.getEmissionExpiry()),r.getVehicleId());
        if(changed==0)jdbc.update("insert into fleet_compliance(vehicle_id,insurance_expiry,licence_expiry,emission_expiry) values(?,?,?,?)",r.getVehicleId(),LocalDate.parse(r.getInsuranceExpiry()),LocalDate.parse(r.getLicenceExpiry()),LocalDate.parse(r.getEmissionExpiry()));
    }
}
