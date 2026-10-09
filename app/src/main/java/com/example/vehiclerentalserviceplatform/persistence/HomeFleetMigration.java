package com.example.vehiclerentalserviceplatform.persistence;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Component
@ConditionalOnProperty(name="driveease.home.apply-fleet-map", havingValue="true")
public class HomeFleetMigration implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public HomeFleetMigration(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override @Transactional public void run(ApplicationArguments args) {
        jdbc.execute("CREATE TABLE IF NOT EXISTS home_fleet_backup (vehicle_id VARCHAR(40) PRIMARY KEY, old_category VARCHAR(30), old_branch VARCHAR(30), applied_category VARCHAR(30), applied_branch VARCHAR(30))");
        Map<String,String> map=Map.ofEntries(
            Map.entry("CAR-0001","DAILY"),
            Map.entry("CAR-0002","DAILY"),
            Map.entry("CAR-0003","BUSINESS"),
            Map.entry("CAR-0004","FAMILY"),
            Map.entry("CAR-0005","DAILY"),
            Map.entry("CAR-0006","BUSINESS"),
            Map.entry("CAR-0007","FAMILY"),
            Map.entry("CAR-0008","BUSINESS"),
            Map.entry("CAR-0009","WEDDING"),
            Map.entry("SUV-0001","FAMILY"),
            Map.entry("SUV-0002","FAMILY"),
            Map.entry("SUV-0003","ADVENTURE"),
            Map.entry("SUV-0004","ADVENTURE"),
            Map.entry("SUV-0005","ADVENTURE"),
            Map.entry("SUV-0006","BUSINESS"),
            Map.entry("SUV-0007","FAMILY"),
            Map.entry("SUV-0008","TRANSPORT"),
            Map.entry("SUV-0009","DAILY"),
            Map.entry("VAN-0001","TRANSPORT"),
            Map.entry("VAN-0002","TRANSPORT"),
            Map.entry("VAN-0003","TRANSPORT"),
            Map.entry("VAN-0004","FAMILY"),
            Map.entry("VAN-0005","FAMILY"),
            Map.entry("VAN-0006","FAMILY"),
            Map.entry("VAN-0007","BUSINESS"),
            Map.entry("VAN-0008","TRANSPORT"),
            Map.entry("VAN-0009","TRANSPORT"),
            Map.entry("MOT-0001","DAILY"),
            Map.entry("MOT-0002","DAILY"),
            Map.entry("MOT-0003","DAILY"),
            Map.entry("MOT-0004","ADVENTURE"),
            Map.entry("MOT-0005","DAILY"),
            Map.entry("MOT-0006","ADVENTURE"),
            Map.entry("MOT-0007","DAILY"),
            Map.entry("MOT-0008","ADVENTURE"),
            Map.entry("MOT-0009","WEDDING"),
            Map.entry("PRE-0001","WEDDING"),
            Map.entry("PRE-0002","BUSINESS"),
            Map.entry("PRE-0003","BUSINESS"),
            Map.entry("PRE-0004","WEDDING"),
            Map.entry("PRE-0005","WEDDING"),
            Map.entry("PRE-0006","BUSINESS"),
            Map.entry("PRE-0007","BUSINESS"),
            Map.entry("PRE-0008","WEDDING"),
            Map.entry("PRE-0009","BUSINESS"));
        for (var entry:map.entrySet()) {
            String id=entry.getKey();
            if (jdbc.queryForObject("SELECT COUNT(*) FROM home_fleet_backup WHERE vehicle_id=?",Long.class,id)>0) continue;
            var rows=jdbc.queryForList("SELECT category_code,branch_id,vehicle_type,status,retired FROM vehicles WHERE vehicle_id=?",id);
            if (rows.isEmpty()) continue;
            var row=rows.get(0);
            String oldCategory=(String)row.get("category_code"), oldBranch=(String)row.get("branch_id");
            String defaultCategory=switch((String)row.get("vehicle_type")) {case "SUV" -> "FAMILY"; case "VAN" -> "TRANSPORT"; default -> "DAILY";};
            String category=oldCategory;
            if ((oldCategory.equals(defaultCategory) || oldCategory.equals(entry.getValue())) && jdbc.queryForObject("SELECT COUNT(*) FROM vehicle_categories WHERE code=? AND active=TRUE",Long.class,entry.getValue())>0) category=entry.getValue();
            String targetBranch=String.format("BR-%03d",(Integer.parseInt(id.substring(id.indexOf('-')+1))-1)%3+1);
            String branch=oldBranch;
            // Only redistribute old default assignments with no unfulfilled booking or service record.
            boolean busy=jdbc.queryForObject("SELECT COUNT(*) FROM bookings WHERE vehicle_id=? AND status IN ('Pending','Approved','Paid','Active')",Long.class,id)>0;
            busy |= jdbc.queryForObject("SELECT COUNT(*) FROM maintenance_records WHERE vehicle_id=? AND status IN ('Pending','In Progress')",Long.class,id)>0;
            if ("BR-001".equals(oldBranch) && "AVAILABLE".equals(row.get("status")) && !Boolean.TRUE.equals(row.get("retired")) && !busy && jdbc.queryForObject("SELECT COUNT(*) FROM branches WHERE branch_id=? AND status='OPEN'",Long.class,targetBranch)>0) branch=targetBranch;
            jdbc.update("INSERT INTO home_fleet_backup VALUES(?,?,?,?,?)",id,oldCategory,oldBranch,category,branch);
            jdbc.update("UPDATE vehicles SET category_code=?,branch_id=? WHERE vehicle_id=?",category,branch,id);
        }
        String[] names={"colombo","kandy","galle"};
        for (int i=0;i<names.length;i++) jdbc.update("UPDATE branches SET image_path=? WHERE branch_id=? AND (image_path IS NULL OR image_path='')","/images/home-redesign/branch-"+names[i]+".jpg",String.format("BR-%03d",i+1));
    }
}
