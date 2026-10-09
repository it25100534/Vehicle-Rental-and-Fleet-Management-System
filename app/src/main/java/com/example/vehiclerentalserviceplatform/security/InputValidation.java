package com.example.vehiclerentalserviceplatform.security;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;

public final class InputValidation {

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,30}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^0\\d{9}$");
    private static final Pattern LICENSE = Pattern.compile("^(?=.{4,25}$)[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$");
    private static final Pattern PASSWORD = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{6,64}$");
    private static final Set<String> VEHICLE_TYPES = Set.of("CAR", "SUV", "VAN", "MOTORCYCLE");
    private static final Set<String> FUELS = Set.of("Petrol", "Diesel");
    private static final Set<String> MAINTENANCE_TYPES = Set.of("Engine", "Tyre", "Oil Change", "Brake", "General");
    private static final Set<String> MAINTENANCE_STATUSES = Set.of("Pending", "In Progress", "Completed", "Cancelled");
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "STAFF");
    private static final Set<String> EMPLOYEE_TYPES = Set.of("FULL_TIME", "PART_TIME", "CONTRACT");
    private static final Set<String> FUEL_LEVELS = Set.of("FULL", "THREE_QUARTERS", "HALF", "QUARTER", "EMPTY");

    private InputValidation() {
    }

    public static boolean isUsername(String value) {
        return value != null && USERNAME.matcher(value.trim()).matches();
    }

    public static boolean isEmail(String value) {
        return value != null && EMAIL.matcher(value.trim()).matches();
    }

    public static boolean isPhone(String value) {
        return value != null && PHONE.matcher(value.trim()).matches();
    }

    public static boolean isLicenseId(String value) {
        return value != null && LICENSE.matcher(value.trim()).matches();
    }

    public static boolean isPassword(String value) {
        return value != null && PASSWORD.matcher(value).matches();
    }

    public static void requireBookingDates(String startValue, String returnValue) {
        LocalDate start = parseDate(startValue, "Start date");
        LocalDate end = parseDate(returnValue, "Return date");
        if (start.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Return date must be after the start date.");
        }
    }

    public static void requireVehicle(String type, String make, String model, String year,
                                      String fuel, double rate, double mileage, Integer seats,
                                      String driveTrain, String motorcycleType) {
        requireChoice(type, VEHICLE_TYPES, "Vehicle type");
        requireSafeText(make, "Make", 2, 50);
        requireSafeText(model, "Model", 1, 50);
        requireChoice(fuel, FUELS, "Fuel type");

        int numericYear;
        try {
            numericYear = Integer.parseInt(year);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Manufacture year must be a four-digit number.");
        }
        int newestYear = LocalDate.now().getYear() + 1;
        if (numericYear < 1900 || numericYear > newestYear) {
            throw new IllegalArgumentException("Manufacture year must be between 1900 and " + newestYear + ".");
        }
        if (!Double.isFinite(rate) || rate <= 0 || rate > 10_000_000) {
            throw new IllegalArgumentException("Daily rate must be greater than zero and within the supported range.");
        }
        if (!Double.isFinite(mileage) || mileage < 0 || mileage > 10_000_000) {
            throw new IllegalArgumentException("Mileage must be zero or greater and within the supported range.");
        }

        if (!"MOTORCYCLE".equals(type) && (seats == null || seats < 1 || seats > 100)) {
            throw new IllegalArgumentException("Seat count must be between 1 and 100.");
        }
        if (("SUV".equals(type) || "VAN".equals(type))) {
            requireSafeText(driveTrain, "Drive train", 2, 30);
        }
        if ("MOTORCYCLE".equals(type)) {
            requireSafeText(motorcycleType, "Motorcycle type", 2, 30);
        }
    }

    public static void requireVehicleAssignment(String category, String branchId) {
        String categoryCode = category == null ? "" : category.trim().toUpperCase();
        if (!categoryCode.matches("[A-Z][A-Z0-9_-]{2,19}")) {
            throw new IllegalArgumentException("Vehicle category code must contain 3 to 20 letters, numbers, underscores, or hyphens.");
        }
        String branch = branchId == null ? "" : branchId.trim().toUpperCase();
        if (!branch.matches("BR-\\d{3}")) {
            throw new IllegalArgumentException("Branch ID must use the format BR-001.");
        }
    }

    public static void requireMaintenance(String serviceType, String serviceDate, String notes) {
        requireChoice(serviceType, MAINTENANCE_TYPES, "Service type");
        parseDate(serviceDate, "Service date");
        if (notes != null && !notes.isBlank()) {
            requireSafeText(notes, "Notes", 1, 250);
        }
    }

    public static void requireMaintenanceDetails(String serviceType, String serviceDate,
                                                 String expectedCompletionDate, String provider,
                                                 double estimatedCost, String notes) {
        requireMaintenance(serviceType, serviceDate, notes);
        LocalDate start = parseDate(serviceDate, "Service date");
        LocalDate end = parseDate(expectedCompletionDate, "Expected completion date");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Expected completion date cannot be before the service date.");
        }
        requireSafeText(provider, "Service provider", 2, 80);
        if (!Double.isFinite(estimatedCost) || estimatedCost < 0 || estimatedCost > 10_000_000) {
            throw new IllegalArgumentException("Estimated cost must be between 0 and 10,000,000.");
        }
    }

    public static boolean isMaintenanceStatus(String status) {
        return MAINTENANCE_STATUSES.contains(status);
    }

    public static boolean isForwardMaintenanceTransition(String current, String next) {
        if (!isMaintenanceStatus(current) || !isMaintenanceStatus(next)) return false;
        if (current.equals(next)) return true;
        if ("Completed".equals(current) || "Cancelled".equals(current)) return false;
        return "In Progress".equals(next) || "Completed".equals(next) || "Cancelled".equals(next);
    }

    public static void requireAdminAccount(String fullName, String username, String role,
                                           String employeeType, String password, boolean passwordRequired) {
        requireSafeText(fullName, "Full name", 2, 80);
        if (!isUsername(username)) {
            throw new IllegalArgumentException("Username must be 3 to 30 letters, numbers, dots, underscores, or hyphens.");
        }
        requireChoice(role, ADMIN_ROLES, "Role");
        requireChoice(employeeType, EMPLOYEE_TYPES, "Employee type");
        if (passwordRequired || (password != null && !password.isBlank())) {
            if (!isPassword(password)) {
                throw new IllegalArgumentException("Password must be 6 to 64 characters and contain a letter and a number.");
            }
        }
    }

    public static void requireHandover(LocalDate scheduledReturnDate, double odometer,
                                       String fuelLevel, String condition) {
        if (scheduledReturnDate == null) {
            throw new IllegalArgumentException("Scheduled return date is required.");
        }
        requireOdometer(odometer, "Odometer");
        requireChoice(fuelLevel, FUEL_LEVELS, "Fuel level");
        requireSafeText(condition, "Vehicle condition", 1, 180);
    }

    public static void requireReturn(double odometer, String fuelLevel, String condition, double lateFee) {
        requireOdometer(odometer, "Return odometer");
        requireChoice(fuelLevel, FUEL_LEVELS, "Return fuel level");
        requireSafeText(condition, "Return condition", 1, 180);
        if (!Double.isFinite(lateFee) || lateFee < 0 || lateFee > 10_000_000) {
            throw new IllegalArgumentException("Late fee must be zero or greater and within the supported range.");
        }
    }

    public static void requireSafeText(String value, String field, int minLength, int maxLength) {
        String clean = value == null ? "" : value.trim();
        if (clean.length() < minLength || clean.length() > maxLength) {
            throw new IllegalArgumentException(field + " must contain " + minLength + " to " + maxLength + " characters.");
        }
        if (clean.contains(",") || clean.contains("|") || clean.contains("\n") || clean.contains("\r")) {
            throw new IllegalArgumentException(field + " contains unsupported characters.");
        }
    }

    private static void requireOdometer(double value, String field) {
        if (!Double.isFinite(value) || value < 0 || value > 10_000_000) {
            throw new IllegalArgumentException(field + " must be zero or greater and within the supported range.");
        }
    }

    private static LocalDate parseDate(String value, String field) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new IllegalArgumentException(field + " must be a valid date.");
        }
    }

    private static void requireChoice(String value, Set<String> allowed, String field) {
        if (!allowed.contains(value)) {
            throw new IllegalArgumentException(field + " is invalid.");
        }
    }

    private static int maintenanceRank(String status) {
        return switch (status) {
            case "Pending" -> 0;
            case "In Progress" -> 1;
            case "Completed" -> 2;
            case "Cancelled" -> 2;
            default -> -1;
        };
    }
}
