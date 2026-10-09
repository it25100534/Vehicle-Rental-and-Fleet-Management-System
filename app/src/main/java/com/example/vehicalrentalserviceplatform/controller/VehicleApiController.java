package com.example.vehicalrentalserviceplatform.controller;

import com.example.vehicalrentalserviceplatform.model.*;
import com.example.vehicalrentalserviceplatform.service.VehicleService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleApiController {

    private final VehicleService vehicleService;
    private final String UPLOAD_DIR = "src/main/resources/static/images/";

    public VehicleApiController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public List<Vehicle> getAll() {
        return vehicleService.getAllVehicles();
    }

    @GetMapping("/{id}")
    public Vehicle getOne(@PathVariable String id) {
        return vehicleService.getVehicleById(id);
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
            @RequestParam(value = "seats", required = false) Integer seats,
            @RequestParam(value = "driveTrain", required = false) String driveTrain,
            @RequestParam(value = "motoType", required = false) String motoType,
            @RequestParam("image") MultipartFile imageFile) {

        try {
            String vehicleId = vehicleService.generateNextId(type);

            String fileName = vehicleId + "_" + imageFile.getOriginalFilename();
            Path path = Paths.get(UPLOAD_DIR + fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, imageFile.getBytes());

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
            @RequestParam(value = "seats", required = false) Integer seats,
            @RequestParam(value = "driveTrain", required = false) String driveTrain,
            @RequestParam(value = "motoType", required = false) String motoType,
            @RequestParam(value = "image", required = false) MultipartFile imageFile) {

        try {
            Vehicle existingVehicle = vehicleService.getVehicleById(id);
            if (existingVehicle == null) {
                return "Error: Vehicle not found!";
            }

            String fileName = existingVehicle.getVehicleImageFileName();

            if (imageFile != null && !imageFile.isEmpty()) {
                fileName = id + "_" + imageFile.getOriginalFilename();
                Path path = Paths.get(UPLOAD_DIR + fileName);
                Files.createDirectories(path.getParent());
                Files.write(path, imageFile.getBytes());
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
    public void delete(@PathVariable String id) {
        vehicleService.deleteVehicle(id);
    }
}