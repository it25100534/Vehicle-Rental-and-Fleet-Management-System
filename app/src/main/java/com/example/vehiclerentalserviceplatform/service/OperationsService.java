package com.example.vehiclerentalserviceplatform.service;

import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@DependsOn("licenceSubmissionSchemaMigration")
public class OperationsService {
    public record Quote(double rentalSubtotal, double extras, double discount, double deposit,
                        double alternateDropoff, double total) {}

    private final JdbcTemplate jdbc;
    private final BookingService bookings;
    private final ActivityLogService activity;
    private final BillingService billing;
    private final NotificationEventService notifications;

    public OperationsService(JdbcTemplate jdbc, BookingService bookings, ActivityLogService activity,
                             BillingService billing, NotificationEventService notifications) {
        this.jdbc = jdbc; this.bookings = bookings; this.activity = activity; this.billing = billing;
        this.notifications = notifications;
    }

    public List<Map<String,Object>> customers() {
        List<Map<String,Object>> rows = jdbc.queryForList("select c.username,coalesce(c.full_name,c.username) as full_name,c.license_id,c.email,c.phone,c.active,c.license_verified,"
                + "coalesce(l.status,case when c.license_verified then 'APPROVED' else 'NOT_SUBMITTED' end) as licence_status,"
                + "case when l.username is null then false else true end as licence_has_images "
                + "from customers c left join licence_submissions l on l.username=c.username "
                + "order by case when l.status='PENDING' then 0 else 1 end, "
                + "coalesce(l.reviewed_at,l.submitted_at) desc, c.username");
        for (Map<String, Object> row : rows) {
            Object hasImages = row.get("licence_has_images");
            row.put("licence_has_images", Boolean.TRUE.equals(hasImages)
                    || hasImages instanceof Number number && number.intValue() != 0);
        }
        return rows;
    }
    @Transactional
    public void setLicenceVerified(String username, boolean verified, String actor) {
        if (verified) throw new IllegalArgumentException("Approve the front and back licence images in the review window.");
        else {
            jdbc.update("update licence_submissions set status='REJECTED',reason='Verification revoked by staff',reviewed_at=?,reviewed_by=? where username=? and status='APPROVED'",
                    Timestamp.from(java.time.Instant.now()), actor, username);
        }
        requireOne(jdbc.update("update customers set license_verified=? where username=?", verified, username), "Customer");
        activity.log("CUSTOMERS", actor, verified ? "VERIFY_LICENSE" : "REVOKE_LICENSE", username, "Licence verification changed");
        notifications.customer(username, "Driving licence verification revoked",
                "Your licence verification was revoked. Please upload current front and back images for review.",
                "/profile#licence-verification", "warning");
    }
    public boolean isLicenceVerified(String username) {
        Boolean result = jdbc.queryForObject("select license_verified from customers where lower(username)=lower(?)", Boolean.class, username);
        return Boolean.TRUE.equals(result);
    }
    @Transactional
    public void setCustomerActive(String username, boolean active, String actor) {
        requireOne(jdbc.update("update customers set active=? where username=?", active, username), "Customer");
        activity.log("CUSTOMERS", actor, active ? "ACTIVATE" : "DEACTIVATE", username, "Customer access changed");
        notifications.customer(username, active ? "Account enabled" : "Account disabled",
                active ? "Your DriveEase account is active again." : "Your DriveEase account has been disabled. Contact our team for help.",
                "/profile", active ? "success" : "warning");
    }

    public void assignStaffBranch(String userId, String branchId, String actor) {
        requireOne(jdbc.update("update staff_accounts set home_branch_id=? where user_id=?", branchId, userId), "Staff account");
        activity.log("STAFF", actor, "ASSIGN_BRANCH", userId, "Home branch: " + branchId);
    }

