package com.example.vehicalrentalserviceplatform.service;

import com.example.vehicalrentalserviceplatform.model.ActivityLogEntry;
import com.example.vehicalrentalserviceplatform.model.Admin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminStaffService {
    private static final String ADMIN_FILE = "admins.txt";
    private static final String ACTIVITY_FILE = "admin-activity.log";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String register(String fullName, String username, String role, String employeeType, String rawPassword, String actor) {
        List<Admin> admins = getAllAdmins();

        for (Admin admin : admins) {
            if (admin.getUsername().equalsIgnoreCase(username)) {
                return "Username already exists.";
            }
        }

        Admin newAdmin = new Admin(
                UUID.randomUUID().toString(),
                sanitize(fullName),
                sanitize(username),
                sanitize(role),
                sanitize(employeeType),
                hashPassword(rawPassword)
        );

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ADMIN_FILE, true))) {
            writer.write(newAdmin.toFileRecord());
            writer.newLine();
            logActivity(actor, "CREATE", "Created " + role + " account for username: " + username);
            return "Registration successful.";
        } catch (IOException e) {
            return "Unable to register account.";
        }
    }

    public List<Admin> getAllAdmins() {
        List<Admin> admins = new ArrayList<>();
        File file = new File(ADMIN_FILE);

        if (!file.exists()) {
            return admins;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length == 6) {
                    admins.add(new Admin(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]));
                }
            }
        } catch (IOException ignored) {
        }
        return admins;
    }

    public Admin authenticate(String username, String rawPassword) {
        List<Admin> admins = getAllAdmins();
        String hashedInput = hashPassword(rawPassword);

        for (Admin admin : admins) {
            // Checks if the username matches and the password hashes are identical
            if (admin.getUsername().equalsIgnoreCase(username) && admin.getPasswordHash().equals(hashedInput)) {
                return admin; // Match found
            }
        }
        return null; // No match found
    }

    public boolean deleteByUserId(String userId, String actor) {
        List<Admin> admins = getAllAdmins();
        boolean removed = admins.removeIf(admin -> admin.getUserId().equals(userId));

        if (!removed) {
            return false;
        }

        overwriteAdmins(admins);
        logActivity(actor, "DELETE", "Deleted account userId: " + userId);
        return true;
    }

    public boolean updateByUserId(String userId, String fullName, String username, String role, String employeeType, String rawPassword, String actor) {
        List<Admin> admins = getAllAdmins();
        boolean isUpdated = false;

        for (Admin admin : admins) {
            if (!admin.getUserId().equals(userId) && admin.getUsername().equalsIgnoreCase(username)) {
                return false;
            }
        }

        for (int i = 0; i < admins.size(); i++) {
            Admin admin = admins.get(i);
            if (admin.getUserId().equals(userId)) {
                String passwordHash = rawPassword == null || rawPassword.trim().isEmpty()
                        ? admin.getPasswordHash()
                        : hashPassword(rawPassword);

                Admin updatedAdmin = new Admin(
                        admin.getUserId(),
                        sanitize(fullName),
                        sanitize(username),
                        sanitize(role),
                        sanitize(employeeType),
                        passwordHash
                );
                admins.set(i, updatedAdmin);
                isUpdated = true;
                break;
            }
        }

        if (!isUpdated) {
            return false;
        }

        overwriteAdmins(admins);
        logActivity(actor, "UPDATE", "Updated account userId: " + userId);
        return true;
    }

    public List<ActivityLogEntry> getAllActivityLogs() {
        List<ActivityLogEntry> logs = new ArrayList<>();
        File file = new File(ACTIVITY_FILE);

        if (!file.exists()) {
            return logs;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|", 4);
                if (parts.length == 4) {
                    logs.add(new ActivityLogEntry(parts[0], parts[1], parts[2], parts[3]));
                }
            }
        } catch (IOException ignored) {
        }
        return logs;
    }

    private void overwriteAdmins(List<Admin> admins) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ADMIN_FILE, false))) {
            for (Admin admin : admins) {
                writer.write(admin.toFileRecord());
                writer.newLine();
            }
        } catch (IOException ignored) {
        }
    }

    public void logActivity(String actor, String action, String details) {
        String row = now() + "|" + sanitize(actor) + "|" + sanitize(action) + "|" + sanitize(details);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ACTIVITY_FILE, true))) {
            writer.write(row);
            writer.newLine();
        } catch (IOException ignored) {
        }
    }

    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", " ").replace("|", " ").trim();
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : encodedHash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return "invalid";
        }
    }
}
