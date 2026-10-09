package com.example.vehiclerentalserviceplatform.service;

import com.example.vehiclerentalserviceplatform.model.Car;
import com.example.vehiclerentalserviceplatform.model.Motorcycle;
import com.example.vehiclerentalserviceplatform.model.Suv;
import com.example.vehiclerentalserviceplatform.model.Van;
import com.example.vehiclerentalserviceplatform.model.Vehicle;
import com.example.vehiclerentalserviceplatform.persistence.VehicleEntity;
import com.example.vehiclerentalserviceplatform.persistence.VehicleRepository;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
@DependsOn({"branchService", "vehicleCategoryService"})
public class VehicleService {
    private static final String FILE_PATH = "vehicles.txt";
    private final VehicleRepository repository;

    public VehicleService(VehicleRepository repository) {
        this.repository = repository;
        importLegacyVehiclesWhenEmpty();
    }

    public List<Vehicle> getAllVehicles() {
        return repository.findAll().stream().map(this::toVehicle).toList();
    }

    public Page<Vehicle> getInventoryPage(String query, int page, int size) {
        return repository.searchActive(query == null ? "" : query.trim(),
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 50)))).map(this::toVehicle);
    }

    public void addVehicle(Vehicle vehicle) { repository.save(toEntity(vehicle)); }

    public boolean deleteVehicle(String vehicleId) {
        VehicleEntity entity = repository.findById(vehicleId).orElse(null);
        if (entity == null) return false;
        entity.setStatus("UNAVAILABLE");
        entity.setRetired(true);
        repository.save(entity);
        return true;
    }

    public Vehicle getVehicleById(String id) { return repository.findById(id).map(this::toVehicle).orElse(null); }

    public boolean updateVehicle(Vehicle updatedVehicle) {
        VehicleEntity existing = repository.findById(updatedVehicle.getVehicleId()).orElse(null);
        if (existing == null) return false;
        VehicleEntity replacement = toEntity(updatedVehicle);
        replacement.setRetired(existing.isRetired());
        if (existing.isRetired()) replacement.setStatus("UNAVAILABLE");
        repository.save(replacement);
        return true;
    }

    public boolean relocateVehicle(String vehicleId, String branchId) {
        Vehicle vehicle = getVehicleById(vehicleId);
        if (vehicle == null || !vehicle.isAvailable() || "RETIRED".equalsIgnoreCase(vehicle.getOperationalStatus())) return false;
        vehicle.setCurrentBranch(branchId);
        repository.save(toEntity(vehicle));
        return true;
    }

    public long countVehiclesAtBranch(String branchId) { return repository.countByBranchIdIgnoreCase(branchId); }
    public long countVehiclesInCategory(String categoryCode) { return repository.countByCategoryCodeIgnoreCase(categoryCode); }

    public String generateNextId(String type) {
        String prefix = switch (type) {
            case "CAR" -> "CAR-";
            case "MOTORCYCLE" -> "MOT-";
            case "SUV" -> "SUV-";
            default -> "VAN-";
        };
        int highestNumber = 0;
        for (Vehicle vehicle : getAllVehicles()) {
            String id = vehicle.getVehicleId();
            if (id != null && id.startsWith(prefix)) {
                try { highestNumber = Math.max(highestNumber, Integer.parseInt(id.substring(prefix.length()))); }
                catch (NumberFormatException ignored) {}
            }
        }
        return prefix + String.format("%04d", highestNumber + 1);
    }

    private void importLegacyVehiclesWhenEmpty() {
        File file = new File(FILE_PATH);
        if (repository.count() != 0 || !file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Vehicle vehicle = parseLegacyVehicle(line);
                if (vehicle != null) repository.save(toEntity(vehicle));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not import vehicle inventory.", ex);
        }
    }

    private Vehicle parseLegacyVehicle(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 11) return null;
        String type = parts[0];
        String id = parts[1];
        String year = parts[2];
        String make = parts[3];
        String model = parts[4];
        String fuel = parts[5];
        double mileage = Double.parseDouble(parts[6]);
        String image = parts[7];
        double rate = Double.parseDouble(parts[8]);
        boolean available = Boolean.parseBoolean(parts[9]);
        Vehicle vehicle;
        int metadataIndex;
        switch (type) {
            case "CAR" -> {
                Car car = new Car(make, model, year, rate, fuel, mileage, available,
                        Integer.parseInt(parts[10]), image, id);
                if (parts.length > 11 && usable(parts[11])) car.setCarType(parts[11]);
                vehicle = car; metadataIndex = 12;
            }
            case "MOTORCYCLE" -> {
                vehicle = new Motorcycle(make, model, year, rate, fuel, mileage, available, parts[10], image, id);
                metadataIndex = 11;
            }
            case "SUV" -> {
                vehicle = new Suv(make, model, year, rate, fuel, mileage, available,
                        Integer.parseInt(parts[10]), parts[11], image, id); metadataIndex = 12;
            }
            case "VAN" -> {
                vehicle = new Van(make, model, year, rate, fuel, mileage, available,
                        Integer.parseInt(parts[10]), parts[11], image, id); metadataIndex = 12;
            }
            default -> { return null; }
        }
        vehicle.setUsageCategory(parts.length > metadataIndex && usable(parts[metadataIndex])
                ? parts[metadataIndex] : vehicle.defaultUsageCategory());
        if (parts.length > metadataIndex + 1 && usable(parts[metadataIndex + 1]))
            vehicle.setCurrentBranch(parts[metadataIndex + 1]);
        return vehicle;
    }

    private VehicleEntity toEntity(Vehicle vehicle) {
        Integer seats = null;
        String subtype = null;
        if (vehicle instanceof Car car) { seats = car.getNumberOfSeats(); subtype = car.getCarType(); }
        else if (vehicle instanceof Suv suv) { seats = suv.getNumberOfSeats(); subtype = suv.getDriveTrain(); }
        else if (vehicle instanceof Van van) { seats = van.getNumberOfSeats(); subtype = van.getDriveTrain(); }
        else if (vehicle instanceof Motorcycle motorcycle) subtype = motorcycle.getMotorcycleType();
        VehicleEntity entity = new VehicleEntity(vehicle.getVehicleId(), vehicle.getMake(), vehicle.getModel(),
                Integer.parseInt(vehicle.getYear()), vehicle.getType(), vehicle.getFuelType(), seats, subtype,
                vehicle.getUsageCategory(), vehicle.getCurrentBranch(), BigDecimal.valueOf(vehicle.getRentalRate()),
                BigDecimal.valueOf(vehicle.getMileage()), vehicle.isAvailable() ? "AVAILABLE" : "UNAVAILABLE",
                vehicle.getVehicleImageFileName());
        entity.setRetired("RETIRED".equalsIgnoreCase(vehicle.getOperationalStatus()));
        return entity;
    }

    private Vehicle toVehicle(VehicleEntity entity) {
        String year = Integer.toString(entity.getManufactureYear());
        boolean available = "AVAILABLE".equalsIgnoreCase(entity.getStatus());
        String subtype = entity.getVehicleSubtype();
        Vehicle vehicle = switch (entity.getVehicleType()) {
            case "CAR" -> {
                Car car = new Car(entity.getMake(), entity.getModel(), year, entity.getRentalRate().doubleValue(),
                        entity.getFuelType(), entity.getMileage().doubleValue(), available,
                        entity.getSeatCount() == null ? 0 : entity.getSeatCount(), entity.getImagePath(), entity.getVehicleId());
                if (usable(subtype)) car.setCarType(subtype);
                yield car;
            }
            case "MOTORCYCLE" -> new Motorcycle(entity.getMake(), entity.getModel(), year,
                    entity.getRentalRate().doubleValue(), entity.getFuelType(), entity.getMileage().doubleValue(),
                    available, usable(subtype) ? subtype : "Standard", entity.getImagePath(), entity.getVehicleId());
            case "SUV" -> new Suv(entity.getMake(), entity.getModel(), year, entity.getRentalRate().doubleValue(),
                    entity.getFuelType(), entity.getMileage().doubleValue(), available,
                    entity.getSeatCount() == null ? 0 : entity.getSeatCount(), usable(subtype) ? subtype : "2WD",
                    entity.getImagePath(), entity.getVehicleId());
            case "VAN" -> new Van(entity.getMake(), entity.getModel(), year, entity.getRentalRate().doubleValue(),
                    entity.getFuelType(), entity.getMileage().doubleValue(), available,
                    entity.getSeatCount() == null ? 0 : entity.getSeatCount(), usable(subtype) ? subtype : "2WD",
                    entity.getImagePath(), entity.getVehicleId());
            default -> throw new IllegalStateException("Unsupported vehicle type: " + entity.getVehicleType());
        };
        vehicle.setUsageCategory(entity.getCategoryCode());
        vehicle.setCurrentBranch(entity.getBranchId());
        vehicle.setOperationalStatus(entity.isRetired() ? "RETIRED" : entity.getStatus());
        return vehicle;
    }

    private boolean usable(String value) { return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value); }
}
