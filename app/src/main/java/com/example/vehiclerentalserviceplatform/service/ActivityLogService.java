package com.example.vehiclerentalserviceplatform.service;

import com.example.vehiclerentalserviceplatform.model.ActivityLogEntry;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ActivityLogService {
    private static final DateTimeFormatter DISPLAY=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JdbcTemplate jdbc;
    public ActivityLogService(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @PostConstruct void importLegacyWhenEmpty(){
        Long count=jdbc.queryForObject("select count(*) from activity_log",Long.class);
        if(count==null||count!=0)return;
        importFile(Path.of("admin-activity.log"),"ADMIN",false);
        importFile(Path.of("rental-activity.log"),"RENTAL",true);
        importFile(Path.of("billing-activity.log"),"BILLING",true);
    }
    public void log(String module,String actor,String action,String reference,String details){jdbc.update("insert into activity_log(module_name,actor,action_name,record_reference,details,occurred_at) values(?,?,?,?,?,?)",clean(module),clean(actor),clean(action),clean(reference),clean(details),Timestamp.valueOf(LocalDateTime.now()));}
    public List<ActivityLogEntry> getAll(){return jdbc.query("select * from activity_log order by occurred_at desc, activity_id desc",(rs,n)->new ActivityLogEntry(rs.getTimestamp("occurred_at").toLocalDateTime().format(DISPLAY),rs.getString("actor"),rs.getString("action_name"),rs.getString("details")));}
    private void importFile(Path file,String module,boolean iso){
        if(Files.notExists(file))return;
        try{for(String line:Files.readAllLines(file, StandardCharsets.UTF_8)){if(line.isBlank())continue;String[] p=line.split("\\|",-1);if(p.length<4)continue;LocalDateTime time=iso?LocalDateTime.parse(p[0]):LocalDateTime.parse(p[0],DISPLAY);String details=String.join(" | ",java.util.Arrays.copyOfRange(p,3,p.length));jdbc.update("insert into activity_log(module_name,actor,action_name,record_reference,details,occurred_at) values(?,?,?,?,?,?)",module,p[1],p[2],null,details,Timestamp.valueOf(time));}}
        catch(Exception ex){throw new IllegalStateException("Could not import "+file+".",ex);}
    }
    private String clean(String value){return value==null?"":value.replace('|','/').replace('\n',' ').replace('\r',' ').trim();}
}
