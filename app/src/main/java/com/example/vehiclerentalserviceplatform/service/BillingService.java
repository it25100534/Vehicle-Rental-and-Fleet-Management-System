package com.example.vehiclerentalserviceplatform.service;

import com.example.vehiclerentalserviceplatform.model.*;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BillingService {
    private static final Path LEGACY_FILE=Path.of("billing.txt").toAbsolutePath();
    private static final String SEP=" | ";
    private static final Pattern FULL_CARD=Pattern.compile("(?<!\\d)(\\d{13,19})(?!\\d)");
    private static final Pattern LAST_FOUR=Pattern.compile("(?<!\\d)(\\d{4})(?!\\d)");
    private final JdbcTemplate jdbc;
    private final ActivityLogService activity;
    public BillingService(JdbcTemplate jdbc,ActivityLogService activity){this.jdbc=jdbc;this.activity=activity;}

    @PostConstruct void importLegacyWhenEmpty(){
        Long count=jdbc.queryForObject("select count(*) from invoices",Long.class);
        if(count==null||count!=0||Files.notExists(LEGACY_FILE))return;
        try{for(String line:Files.readAllLines(LEGACY_FILE, StandardCharsets.UTF_8))if(!line.isBlank())insert(parse(line),true);}
        catch(Exception ex){throw new IllegalStateException("Could not import invoices.",ex);}
    }

    public synchronized Invoice createInvoice(Invoice invoice){
        invoice.setLateFee(0.0);invoice.setDamageFee(0.0);invoice.setFuelCharge(0.0);invoice.setMileageCharge(0.0);invoice.setRefundAmount(0.0);
        validateInvoice(invoice);invoice.setStatus(InvoiceStatus.PAID);invoice.setPaidAt(LocalDateTime.now());if(invoice.getCreatedAt()==null)invoice.setCreatedAt(LocalDateTime.now());invoice.getPaymentMethod().validate();protect(invoice.getPaymentMethod());
        insert(invoice,false);return invoice;
    }
    public List<Invoice> getAllInvoices(){return query(" order by i.invoice_id");}
    public List<Invoice> getInvoicesForCustomer(String customer){if(customer==null||customer.isBlank())return List.of();return query(" where lower(i.customer_username)=lower(?) order by i.invoice_id desc",customer);}
    public Optional<Invoice> getInvoiceById(Long id){return query(" where i.invoice_id=?",id).stream().findFirst();}
    public Optional<Invoice> getInvoiceByTransactionId(String id){if(id==null||id.isBlank())return Optional.empty();return query(" where i.booking_id=?",id).stream().findFirst();}
    public Optional<Invoice> findInvoiceForBooking(String id,String customer,String vehicle){
        Optional<Invoice> linked=getInvoiceByTransactionId(id);if(linked.isPresent())return linked;
        if(customer==null||vehicle==null)return Optional.empty();
        return query(" where i.booking_id is null and lower(i.customer_username)=lower(?) and lower(i.vehicle_id)=lower(?) order by i.invoice_id desc",customer,vehicle).stream().findFirst();
    }
    public Invoice applyReturnAdjustments(String id,double late,double damage,double fuel,double mileage,double dropoff,double deposit,double refund){return applyReturnAdjustments(id,null,null,late,damage,fuel,mileage,dropoff,deposit,refund);}
    public synchronized Invoice applyReturnAdjustments(String id,String customer,String vehicle,double late,double damage,double fuel,double mileage,double dropoff,double deposit,double refund){
        Invoice i=findInvoiceForBooking(id,customer,vehicle).orElseThrow(()->new IllegalArgumentException("No paid invoice is linked to booking "+id+"."));
        validateMoney(late,"Late fee");validateMoney(damage,"Damage fee");validateMoney(fuel,"Fuel charge");validateMoney(mileage,"Mileage charge");validateMoney(dropoff,"Alternate drop-off charge");validateMoney(deposit,"Deposit amount");validateMoney(refund,"Refund amount");
        jdbc.update("update invoices set booking_id=?,late_fee=?,damage_fee=?,fuel_charge=?,mileage_charge=?,alternate_dropoff_charge=?,deposit_amount=?,refund_amount=? where invoice_id=?",id,late,damage,fuel,mileage,dropoff,deposit,refund,i.getId());return require(i.getId());
    }
    public synchronized Invoice updateInvoice(Long id,Invoice u){
        Invoice e=require(id);e.setCustomerName(u.getCustomerName());e.setVehicleId(u.getVehicleId());e.setVehicleName(u.getVehicleName());e.setRentalDays(u.getRentalDays());e.setAmountPerDay(u.getAmountPerDay());
        if(u.getDiscount()!=null&&u.getDiscount()>=0)e.setDiscount(u.getDiscount());if(u.getLateFee()!=null&&u.getLateFee()>=0)e.setLateFee(u.getLateFee());
        if(u.getStatus()!=null){e.setStatus(u.getStatus());if(u.getStatus()==InvoiceStatus.PAID)e.markPaid();else e.setPaidAt(null);}
        if(u.getPaymentMethod()!=null){u.getPaymentMethod().validate();protect(u.getPaymentMethod());e.setPaymentMethod(u.getPaymentMethod());}
        validateInvoice(e);update(e);return require(id);
    }
    public synchronized Invoice updateAmounts(Long id,double discount,double late,double damage,double fuel,double mileage,double dropoff,double deposit){
        Invoice i=require(id);if(i.getStatus()==InvoiceStatus.VOID||i.getStatus()==InvoiceStatus.REFUNDED)throw new IllegalStateException("Finalized invoices cannot be edited.");
        validateMoney(discount,"Discount");validateMoney(late,"Late fee");validateMoney(damage,"Damage fee");validateMoney(fuel,"Fuel charge");validateMoney(mileage,"Mileage charge");validateMoney(dropoff,"Alternate drop-off charge");validateMoney(deposit,"Deposit amount");
        jdbc.update("update invoices set discount=?,late_fee=?,damage_fee=?,fuel_charge=?,mileage_charge=?,alternate_dropoff_charge=?,deposit_amount=? where invoice_id=?",discount,late,damage,fuel,mileage,dropoff,deposit,id);activity.log("BILLING","system","EDIT",id.toString(),"Billing amounts updated");return require(id);
    }
    public synchronized Invoice voidInvoice(Long id,String reason){Invoice i=require(id);i.voidInvoice(reason);jdbc.update("update invoices set status=?,voided_at=?,void_reason=? where invoice_id=?",i.getStatus().name(),Timestamp.valueOf(i.getVoidedAt()),i.getVoidReason(),id);activity.log("BILLING","system","VOID",id.toString(),reason);return require(id);}
    public synchronized Invoice refundInvoice(Long id,double amount,String reason){Invoice i=require(id);i.recordRefund(amount,reason);jdbc.update("update invoices set status=?,refund_amount=?,refunded_at=?,refund_reason=? where invoice_id=?",i.getStatus().name(),i.getRefundAmount(),Timestamp.valueOf(i.getRefundedAt()),i.getRefundReason(),id);activity.log("BILLING","system","REFUND",id.toString(),"Rs. "+amount+" - "+reason);return require(id);}
    public void deleteInvoice(Long id){throw new UnsupportedOperationException("Invoices are retained for audit. Use void instead.");}
    public String getSafePaymentLabel(Invoice i){if(i==null||i.getPaymentMethod()==null)return "Unknown";if(i.getPaymentMethod() instanceof CreditCardPayment c)return "Card ending "+c.getCardLastFour();if(i.getPaymentMethod() instanceof PaypalPayment p)return "PayPal "+maskEmail(p.getPaypalEmail());if(i.getPaymentMethod() instanceof CashPayment c)return "Cash receipt "+clean(c.getReceiptNumber());return "Unknown";}

    private Invoice require(Long id){return getInvoiceById(id).orElseThrow(()->new IllegalArgumentException("Invoice not found: "+id));}
    private List<Invoice> query(String suffix,Object...args){String sql="select i.*, concat(v.make, ' ', v.model) vehicle_name from invoices i join vehicles v on v.vehicle_id=i.vehicle_id"+suffix;return jdbc.query(sql,(rs,n)->{Invoice i=new Invoice();i.setId(rs.getLong("invoice_id"));i.setTransactionId(rs.getString("booking_id"));i.setCustomerName(rs.getString("customer_username"));i.setVehicleId(rs.getString("vehicle_id"));i.setVehicleName(rs.getString("vehicle_name"));i.setBranchId(rs.getString("branch_id"));i.setRentalDays(rs.getInt("rental_days"));i.setAmountPerDay(rs.getDouble("amount_per_day"));i.setDiscount(rs.getDouble("discount"));i.setLateFee(rs.getDouble("late_fee"));i.setDamageFee(rs.getDouble("damage_fee"));i.setFuelCharge(rs.getDouble("fuel_charge"));i.setMileageCharge(rs.getDouble("mileage_charge"));i.setAlternateDropoffCharge(rs.getDouble("alternate_dropoff_charge"));i.setDepositAmount(rs.getDouble("deposit_amount"));i.setRefundAmount(rs.getDouble("refund_amount"));i.setStatus(InvoiceStatus.valueOf(rs.getString("status")));i.setPaymentMethod(buildPayment(rs.getString("payment_type"),rs.getString("payment_reference")));i.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());i.setPaidAt(time(rs.getTimestamp("paid_at")));i.setVoidedAt(time(rs.getTimestamp("voided_at")));i.setVoidReason(rs.getString("void_reason"));i.setRefundedAt(time(rs.getTimestamp("refunded_at")));i.setRefundReason(rs.getString("refund_reason"));return i;},args);}
    private void insert(Invoice i,boolean explicit){String cols=explicit?"invoice_id,booking_id,customer_username,vehicle_id,branch_id,rental_days,amount_per_day,discount,late_fee,damage_fee,fuel_charge,mileage_charge,alternate_dropoff_charge,deposit_amount,refund_amount,status,payment_type,payment_reference,created_at,paid_at,voided_at,void_reason,refunded_at,refund_reason":"booking_id,customer_username,vehicle_id,branch_id,rental_days,amount_per_day,discount,late_fee,damage_fee,fuel_charge,mileage_charge,alternate_dropoff_charge,deposit_amount,refund_amount,status,payment_type,payment_reference,created_at,paid_at,voided_at,void_reason,refunded_at,refund_reason";String marks=String.join(",",java.util.Collections.nCopies(explicit?24:23,"?"));Object[]v=values(i,explicit);if(explicit){jdbc.update("insert into invoices("+cols+") values("+marks+")",v);}else{KeyHolder key=new GeneratedKeyHolder();jdbc.update(c->{PreparedStatement ps=c.prepareStatement("insert into invoices("+cols+") values("+marks+")",Statement.RETURN_GENERATED_KEYS);for(int n=0;n<v.length;n++)ps.setObject(n+1,v[n]);return ps;},key);i.setId(key.getKey().longValue());}}
    private Object[] values(Invoice i,boolean id){java.util.ArrayList<Object>v=new java.util.ArrayList<>();if(id)v.add(i.getId());v.add(i.getTransactionId());v.add(i.getCustomerName());v.add(i.getVehicleId());v.add(i.getBranchId());v.add(i.getRentalDays());v.add(i.getAmountPerDay());v.add(i.getDiscount());v.add(i.getLateFee());v.add(i.getDamageFee());v.add(i.getFuelCharge());v.add(i.getMileageCharge());v.add(i.getAlternateDropoffCharge());v.add(i.getDepositAmount());v.add(i.getRefundAmount());v.add(i.getStatus().name());v.add(i.getPaymentMethod().getType());v.add(paymentReference(i.getPaymentMethod()));v.add(ts(i.getCreatedAt()));v.add(ts(i.getPaidAt()));v.add(ts(i.getVoidedAt()));v.add(i.getVoidReason());v.add(ts(i.getRefundedAt()));v.add(i.getRefundReason());return v.toArray();}
    private void update(Invoice i){jdbc.update("update invoices set customer_username=?,vehicle_id=?,rental_days=?,amount_per_day=?,discount=?,late_fee=?,status=?,payment_type=?,payment_reference=?,paid_at=? where invoice_id=?",i.getCustomerName(),i.getVehicleId(),i.getRentalDays(),i.getAmountPerDay(),i.getDiscount(),i.getLateFee(),i.getStatus().name(),i.getPaymentMethod().getType(),paymentReference(i.getPaymentMethod()),ts(i.getPaidAt()),i.getId());}
    private Invoice parse(String line){String[]p=line.split("\\Q"+SEP+"\\E",-1);if(p.length!=13&&p.length!=20&&p.length!=25)throw new IllegalStateException("Invalid billing record: "+line);Invoice i=new Invoice();i.setId(Long.parseLong(p[0]));i.setCustomerName(empty(p[1]));i.setVehicleId(empty(p[2]));i.setVehicleName(empty(p[3]));i.setRentalDays(Integer.parseInt(p[4]));i.setAmountPerDay(Double.parseDouble(p[5]));i.setDiscount(Double.parseDouble(p[6]));i.setLateFee(Double.parseDouble(p[7]));i.setStatus(InvoiceStatus.valueOf(p[8]));i.setPaymentMethod(buildPayment(p[9],p[10]));i.setCreatedAt(parseTime(p[11]));i.setPaidAt(parseTime(p[12]));i.setTransactionId(at(p,13));i.setDamageFee(num(p,14));i.setFuelCharge(num(p,15));i.setMileageCharge(num(p,16));i.setAlternateDropoffCharge(num(p,17));i.setDepositAmount(num(p,18));i.setRefundAmount(num(p,19));i.setBranchId(at(p,20));i.setVoidedAt(parseTime(at(p,21)));i.setVoidReason(at(p,22));i.setRefundedAt(parseTime(at(p,23)));i.setRefundReason(at(p,24));return i;}
    private PaymentMethod buildPayment(String type,String ref){if("creditCard".equals(type)){CreditCardPayment p=new CreditCardPayment();p.loadSafeReference(lastFour(ref));return p;}if("paypal".equals(type)){PaypalPayment p=new PaypalPayment();p.setPaypalEmail(ref);return p;}if("cash".equals(type)){CashPayment p=new CashPayment();p.setReceiptNumber(ref);return p;}throw new IllegalArgumentException("Unsupported payment type: "+type);}
    private String paymentReference(PaymentMethod p){if(p instanceof CreditCardPayment c)return c.getCardLastFour();if(p instanceof PaypalPayment x)return clean(x.getPaypalEmail());if(p instanceof CashPayment c)return clean(c.getReceiptNumber());throw new IllegalArgumentException("Unsupported payment type.");}
    private void protect(PaymentMethod p){if(p instanceof CreditCardPayment c)c.retainSafeReference();}
    private String lastFour(String s){if(s==null)throw new IllegalArgumentException("Stored card reference is missing.");Matcher m=FULL_CARD.matcher(s);if(m.find()){String n=m.group(1);return n.substring(n.length()-4);}m=LAST_FOUR.matcher(s);if(m.find())return m.group(1);throw new IllegalArgumentException("Stored card reference is invalid.");}
    private void validateInvoice(Invoice i){if(i.getCustomerName()==null||i.getCustomerName().isBlank())throw new IllegalArgumentException("Customer name is required.");if(i.getVehicleId()==null||i.getVehicleId().isBlank())throw new IllegalArgumentException("Vehicle ID is required.");if(i.getRentalDays()==null||i.getRentalDays()<=0||i.getRentalDays()>3650)throw new IllegalArgumentException("Rental days must be between 1 and 3650.");if(i.getAmountPerDay()==null||!Double.isFinite(i.getAmountPerDay())||i.getAmountPerDay()<=0||i.getAmountPerDay()>10000000)throw new IllegalArgumentException("Amount per day must be greater than zero and within the supported range.");validateMoney(i.getDiscount(),"Discount");validateMoney(i.getLateFee(),"Late fee");validateMoney(i.getDamageFee(),"Damage fee");validateMoney(i.getFuelCharge(),"Fuel charge");validateMoney(i.getMileageCharge(),"Mileage charge");validateMoney(i.getAlternateDropoffCharge(),"Alternate drop-off charge");validateMoney(i.getDepositAmount(),"Deposit amount");validateMoney(i.getRefundAmount(),"Refund amount");if(i.getPaymentMethod()==null)throw new IllegalArgumentException("Payment method is required.");}
    private void validateMoney(Double v,String label){if(v==null||!Double.isFinite(v)||v<0||v>10000000)throw new IllegalArgumentException(label+" must be between 0 and 10,000,000.");}
    private Timestamp ts(LocalDateTime t){return t==null?null:Timestamp.valueOf(t);}private LocalDateTime time(Timestamp t){return t==null?null:t.toLocalDateTime();}private LocalDateTime parseTime(String s){return s==null||s.isBlank()?null:LocalDateTime.parse(s);}private String at(String[]p,int i){return i<p.length?empty(p[i]):null;}private double num(String[]p,int i){return i<p.length&&!p[i].isBlank()?Double.parseDouble(p[i]):0;}private String empty(String s){return s==null||s.isBlank()?null:s;}private String clean(String s){return s==null?"":s.replace(SEP," ").replace(";"," ").trim();}private String maskEmail(String s){if(s==null||!s.contains("@"))return "account";String[]p=s.split("@",2);return p[0].substring(0,Math.min(2,p[0].length()))+"***@"+p[1];}
}
