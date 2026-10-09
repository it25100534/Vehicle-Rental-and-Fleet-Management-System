package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.model.*;
import com.example.vehiclerentalserviceplatform.service.VehicleService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import com.example.vehiclerentalserviceplatform.security.InputValidation;
import com.example.vehiclerentalserviceplatform.service.BookingService;
import com.example.vehiclerentalserviceplatform.fleet.BranchService;
import com.example.vehiclerentalserviceplatform.fleet.VehicleCategoryService;
import com.example.vehiclerentalserviceplatform.maintenance.MaintenanceFileHandler;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleApiController {

    private final VehicleService vehicleService;
    private final BookingService bookingService;
    private final BranchService branchService;
    private final VehicleCategoryService categoryService;
    private final MaintenanceFileHandler maintenance;
    private final CatalogBrowseService catalogBrowse;
    private final String UPLOAD_DIR = "src/main/resources/static/images/";

    public VehicleApiController(VehicleService vehicleService, BookingService bookingService,
                                BranchService branchService, VehicleCategoryService categoryService,
                                MaintenanceFileHandler maintenance, CatalogBrowseService catalogBrowse) {
        this.vehicleService = vehicleService;
        this.bookingService = bookingService;
        this.branchService = branchService;
        this.categoryService = categoryService;
        this.maintenance = maintenance;
        this.catalogBrowse = catalogBrowse;
    }

    @GetMapping
    public List<Vehicle> getAll() {
        List<Vehicle> vehicles = vehicleService.getAllVehicles();
        vehicles.forEach(this::applyOperationalStatus);
        return vehicles;
    }

    @GetMapping("/catalog-page")
    public ResponseEntity<CatalogBrowseService.CatalogPage> catalogPage(@RequestParam Map<String, String> filters) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(catalogBrowse.browse(filters));
    }

    @GetMapping("/home-featured")
    public ResponseEntity<CatalogBrowseService.HomeFleet> homeFeatured() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(catalogBrowse.homeFleet());
    }

    @GetMapping("/inventory-page")
    public ResponseEntity<Map<String, Object>> inventoryPage(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "20") int size,
                                                               @RequestParam(defaultValue = "") String query) {
        Page<Vehicle> result = vehicleService.getInventoryPage(query, page, size);
        result.getContent().forEach(this::applyOperationalStatus);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "vehicles", result.getContent(), "total", result.getTotalElements(),
                "page", result.getNumber(), "size", result.getSize()));
    }

    @GetMapping("/{id}")
    public Vehicle getOne(@PathVariable String id) {
        Vehicle vehicle = vehicleService.getVehicleById(id);
        if (vehicle != null) applyOperationalStatus(vehicle);
        return vehicle;
    }

    @GetMapping("/availability")
    public List<Vehicle> getAvailable(@RequestParam String startDate,
                                      @RequestParam String returnDate,
                                      @RequestParam(required = false) String branch,
                                      @RequestParam(required = false) String category) {
        InputValidation.requireBookingDates(startDate, returnDate);
        return vehicleService.getAllVehicles().stream()
                .filter(Vehicle::isAvailable)
                .filter(v -> branchService.isOpen(v.getCurrentBranch()))
                .filter(v -> branch == null || branch.isBlank() || v.getCurrentBranch().equalsIgnoreCase(branch))
                .filter(v -> category == null || category.isBlank() || v.getUsageCategory().equalsIgnoreCase(category))
                .filter(v -> bookingService.isVehicleAvailable(v.getVehicleId(), startDate, returnDate))
                .toList();
    }

    @PostMapping("/add")
    public String addVehicle(
            @RequestParam("type") String type,
            @RequestParam("make") String make,
            @RequestParam("model") String model,
            @RequestParam("year") String year,
            @RequestParam("fuel") String fuel,
            @RequestParam("rate") double rate,
            @RequestParam("mileage") double mileage,
            @RequestParam("category") String category,
            @RequestParam("branch") String branch,
            @RequestParam(value = "seats", required = false) Integer seats,
            @RequestParam(value = "driveTrain", required = false) String driveTrain,
            @RequestParam(value = "motoType", required = false) String motoType,
            @RequestParam("image") MultipartFile imageFile) {

        type = normalizeType(type);
        fuel = normalizeFuel(fuel);
        InputValidation.requireVehicle(type, make, model, year, fuel, rate, mileage,
                seats, driveTrain, motoType);
        requireAssignment(category, branch);

        try {
            String vehicleId = vehicleService.generateNextId(type);

            String fileName = saveImage(imageFile, vehicleId);

            Vehicle v;
            if (type.equals("CAR")) {
                v = new Car(make, model, year, rate, fuel, mileage, true, seats, fileName, vehicleId);
            } else if (type.equals("MOTORCYCLE")) {
                v = new Motorcycle(make, model, year, rate, fuel, mileage, true, motoType, fileName, vehicleId);
            } else if (type.equals("SUV")) {
                v = new Suv(make, model, year, rate, fuel, mileage, true, seats, driveTrain, fileName, vehicleId);
            } else {
                v = new Van(make, model, year, rate, fuel, mileage, true, seats, driveTrain, fileName, vehicleId);
            }

            v.setUsageCategory(category);
            v.setCurrentBranch(branch);

            vehicleService.addVehicle(v);
            return "Vehicle and image uploaded successfully! ID: " + vehicleId;

        } catch (IOException e) {
            return "Error uploading image: " + e.getMessage();
        }
    }

    @PostMapping("/update")
    public String updateVehicle(
            @RequestParam("id") String id,
            @RequestParam("type") String type,
            @RequestParam("make") String make,
            @RequestParam("model") String model,
            @RequestParam("year") String year,
            @RequestParam("fuel") String fuel,
            @RequestParam("rate") double rate,
            @RequestParam("mileage") double mileage,
            @RequestParam("category") String category,
            @RequestParam("branch") String branch,
            @RequestParam(value = "seats", required = false) Integer seats,
            @RequestParam(value = "driveTrain", required = false) String driveTrain,
            @RequestParam(value = "motoType", required = false) String motoType,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {

        type = normalizeType(type);
        fuel = normalizeFuel(fuel);
        InputValidation.requireVehicle(type, make, model, year, fuel, rate, mileage,
                seats, driveTrain, motoType);
        requireAssignment(category, branch);

        try {
            Vehicle existingVehicle = vehicleService.getVehicleById(id);
            if (existingVehicle == null) {
                return "Error: Vehicle not found!";
            }
            if (!existingVehicle.getType().equals(type)) {
                throw new IllegalArgumentException("Vehicle type cannot be changed during an update.");
            }
            if ("RETIRED".equalsIgnoreCase(existingVehicle.getOperationalStatus())) {
                throw new IllegalArgumentException("A retired vehicle cannot be edited.");
            }
            boolean branchChanged = !existingVehicle.getCurrentBranch().equalsIgnoreCase(branch);
            if (branchChanged && maintenance.isCurrentlyUnavailableForMaintenance(id)) {
                throw new IllegalArgumentException("A vehicle under maintenance cannot be moved to another branch.");
            }
            if (branchChanged && bookingService.getCurrentVehicleBookingStatus(id) != null) {
                throw new IllegalArgumentException("A currently booked or rented vehicle cannot be moved to another branch.");
            }

            String fileName = existingVehicle.getVehicleImageFileName();

            if (imageFile != null && !imageFile.isEmpty()) {
                fileName = saveImage(imageFile, id);
            }

            boolean isAvailable = existingVehicle.isAvailable();

            Vehicle v;
            if (type.equals("CAR")) {
                v = new Car(make, model, year, rate, fuel, mileage, isAvailable, seats, fileName, id);
            } else if (type.equals("MOTORCYCLE")) {
                v = new Motorcycle(make, model, year, rate, fuel, mileage, isAvailable, motoType, fileName, id);
            } else if (type.equals("SUV")) {
                v = new Suv(make, model, year, rate, fuel, mileage, isAvailable, seats, driveTrain, fileName, id);
            } else {
                v = new Van(make, model, year, rate, fuel, mileage, isAvailable, seats, driveTrain, fileName, id);
            }

            v.setUsageCategory(category);
            v.setCurrentBranch(branch);

            boolean success = vehicleService.updateVehicle(v);

            if (success) {
                return "Vehicle updated successfully!";
            } else {
                return "Error: Could not update vehicle database.";
            }

        } catch (IOException e) {
            return "Error uploading new image: " + e.getMessage();
        }
    }

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable String id) {
        Vehicle vehicle = vehicleService.getVehicleById(id);
        if (vehicle == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle was not found.");
        if ("RETIRED".equalsIgnoreCase(vehicle.getOperationalStatus())) {
            throw new IllegalArgumentException("This vehicle is already retired.");
        }
        if (maintenance.isCurrentlyUnavailableForMaintenance(id)) {
            throw new IllegalArgumentException("Finish or cancel maintenance before retiring this vehicle.");
        }
        if (bookingService.getCurrentVehicleBookingStatus(id) != null) {
            throw new IllegalArgumentException("A currently booked or rented vehicle cannot be retired.");
        }
        if (!vehicleService.deleteVehicle(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle was not found.");
        }
        return "Vehicle retired successfully.";
    }

    @PostMapping("/{id}/relocate")
    public String relocate(@PathVariable String id, @RequestParam String branch) {
        Vehicle vehicle = vehicleService.getVehicleById(id);
        if (vehicle == null) throw new IllegalArgumentException("Vehicle was not found.");
        if (!branchService.isOpen(branch)) throw new IllegalArgumentException("Select an open destination branch.");
        if (maintenance.isCurrentlyUnavailableForMaintenance(id)) {
            throw new IllegalArgumentException("A vehicle under maintenance cannot be relocated.");
        }
        if (bookingService.getCurrentVehicleBookingStatus(id) != null) {
            throw new IllegalArgumentException("A currently booked or rented vehicle cannot be relocated.");
        }
        if (!vehicleService.relocateVehicle(id, branch)) {
            throw new IllegalArgumentException("Only currently available vehicles can be relocated.");
        }
        return "Vehicle relocated to " + branch.toUpperCase() + ".";
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toUpperCase();
    }

    private String normalizeFuel(String fuel) {
        if (fuel == null) return "";
        if (fuel.equalsIgnoreCase("petrol")) return "Petrol";
        if (fuel.equalsIgnoreCase("diesel")) return "Diesel";
        return fuel.trim();
    }

    private void requireAssignment(String category, String branch) {
        category = category == null ? "" : category.trim().toUpperCase();
        branch = branch == null ? "" : branch.trim().toUpperCase();
        InputValidation.requireVehicleAssignment(category, branch);
        if (!categoryService.isActive(category)) throw new IllegalArgumentException("Select an active vehicle category.");
        if (!branchService.isOpen(branch)) throw new IllegalArgumentException("Select an open branch.");
    }

    private void applyOperationalStatus(Vehicle vehicle) {
        String status;
        if ("RETIRED".equalsIgnoreCase(vehicle.getOperationalStatus())) {
            status = "RETIRED";
        } else if (!branchService.isOpen(vehicle.getCurrentBranch())) {
            status = "BRANCH CLOSED";
        } else if (maintenance.isCurrentlyUnavailableForMaintenance(vehicle.getVehicleId())) {
            status = "MAINTENANCE";
        } else {
            String bookingStatus = bookingService.getCurrentVehicleBookingStatus(vehicle.getVehicleId());
            if (bookingStatus != null) status = bookingStatus;
            else status = vehicle.isAvailable() ? "AVAILABLE" : "RENTED";
        }
        vehicle.setOperationalStatus(status);
    }

    private String saveImage(MultipartFile image, String vehicleId) throws IOException {
        if (image == null || image.isEmpty()) return "vehicle-placeholder.svg";
        if (image.getSize() > 5L * 1024 * 1024) throw new IllegalArgumentException("Vehicle images must be 5 MB or smaller.");
        String type = image.getContentType() == null ? "" : image.getContentType().toLowerCase();
        if (!List.of("image/jpeg","image/png","image/webp").contains(type))
            throw new IllegalArgumentException("Use a JPEG, PNG, or WebP vehicle image.");
        byte[] bytes = image.getBytes();
        String hash;
        try { hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception ex) { throw new IOException("Could not validate image.", ex); }
        Path dir = Paths.get(UPLOAD_DIR); Files.createDirectories(dir);
        try (var files = Files.list(dir)) {
            if (files.filter(Files::isRegularFile).anyMatch(path -> sameHash(path, hash)))
                throw new IllegalArgumentException("This image is already used in the vehicle inventory.");
        }
        String ext = type.equals("image/png") ? ".png" : type.equals("image/webp") ? ".webp" : ".jpg";
        String fileName = vehicleId + "_" + hash.substring(0,12) + ext;
        Files.write(dir.resolve(fileName), bytes, StandardOpenOption.CREATE_NEW);
        return fileName;
    }

    private boolean sameHash(Path path, String expected) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))).equals(expected); }
        catch (Exception ignored) { return false; }
    }
}