    public void enrichBooking(String id, String pickup, String dropoff, double extras, String promo) {
        double alternate = pickup.equalsIgnoreCase(dropoff) ? 0 : 2500;
        double discount = "DRIVE10".equalsIgnoreCase(promo) ? 0.10 : 0;
        jdbc.update("update bookings set pickup_branch_id=?,dropoff_branch_id=?,extras_total=?,promo_code=?,discount_amount=?,alternate_dropoff_charge=? where transaction_id=?",
                pickup, dropoff, money(extras), clean(promo), discount, alternate, id);
    }

    public List<Map<String,Object>> bookingQueue(String status, String branch) {
        StringBuilder sql = new StringBuilder("select b.*,concat(v.make,' ',v.model) vehicle_name from bookings b join vehicles v on v.vehicle_id=b.vehicle_id where 1=1");
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) { sql.append(" and lower(b.status)=lower(?)"); args.add(status); }
        if (branch != null && !branch.isBlank()) { sql.append(" and lower(coalesce(b.pickup_branch_id,v.branch_id))=lower(?)"); args.add(branch); }
        sql.append(" order by b.start_date desc");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    @Transactional
    public void reassignVehicle(String bookingId, String vehicleId, String actor) {
        Map<String,Object> b = jdbc.queryForMap("select * from bookings where transaction_id=?", bookingId);
        String status = String.valueOf(b.get("status"));
        if (!(status.equalsIgnoreCase("Pending") || status.equalsIgnoreCase("Approved")))
            throw new IllegalStateException("Only pending or approved bookings can be reassigned.");
        String start = String.valueOf(b.get("start_date")); String end = String.valueOf(b.get("return_date"));
        if (!bookings.isVehicleAvailableForUpdate(bookingId, vehicleId, start, end))
            throw new IllegalArgumentException("The replacement vehicle is unavailable for those dates.");
        String branch = jdbc.queryForObject("select branch_id from vehicles where vehicle_id=? and status='AVAILABLE'", String.class, vehicleId);
        requireOne(jdbc.update("update bookings set vehicle_id=?,pickup_branch_id=coalesce(pickup_branch_id,?) where transaction_id=?", vehicleId, branch, bookingId), "Booking");
        activity.log("BOOKINGS", actor, "REASSIGN", bookingId, "Vehicle changed to " + vehicleId);
        notifications.customer(String.valueOf(b.get("customer_username")), "Booking vehicle changed",
                "Your booking " + bookingId + " was assigned " + vehicleId + ".", "/reservationHistory", "warning");
    }

    @Transactional
    public double cancelBooking(String bookingId, String customer, String reason, String actor) {
        Map<String,Object> b = jdbc.queryForMap("select * from bookings where transaction_id=?", bookingId);
        if (customer != null && !customer.equalsIgnoreCase(String.valueOf(b.get("customer_username"))))
            throw new SecurityException("This booking belongs to another customer.");
        String status = String.valueOf(b.get("status"));
        if (List.of("Cancelled","Rejected","Returned","Completed","Active").stream().anyMatch(s -> s.equalsIgnoreCase(status)))
            throw new IllegalStateException("This booking cannot be cancelled in its current status.");
        LocalDate start = date(b.get("start_date"));
        if (start.isBefore(LocalDate.now())) throw new IllegalStateException("A booking that has already started cannot be cancelled.");
        long hours = ChronoUnit.HOURS.between(LocalDateTime.now(), start.atStartOfDay());
        double fee = status.equalsIgnoreCase("Pending") ? 0 : (hours >= 48 ? 0 : 2500);
        String cleanReason = required(reason, "Cancellation reason");
        if (status.equalsIgnoreCase("Paid")) {
            var invoice = billing.getInvoiceByTransactionId(bookingId)
                    .orElseThrow(() -> new IllegalStateException("The paid invoice for this booking was not found."));
            double refundable = Math.max(0, invoice.getAmountBeforeRefund() - fee);
            if (refundable > 0) billing.refundInvoice(invoice.getId(), refundable, "Booking cancelled: " + cleanReason);
        }
        jdbc.update("update bookings set status='Cancelled',cancellation_fee=?,cancellation_reason=?,cancelled_at=? where transaction_id=?",
                fee, cleanReason, Timestamp.valueOf(LocalDateTime.now()), bookingId);
        activity.log("BOOKINGS", actor, "CANCEL", bookingId, "Cancellation fee Rs. " + fee + ": " + cleanReason);
        if (customer != null) notifications.staff("Booking cancelled", customer + " cancelled booking " + bookingId + ".",
                "/admin/bookings");
        return fee;
    }

