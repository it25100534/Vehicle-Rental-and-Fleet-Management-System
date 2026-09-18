package com.example.vehicalrentalserviceplatform.billing.service;

import com.example.vehicalrentalserviceplatform.billing.model.Payment;
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
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Path PAYMENT_FILE_PATH = Paths.get("data", "payments.txt");
    private static final String SEPARATOR = " | ";

    private final Object fileLock = new Object();

    public String processPayment(Double amount, String method) {
        validatePayment(amount, method);

        Payment payment = new Payment(
                UUID.randomUUID().toString(),
                amount,
                method.trim(),
                LocalDateTime.now().toString()
        );

        savePayment(payment);

        return "Payment successful. Payment ID: " + payment.getPaymentId();
    }

    public List<Payment> readAllPayments() {
        synchronized (fileLock) {
            ensureFileExists();

            List<Payment> payments = new ArrayList<>();

            try (BufferedReader reader = Files.newBufferedReader(PAYMENT_FILE_PATH, StandardCharsets.UTF_8)) {
                String line;

                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        payments.add(parsePaymentLine(line));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read payments from file.", e);
            }

            return payments;
        }
    }

    private void savePayment(Payment payment) {
        synchronized (fileLock) {
            ensureFileExists();

            try (BufferedWriter writer = Files.newBufferedWriter(
                    PAYMENT_FILE_PATH,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            )) {
                writer.write(formatPaymentLine(payment));
                writer.newLine();
            } catch (IOException e) {
                throw new RuntimeException("Failed to save payment to file.", e);
            }
        }
    }

    private void ensureFileExists() {
        try {
            Path parentDirectory = PAYMENT_FILE_PATH.getParent();
            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }

            if (Files.notExists(PAYMENT_FILE_PATH)) {
                Files.createFile(PAYMENT_FILE_PATH);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare payment file.", e);
        }
    }

    private void validatePayment(Double amount, String method) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0.");
        }

        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("Payment method is required.");
        }
    }

    private String formatPaymentLine(Payment payment) {
        return payment.getPaymentId()
                + SEPARATOR
                + payment.getAmount()
                + SEPARATOR
                + sanitizeField(payment.getPaymentMethod())
                + SEPARATOR
                + payment.getPaymentDateTime();
    }

    private Payment parsePaymentLine(String line) {
        String[] parts = line.split("\\Q" + SEPARATOR + "\\E", 4);

        if (parts.length != 4) {
            throw new RuntimeException("Invalid payment record found in file: " + line);
        }

        return new Payment(
                parts[0],
                Double.parseDouble(parts[1]),
                parts[2],
                parts[3]
        );
    }

    private String sanitizeField(String value) {
        return value.replace(SEPARATOR, " ").trim();
    }
}
