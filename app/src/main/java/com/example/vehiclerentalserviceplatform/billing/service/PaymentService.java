package com.example.vehiclerentalserviceplatform.billing.service;

import com.example.vehiclerentalserviceplatform.billing.model.Payment;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    private static final Path LEGACY_FILE=Path.of("data","payments.txt");
    private static final String SEPARATOR=" | ";
    private final JdbcTemplate jdbc;
    public PaymentService(JdbcTemplate jdbc){this.jdbc=jdbc;}
    @PostConstruct void importLegacyWhenEmpty(){
        Long count=jdbc.queryForObject("select count(*) from payments",Long.class);
        if(count==null||count!=0||Files.notExists(LEGACY_FILE))return;
        try{for(String line:Files.readAllLines(LEGACY_FILE, StandardCharsets.UTF_8)){if(line.isBlank())continue;String[]p=line.split("\\Q"+SEPARATOR+"\\E",4);if(p.length==4)save(new Payment(p[0],Double.parseDouble(p[1]),p[2],p[3]));}}
        catch(Exception ex){throw new IllegalStateException("Could not import payments.",ex);}
    }
    public String processPayment(Double amount,String method){validatePayment(amount,method);Payment p=new Payment(UUID.randomUUID().toString(),amount,method.trim(),LocalDateTime.now().toString());save(p);return "Payment successful. Payment ID: "+p.getPaymentId();}
    public List<Payment> readAllPayments(){return jdbc.query("select * from payments order by paid_at desc",(rs,n)->new Payment(rs.getString("payment_id"),rs.getDouble("amount"),rs.getString("payment_method"),rs.getTimestamp("paid_at").toLocalDateTime().toString()));}
    private void save(Payment p){jdbc.update("insert into payments(payment_id,amount,payment_method,paid_at) values(?,?,?,?)",p.getPaymentId(),p.getAmount(),p.getPaymentMethod(),Timestamp.valueOf(LocalDateTime.parse(p.getPaymentDateTime())));}
    private void validatePayment(Double amount,String method){if(amount==null||amount<=0)throw new IllegalArgumentException("Amount must be greater than 0.");if(method==null||method.isBlank())throw new IllegalArgumentException("Payment method is required.");}
}
