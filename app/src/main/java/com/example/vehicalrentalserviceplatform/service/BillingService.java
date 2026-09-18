package com.example.vehicalrentalserviceplatform.service;

import com.example.vehicalrentalserviceplatform.model.CashPayment;
import com.example.vehicalrentalserviceplatform.model.CreditCardPayment;
import com.example.vehicalrentalserviceplatform.model.Invoice;
import com.example.vehicalrentalserviceplatform.model.InvoiceStatus;
import com.example.vehicalrentalserviceplatform.model.PaymentMethod;
import com.example.vehicalrentalserviceplatform.model.PaypalPayment;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BillingService {

    private static final Path FILE_PATH = Paths.get("billing.txt").toAbsolutePath();
    private static final String SEPARATOR = " | ";

    private final ConcurrentMap<Long, Invoice> invoices = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final Object fileLock = new Object();

    public BillingService() {
        loadInvoicesFromFile();
    }

    public Invoice createInvoice(Invoice invoice) {
        synchronized (fileLock) {

            invoice.setDiscount(0.0);
            invoice.setLateFee(0.0);

            validateInvoice(invoice);
            invoice.setId(nextId.getAndIncrement());

            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(LocalDateTime.now());

            if (invoice.getCreatedAt() == null) {
                invoice.setCreatedAt(LocalDateTime.now());
            }
            invoice.getPaymentMethod().validate();
            invoices.put(invoice.getId(), invoice);
            saveAllInvoices();
            return invoice;
        }
    }

    public List<Invoice> getAllInvoices() {
        return Collections.unmodifiableList(new ArrayList<>(invoices.values()));
    }

    public Optional<Invoice> getInvoiceById(Long id) {
        return Optional.ofNullable(invoices.get(id));
    }

    public Invoice updateInvoice(Long id, Invoice update) {
        synchronized (fileLock) {
            Invoice existing = invoices.get(id);
            if (existing == null) {
                throw new IllegalArgumentException("Invoice not found: " + id);
            }
            existing.setCustomerName(update.getCustomerName());
            existing.setVehicleId(update.getVehicleId());
            existing.setVehicleName(update.getVehicleName());
            existing.setRentalDays(update.getRentalDays());
            existing.setAmountPerDay(update.getAmountPerDay());
            if (update.getDiscount() != null && update.getDiscount() >= 0) {
                existing.applyDiscount(update.getDiscount());
            }
            if (update.getLateFee() != null && update.getLateFee() >= 0) {
                existing.applyLateFee(update.getLateFee());
            }
            if (update.getStatus() != null) {
                existing.setStatus(update.getStatus());
                if (update.getStatus() == InvoiceStatus.PAID) {
                    existing.markPaid();
                } else if (update.getStatus() != InvoiceStatus.PAID) {
                    existing.setPaidAt(null);
                }
            }
            if (update.getPaymentMethod() != null) {
                update.getPaymentMethod().validate();
                existing.setPaymentMethod(update.getPaymentMethod());
            }
            validateInvoice(existing);
            invoices.put(id, existing);
            saveAllInvoices();
            return existing;
        }
    }

    public void deleteInvoice(Long id) {
        synchronized (fileLock) {
            invoices.remove(id);
            saveAllInvoices();
        }
    }

    private void validateInvoice(Invoice invoice) {
        if (invoice.getCustomerName() == null || invoice.getCustomerName().isBlank()) {
            throw new IllegalArgumentException("Customer name is required.");
        }
        if (invoice.getVehicleId() == null || invoice.getVehicleId().isBlank()) {
            throw new IllegalArgumentException("Vehicle ID is required.");
        }
        if (invoice.getRentalDays() == null || invoice.getRentalDays() <= 0) {
            throw new IllegalArgumentException("Rental days must be greater than zero.");
        }
        if (invoice.getAmountPerDay() == null || invoice.getAmountPerDay() <= 0) {
            throw new IllegalArgumentException("Amount per day must be greater than zero.");
        }
        if (invoice.getDiscount() == null || invoice.getDiscount() < 0) {
            throw new IllegalArgumentException("Discount cannot be negative.");
        }
        if (invoice.getLateFee() == null || invoice.getLateFee() < 0) {
            throw new IllegalArgumentException("Late fee cannot be negative.");
        }
        if (invoice.getPaymentMethod() == null) {
            throw new IllegalArgumentException("Payment method is required.");
        }
    }

    private void loadInvoicesFromFile() {
        synchronized (fileLock) {
            ensureFileExists();

            long maxId = 0;

            try (BufferedReader reader = Files.newBufferedReader(FILE_PATH, StandardCharsets.UTF_8)) {
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }

                    Invoice invoice = parseInvoice(line);
                    invoices.put(invoice.getId(), invoice);
                    if (invoice.getId() != null && invoice.getId() > maxId) {
                        maxId = invoice.getId();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read billing records.", e);
            }

            nextId.set(maxId + 1);
        }
    }

    private void saveAllInvoices() {
        ensureFileExists();

        try (BufferedWriter writer = Files.newBufferedWriter(
                FILE_PATH,
                StandardCharsets.UTF_8,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            List<Long> ids = new ArrayList<>(invoices.keySet());
            Collections.sort(ids);

            for (Long id : ids) {
                writer.write(formatInvoice(invoices.get(id)));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save billing records.", e);
        }
    }

    private void ensureFileExists() {
        try {
            if (Files.notExists(FILE_PATH)) {
                Files.createFile(FILE_PATH);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare billing file.", e);
        }
    }

    private String formatInvoice(Invoice invoice) {
        return invoice.getId()
                + SEPARATOR + clean(invoice.getCustomerName())
                + SEPARATOR + clean(invoice.getVehicleId())
                + SEPARATOR + clean(invoice.getVehicleName())
                + SEPARATOR + invoice.getRentalDays()
                + SEPARATOR + invoice.getAmountPerDay()
                + SEPARATOR + invoice.getDiscount()
                + SEPARATOR + invoice.getLateFee()
                + SEPARATOR + invoice.getStatus().name()
                + SEPARATOR + invoice.getPaymentMethod().getType()
                + SEPARATOR + clean(getPaymentDetails(invoice.getPaymentMethod()))
                + SEPARATOR + formatDateTime(invoice.getCreatedAt())
                + SEPARATOR + formatDateTime(invoice.getPaidAt());
    }

    private Invoice parseInvoice(String line) {
        String[] parts = line.split("\\Q" + SEPARATOR + "\\E", -1);
        if (parts.length != 13) {
            throw new RuntimeException("Invalid billing record: " + line);
        }

        Invoice invoice = new Invoice();
        invoice.setId(Long.parseLong(parts[0]));
        invoice.setCustomerName(emptyToNull(parts[1]));
        invoice.setVehicleId(emptyToNull(parts[2]));
        invoice.setVehicleName(emptyToNull(parts[3]));
        invoice.setRentalDays(Integer.parseInt(parts[4]));
        invoice.setAmountPerDay(Double.parseDouble(parts[5]));
        invoice.setDiscount(Double.parseDouble(parts[6]));
        invoice.setLateFee(Double.parseDouble(parts[7]));
        invoice.setStatus(InvoiceStatus.valueOf(parts[8]));
        invoice.setPaymentMethod(buildPaymentMethod(parts[9], parts[10]));
        invoice.setCreatedAt(parseDateTime(parts[11]));
        invoice.setPaidAt(parseDateTime(parts[12]));
        return invoice;
    }

    private PaymentMethod buildPaymentMethod(String type, String details) {
        if ("creditCard".equals(type)) {
            String[] values = details.split(";", -1);
            CreditCardPayment payment = new CreditCardPayment();
            payment.setCardNumber(valueAt(values, 0));
            payment.setCardHolder(valueAt(values, 1));
            payment.setExpiryDate(valueAt(values, 2));
            payment.setCvv(valueAt(values, 3));
            return payment;
        }

        if ("paypal".equals(type)) {
            PaypalPayment payment = new PaypalPayment();
            payment.setPaypalEmail(details);
            return payment;
        }

        if ("cash".equals(type)) {
            CashPayment payment = new CashPayment();
            payment.setReceiptNumber(details);
            return payment;
        }

        throw new IllegalArgumentException("Unsupported payment type: " + type);
    }

    private String getPaymentDetails(PaymentMethod paymentMethod) {
        if (paymentMethod instanceof CreditCardPayment creditCardPayment) {
            return clean(creditCardPayment.getCardNumber()) + ";"
                    + clean(creditCardPayment.getCardHolder()) + ";"
                    + clean(creditCardPayment.getExpiryDate()) + ";"
                    + clean(creditCardPayment.getCvv());
        }

        if (paymentMethod instanceof PaypalPayment paypalPayment) {
            return clean(paypalPayment.getPaypalEmail());
        }

        if (paymentMethod instanceof CashPayment cashPayment) {
            return clean(cashPayment.getReceiptNumber());
        }

        throw new IllegalArgumentException("Unsupported payment type.");
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.toString();
    }

    private LocalDateTime parseDateTime(String value) {
        return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(SEPARATOR, " ").replace(";", " ").trim();
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String valueAt(String[] values, int index) {
        return index < values.length ? emptyToNull(values[index]) : null;
    }
}