    public Quote quote(String bookingId) {
        Map<String,Object> b = jdbc.queryForMap("select b.*,v.rental_rate from bookings b join vehicles v on v.vehicle_id=b.vehicle_id where b.transaction_id=?", bookingId);
        LocalDate start = date(b.get("start_date"));
        LocalDate end = date(b.get("return_date"));
        double subtotal = ChronoUnit.DAYS.between(start,end) * number(b.get("rental_rate"));
        double extras = number(b.get("extras_total"));
        double discountRate = "DRIVE10".equalsIgnoreCase(String.valueOf(b.get("promo_code"))) ? .10 : 0;
        double discount = (subtotal + extras) * discountRate;
        double deposit = Math.max(5000, Math.min(25000, subtotal * .20));
        double dropoff = number(b.get("alternate_dropoff_charge"));
        jdbc.update("update bookings set discount_amount=?,deposit_amount=? where transaction_id=?", discount, deposit, bookingId);
        return new Quote(money(subtotal), money(extras), money(discount), money(deposit), money(dropoff), money(subtotal + extras - discount + deposit + dropoff));
    }

    public Quote quoteForCustomer(String bookingId, String customer) {
        Integer owned = jdbc.queryForObject(
                "select count(*) from bookings where transaction_id=? and lower(customer_username)=lower(?)",
                Integer.class, bookingId, customer);
        if (owned == null || owned != 1) throw new SecurityException("This booking belongs to another customer.");
        return quote(bookingId);
    }

    public void recordPaymentAttempt(String bookingId, Long invoiceId, String key, double amount,
                                     String method, boolean success, String reason) {
        Integer existing = jdbc.queryForObject("select count(*) from payments where idempotency_key=?", Integer.class, key);
        if (existing != null && existing > 0) {
            jdbc.update("update payments set invoice_id=?,amount=?,payment_method=?,status=?,failure_reason=?,paid_at=? where idempotency_key=?",
                    invoiceId, amount, method, success ? "PAID" : "FAILED", success ? null : clean(reason),
                    success ? Timestamp.valueOf(LocalDateTime.now()) : null, key);
            return;
        }
        jdbc.update("insert into payments(payment_id,booking_id,invoice_id,idempotency_key,amount,payment_method,status,failure_reason,paid_at) values(?,?,?,?,?,?,?,?,?)",
                "PAY-" + UUID.randomUUID().toString().substring(0,8).toUpperCase(), bookingId, invoiceId, key,
                amount, method, success ? "PAID" : "FAILED", success ? null : clean(reason), success ? Timestamp.valueOf(LocalDateTime.now()) : null);
    }
    public boolean paymentKeyExists(String key) {
        Integer n = jdbc.queryForObject("select count(*) from payments where idempotency_key=? and status='PAID'", Integer.class, key);
        return n != null && n > 0;
    }

