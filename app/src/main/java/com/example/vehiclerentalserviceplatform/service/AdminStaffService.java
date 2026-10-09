package com.example.vehiclerentalserviceplatform.service;

import com.example.vehiclerentalserviceplatform.model.ActivityLogEntry;
import com.example.vehiclerentalserviceplatform.model.Admin;
import com.example.vehiclerentalserviceplatform.persistence.StaffAccountEntity;
import com.example.vehiclerentalserviceplatform.persistence.StaffAccountRepository;
import com.example.vehiclerentalserviceplatform.security.InputValidation;
import com.example.vehiclerentalserviceplatform.security.PasswordHash;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AdminStaffService {
    private static final String ADMIN_FILE = "admins.txt";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final StaffAccountRepository repository;
    private final ActivityLogService activityLogService;

    public AdminStaffService(StaffAccountRepository repository, ActivityLogService activityLogService) {
        this.repository = repository;
        this.activityLogService = activityLogService;
        importLegacyStaffWhenEmpty();
    }

    public String register(String fullName, String username, String role, String employeeType, String rawPassword, String actor) {
        return register(fullName, username, role, employeeType, rawPassword, null, actor);
    }
    public String register(String fullName, String username, String role, String employeeType, String rawPassword, String homeBranch, String actor) {
        try {
            InputValidation.requireAdminAccount(fullName, username, role, employeeType, rawPassword, true);
        } catch (IllegalArgumentException ex) {
            return ex.getMessage();
        }
        if (repository.existsByUsernameIgnoreCase(username.trim())) return "Username already exists.";
        StaffAccountEntity entity = new StaffAccountEntity(UUID.randomUUID().toString(), sanitize(fullName),
                sanitize(username), PasswordHash.encode(rawPassword), sanitize(role).toUpperCase(),
                sanitize(employeeType), true);
        entity.setHomeBranchId(homeBranch == null || homeBranch.isBlank() ? null : homeBranch.trim().toUpperCase());
        repository.save(entity);
        logActivity(actor, "CREATE", "Created " + role + " account for username: " + username);
        return "Registration successful.";
    }

    public List<Admin> getAllAdmins() {
        return repository.findAllByActiveTrueOrderByFullNameAsc().stream().map(this::toAdmin).toList();
    }

    public Admin authenticate(String username, String rawPassword) {
        if (username == null || rawPassword == null) return null;
        return repository.findByUsernameIgnoreCaseAndActiveTrue(username.trim())
                .filter(account -> passwordMatches(rawPassword, account.getPasswordHash()))
                .map(account -> {
                    if (!PasswordHash.isEncoded(account.getPasswordHash())) {
                        account.setPasswordHash(PasswordHash.encode(rawPassword));
                        repository.save(account);
                    }
                    return toAdmin(account);
                }).orElse(null);
    }

    public boolean deleteByUserId(String userId, String actor) {
        StaffAccountEntity account = repository.findById(userId).orElse(null);
        if (account == null || !account.isActive()) return false;
        account.setActive(false);
        repository.save(account);
        logActivity(actor, "DEACTIVATE", "Deactivated account userId: " + userId);
        return true;
    }

    public boolean updateByUserId(String userId, String fullName, String username, String role,
                                  String employeeType, String rawPassword, String actor) {
        try {
            InputValidation.requireAdminAccount(fullName, username, role, employeeType, rawPassword, false);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        StaffAccountEntity account = repository.findById(userId).orElse(null);
        if (account == null || !account.isActive()) return false;
        boolean duplicate = repository.findAll().stream().anyMatch(other ->
                !other.getUserId().equals(userId) && other.getUsername().equalsIgnoreCase(username.trim()));
        if (duplicate) return false;
        account.setFullName(sanitize(fullName));
        account.setUsername(sanitize(username));
        account.setRole(sanitize(role).toUpperCase());
        account.setEmployeeType(sanitize(employeeType));
        if (rawPassword != null && !rawPassword.trim().isEmpty()) account.setPasswordHash(PasswordHash.encode(rawPassword));
        repository.save(account);
        logActivity(actor, "UPDATE", "Updated account userId: " + userId);
        return true;
    }

    public List<ActivityLogEntry> getAllActivityLogs() {
        return activityLogService.getAll();
    }

    public void logActivity(String actor, String action, String details) {
        activityLogService.log("ADMIN", actor, action, null, details);
    }

    private void importLegacyStaffWhenEmpty() {
        File file = new File(ADMIN_FILE);
        if (repository.count() != 0 || !file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length == 6) repository.save(new StaffAccountEntity(
                        parts[0], parts[1], parts[2], parts[5], parts[3].toUpperCase(), parts[4], true));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not import staff accounts.", ex);
        }
    }

    private Admin toAdmin(StaffAccountEntity entity) {
        Admin admin = new Admin(entity.getUserId(), entity.getFullName(), entity.getUsername(), entity.getRole(),
                entity.getEmployeeType(), entity.getPasswordHash());
        admin.setHomeBranchId(entity.getHomeBranchId());
        return admin;
    }
    private String now() { return LocalDateTime.now().format(FORMATTER); }
    private String sanitize(String value) {
        return value == null ? "" : value.replace(",", " ").replace("|", " ").trim();
    }
    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : encodedHash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 is unavailable.", e); }
    }

    private boolean passwordMatches(String rawPassword, String stored) {
        return PasswordHash.matches(rawPassword, stored)
                || (stored != null && stored.equalsIgnoreCase(hashPassword(rawPassword)));
    }
}
