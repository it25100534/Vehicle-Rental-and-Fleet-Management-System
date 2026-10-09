package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.fleet.Branch;
import com.example.vehiclerentalserviceplatform.fleet.BranchService;
import com.example.vehiclerentalserviceplatform.maintenance.MaintenanceFileHandler;
import com.example.vehiclerentalserviceplatform.maintenance.MaintenanceRecord;
import com.example.vehiclerentalserviceplatform.model.*;
import com.example.vehiclerentalserviceplatform.security.InputValidation;
import com.example.vehiclerentalserviceplatform.service.BookingService;
import com.example.vehiclerentalserviceplatform.service.VehicleService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CatalogBrowseService {
    public record CatalogPage(List<Vehicle> vehicles, int total, int offset, int limit,
                              Map<String, List<String>> facets) {}
    public record HomeFleet(List<Vehicle> vehicles, Map<String, Long> categoryCounts, List<String> brands) {}

    private final VehicleService vehicles;
    private final BookingService bookings;
    private final BranchService branches;
    private final MaintenanceFileHandler maintenance;
    private final Map<String, Map<String, Object>> metadata;

    public CatalogBrowseService(VehicleService vehicles, BookingService bookings, BranchService branches,
                                MaintenanceFileHandler maintenance, ObjectMapper mapper) throws IOException {
        this.vehicles = vehicles;
        this.bookings = bookings;
        this.branches = branches;
        this.maintenance = maintenance;
        try (var stream = new ClassPathResource("static/data/catalog-metadata.json").getInputStream()) {
            metadata = mapper.readValue(stream, new TypeReference<>() {});
        }
    }

    public CatalogPage browse(Map<String, String> query) {
        int offset = Math.max(0, number(query.get("offset"), 0));
        int limit = Math.max(0, Math.min(24, number(query.get("limit"), 12)));
        String startDate = query.getOrDefault("startDate", "");
        String returnDate = query.getOrDefault("returnDate", "");
        if (startDate.isBlank() != returnDate.isBlank()) throw new IllegalArgumentException("Select both rental dates.");
        LocalDate start = null, end = null;
        if (!startDate.isBlank()) {
            InputValidation.requireBookingDates(startDate, returnDate);
            start = LocalDate.parse(startDate);
            end = LocalDate.parse(returnDate);
        }

        Set<String> openBranches = branches.getOpenBranches().stream().map(Branch::getBranchId).collect(Collectors.toSet());
        List<Vehicle> fleet = vehicles.getAllVehicles().stream()
                .filter(v -> !"RETIRED".equalsIgnoreCase(v.getOperationalStatus()))
                .filter(v -> openBranches.contains(v.getCurrentBranch()))
                .sorted(Comparator.comparing(Vehicle::getVehicleId))
                .toList();
        Map<String, List<String>> facets = facets(fleet);
        Set<String> unavailable = start == null ? Set.of() : unavailable(start, end);
        Map<String, String> current = start == null ? currentBookingStatuses() : Map.of();
        Set<String> dueMaintenance = start == null ? dueMaintenance() : Set.of();
        Map<String, Vehicle> models = new LinkedHashMap<>();
        for (Vehicle vehicle : fleet) {
            if (start != null && (!vehicle.isAvailable() || unavailable.contains(vehicle.getVehicleId()))) continue;
            if (!matches(vehicle, query)) continue;
            String id = modelId(vehicle);
            Vehicle previous = models.get(id);
            String status = start == null ? status(vehicle, current, dueMaintenance) : "AVAILABLE";
            vehicle.setOperationalStatus(status);
            if (previous == null || (!"AVAILABLE".equals(previous.getOperationalStatus()) && "AVAILABLE".equals(status)))
                models.put(id, vehicle);
        }
        List<Vehicle> matches = new ArrayList<>(models.values());
        return new CatalogPage(matches.subList(Math.min(offset, matches.size()),
                Math.min(offset + limit, matches.size())), matches.size(), offset, limit, facets);
    }

    public HomeFleet homeFleet() {
        Set<String> openBranches = branches.getOpenBranches().stream().map(Branch::getBranchId).collect(Collectors.toSet());
        List<Vehicle> fleet = vehicles.getAllVehicles().stream()
                .filter(v -> !"RETIRED".equalsIgnoreCase(v.getOperationalStatus()))
                .filter(v -> openBranches.contains(v.getCurrentBranch())).toList();
        Map<String, String> current = currentBookingStatuses();
        Set<String> due = dueMaintenance();
        List<String> curated = List.of("CAR-0001", "SUV-0004", "VAN-0004", "PRE-0001", "MOT-0004");
        List<Vehicle> picks = fleet.stream().filter(v -> "AVAILABLE".equals(status(v, current, due)))
                .sorted(Comparator.comparingInt((Vehicle v) -> {
                    int index = curated.indexOf(v.getVehicleId());
                    return index < 0 ? 100 : index;
                }).thenComparing(Vehicle::getVehicleId))
                .limit(4).toList();
        picks.forEach(v -> v.setOperationalStatus("AVAILABLE"));
        Map<String, Long> categories = fleet.stream().collect(Collectors.groupingBy(Vehicle::getUsageCategory, Collectors.counting()));
        List<String> brands = fleet.stream().map(Vehicle::getMake).distinct().sorted().toList();
        return new HomeFleet(picks, categories, brands);
    }

    private boolean matches(Vehicle v, Map<String, String> q) {
        String search = q.getOrDefault("search", "").strip().toLowerCase(Locale.ROOT);
        if (!search.isEmpty() && !(v.getMake() + " " + v.getModel()).toLowerCase(Locale.ROOT).contains(search)) return false;
        Map<String, Object> meta = metadata.getOrDefault(modelId(v), Map.of());
        String type = String.valueOf(meta.getOrDefault("displayCategory", v.getRentalRate() >= 25000 ? "PREMIUM" : v.getType()));
        String use = String.valueOf(meta.getOrDefault("useCategory", v.getUsageCategory()));
        String style = String.valueOf(meta.getOrDefault("body", defaultStyle(v)));
        String transmission = String.valueOf(meta.getOrDefault("transmission", defaultTransmission(v, style)));
        String fuel = v.getFuelType();
        String seats = Integer.toString(seats(v));
        double rate = v.getRentalRate();
        if (!choice(q, "types", type) || !choice(q, "uses", use) || !choice(q, "styles", style)
                || !choice(q, "transmissions", transmission) || !choice(q, "fuels", fuel)
                || !choice(q, "seats", seats) || !choice(q, "branches", v.getCurrentBranch())) return false;
        if (q.containsKey("minPrice") && rate < number(q.get("minPrice"), 0)) return false;
        if (q.containsKey("maxPrice") && rate > number(q.get("maxPrice"), Integer.MAX_VALUE)) return false;
        return !"true".equals(q.get("topOnly")) || Boolean.TRUE.equals(meta.getOrDefault("topChoice", rate >= 10000));
    }

    private Map<String, List<String>> facets(List<Vehicle> fleet) {
        Set<String> styles = new TreeSet<>(), transmissions = new TreeSet<>(), fuels = new TreeSet<>(), seats = new TreeSet<>(Comparator.comparingInt(Integer::parseInt));
        for (Vehicle v : fleet) {
            Map<String, Object> meta = metadata.getOrDefault(modelId(v), Map.of());
            String style = String.valueOf(meta.getOrDefault("body", defaultStyle(v)));
            styles.add(style);
            transmissions.add(String.valueOf(meta.getOrDefault("transmission", defaultTransmission(v, style))));
            fuels.add(v.getFuelType());
            seats.add(Integer.toString(seats(v)));
        }
        return Map.of("styles", List.copyOf(styles), "transmissions", List.copyOf(transmissions),
                "fuels", List.copyOf(fuels), "seats", List.copyOf(seats));
    }

    private Set<String> unavailable(LocalDate start, LocalDate end) {
        Set<String> blocked = new HashSet<>();
        for (Booking booking : bookings.getAllBookings()) {
            if (blocks(booking.getBookingStatus()) && overlaps(start, end,
                    LocalDate.parse(booking.getStartDate()), LocalDate.parse(booking.getReturnDate())))
                blocked.add(booking.getVehicleId());
        }
        for (MaintenanceRecord record : maintenance.loadAllRecords()) {
            if (record.isActive() && overlaps(start, end, LocalDate.parse(record.getServiceDate()),
                    LocalDate.parse(record.getExpectedCompletionDate()))) blocked.add(record.getVehicleId());
        }
        return blocked;
    }

    private Map<String, String> currentBookingStatuses() {
        Map<String, String> statuses = new HashMap<>();
        LocalDate today = LocalDate.now();
        for (Booking booking : bookings.getAllBookings()) {
            if (!blocks(booking.getBookingStatus()) || !overlaps(today, today,
                    LocalDate.parse(booking.getStartDate()), LocalDate.parse(booking.getReturnDate()))) continue;
            statuses.put(booking.getVehicleId(), "Active".equalsIgnoreCase(booking.getBookingStatus()) ? "RENTED" :
                    statuses.getOrDefault(booking.getVehicleId(), "BOOKED"));
        }
        return statuses;
    }

    private Set<String> dueMaintenance() {
        return maintenance.loadAllRecords().stream()
                .filter(r -> "In Progress".equalsIgnoreCase(r.getStatus()) || r.isDue())
                .map(MaintenanceRecord::getVehicleId).collect(Collectors.toSet());
    }

    private String status(Vehicle v, Map<String, String> bookings, Set<String> maintenance) {
        if (maintenance.contains(v.getVehicleId())) return "MAINTENANCE";
        return bookings.getOrDefault(v.getVehicleId(), v.isAvailable() ? "AVAILABLE" : "RENTED");
    }

    private boolean blocks(String status) {
        return status != null && !List.of("Cancelled", "Rejected", "Returned", "Completed").stream()
                .anyMatch(value -> value.equalsIgnoreCase(status));
    }

    private boolean overlaps(LocalDate start, LocalDate end, LocalDate otherStart, LocalDate otherEnd) {
        return !start.isAfter(otherEnd) && !end.isBefore(otherStart);
    }

    private boolean choice(Map<String, String> query, String key, String value) {
        String selected = query.getOrDefault(key, "");
        return selected.isBlank() || Arrays.stream(selected.split(",")).anyMatch(item -> item.equalsIgnoreCase(value));
    }

    private int number(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ex) { return fallback; }
    }

    private String modelId(Vehicle v) { return v.getVehicleId().replaceFirst("-U[23]$", ""); }
    private String defaultStyle(Vehicle v) {
        if (v instanceof Car c && c.getCarType() != null) return c.getCarType();
        if (v instanceof Motorcycle m) return m.getMotorcycleType();
        if (v instanceof Suv s) return s.getDriveTrain() + " SUV";
        return v instanceof Van ? "Passenger van" : "Car";
    }
    private String defaultTransmission(Vehicle v, String style) {
        return v instanceof Motorcycle && !"Scooter".equalsIgnoreCase(style) ? "Manual" : "Automatic";
    }
    private int seats(Vehicle v) {
        if (v instanceof Car c) return c.getNumberOfSeats();
        if (v instanceof Suv s) return s.getNumberOfSeats();
        if (v instanceof Van van) return van.getNumberOfSeats();
        return 2;
    }
}