    @Transactional
    public void saveEnquiry(String username, String name, String email, String subject, String message) {
        com.example.vehiclerentalserviceplatform.security.InputValidation.requireSafeText(name, "Name", 2, 120);
        if (!com.example.vehiclerentalserviceplatform.security.InputValidation.isEmail(email) || email.trim().length() > 160)
            throw new IllegalArgumentException("Enter a valid email address.");
        com.example.vehiclerentalserviceplatform.security.InputValidation.requireSafeText(subject, "Subject", 3, 160);
        String cleanMessage = required(message, "Message");
        if (cleanMessage.length() < 10 || cleanMessage.length() > 1000)
            throw new IllegalArgumentException("Message must contain 10 to 1000 characters.");
        jdbc.update("insert into contact_enquiries(customer_username,name,email,subject,message,status,created_at) values(?,?,?,?,?,'NEW',?)",
                clean(username), name.trim(), email.trim(), subject.trim(), cleanMessage, Timestamp.valueOf(LocalDateTime.now()));
        notifications.staff("New contact enquiry", name.trim() + ": " + subject.trim(), "/admin/contact-enquiries");
    }
    public List<Map<String,Object>> enquiries() { return jdbc.queryForList("select * from contact_enquiries order by created_at desc"); }
    @Transactional
    public void resolveEnquiry(long id, String actor) {
        Map<String,Object> enquiry = jdbc.queryForMap("select customer_username,subject from contact_enquiries where enquiry_id=?", id);
        requireOne(jdbc.update("update contact_enquiries set status='RESOLVED' where enquiry_id=?", id), "Enquiry");
        activity.log("CONTACT", actor, "RESOLVE", Long.toString(id), "Enquiry resolved");
        notifications.customer((String) enquiry.get("customer_username"), "Enquiry reviewed",
                "Our team reviewed your message: " + enquiry.get("subject") + ".", "/contact", "success");
    }

    public List<Map<String,Object>> vehicleTimeline(String vehicleId) {
        List<Map<String,Object>> rows = new ArrayList<>();
        rows.addAll(jdbc.queryForList("select scheduled_date event_date,'MAINTENANCE' event_type,service_type title,status,notes details from maintenance_records where vehicle_id=?", vehicleId));
        rows.addAll(jdbc.queryForList("select start_date event_date,'BOOKING' event_type,transaction_id title,status,concat(customer_username,' to ',return_date) details from bookings where vehicle_id=?", vehicleId));
        rows.addAll(jdbc.queryForList("select cast(handed_over_at as date) event_date,'INSPECTION' event_type,record_id title,status,concat(condition_out,coalesce(concat(' / ',condition_in),'')) details from rentals where vehicle_id=?", vehicleId));
        rows.sort(Comparator.comparing(m -> String.valueOf(m.get("event_date")), Comparator.reverseOrder()));
        return rows;
    }

    @Transactional
    public void voidInspection(String rentalId, String reason, String actor) {
        String status = jdbc.queryForObject("select status from rentals where record_id=?", String.class, rentalId);
        if ("VOID".equalsIgnoreCase(status)) throw new IllegalStateException("Inspection is already voided.");
        jdbc.update("update rentals set status='VOID' where record_id=?", rentalId);
        jdbc.update("insert into rental_inspection_audit(rental_id,action_name,reason,actor,occurred_at) values(?,'VOID',?,?,?)",
                rentalId, required(reason,"Void reason"), actor, Timestamp.valueOf(LocalDateTime.now()));
        activity.log("RENTALS", actor, "VOID_INSPECTION", rentalId, reason);
    }

    private void requireOne(int count, String label) { if (count != 1) throw new IllegalArgumentException(label + " was not found."); }
    private String required(String value,String label){ if(value==null||value.isBlank())throw new IllegalArgumentException(label+" is required."); return value.trim(); }
    private String clean(String value){return value==null||value.isBlank()?null:value.trim();}
    private double number(Object value){return value==null?0:((Number)value).doubleValue();}
    private LocalDate date(Object value){if(value instanceof LocalDate d)return d;if(value instanceof java.sql.Date d)return d.toLocalDate();return LocalDate.parse(String.valueOf(value));}
    private double money(double value){return BigDecimal.valueOf(value).setScale(2, java.math.RoundingMode.HALF_UP).doubleValue();}
}
