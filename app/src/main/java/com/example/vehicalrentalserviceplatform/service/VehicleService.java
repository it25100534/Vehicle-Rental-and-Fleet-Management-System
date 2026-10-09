package com.example.vehicalrentalserviceplatform.service;

import com.example.vehicalrentalserviceplatform.model.*;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class VehicleService {
    private final String FILE_PATH = "vehicles.txt";
    private List<Vehicle> vehicles = new ArrayList<>();

    public VehicleService() {
        loadVehicles();
    }

    public List<Vehicle> getAllVehicles() {
        return vehicles;
    }

    public void addVehicle(Vehicle vehicle) {
        vehicles.add(vehicle);
        saveVehiclesToFile();
    }

    public boolean deleteVehicle(String vehicleId) {
        boolean removed = vehicles.removeIf(v -> v.getVehicleId().equals(vehicleId));
        if (removed) {
            saveVehiclesToFile();
        }
        return removed;
    }

    private void saveVehiclesToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (Vehicle v : vehicles) {
                // toString() method to save txt
                writer.println(v.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadVehicles() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 2) continue;

                //Read everything in the exact order of super.toString()
                String type = parts[0];
                String id = parts[1];
                String year = parts[2];
                String make = parts[3];
                String model = parts[4];
                String fuel = parts[5];
                double mileage = Double.parseDouble(parts[6]);
                String image = parts[7];
                double rate = Double.parseDouble(parts[8]);
                boolean isAvailable = Boolean.parseBoolean(parts[9]);

                //Put them into the Constructors
                if (type.equals("CAR")) {
                    int seats = Integer.parseInt(parts[10]);
                    vehicles.add(new Car(make, model, year, rate, fuel, mileage, isAvailable, seats, image, id));
                } else if (type.equals("MOTORCYCLE")) {
                    String motoType = parts[10];
                    vehicles.add(new Motorcycle(make, model, year, rate, fuel, mileage, isAvailable, motoType, image, id));
                } else if (type.equals("SUV")) {
                    int seats = Integer.parseInt(parts[10]);
                    String driveTrain = parts[11];
                    vehicles.add(new Suv(make, model, year, rate, fuel, mileage, isAvailable, seats, driveTrain, image, id));
                } else if (type.equals("VAN")) {
                    int seats = Integer.parseInt(parts[10]);
                    String driveTrain = parts[11];
                    vehicles.add(new Van(make, model, year, rate, fuel, mileage, isAvailable, seats, driveTrain, image, id));
                }
            }
        } catch (IOException e) {
            System.out.println("Error loading inventory: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Data format error in text file. Ensure it matches the new super.toString() format: " + e.getMessage());
        }
    }
    public Vehicle getVehicleById(String id) {
        return vehicles.stream()
                .filter(v -> v.getVehicleId().equals(id))
                .findFirst()
                .orElse(null);
    }

    public boolean updateVehicle(Vehicle updatedVehicle) {
        for (int i = 0; i < vehicles.size(); i++) {
            // Find the vehicle that has the same ID
            if (vehicles.get(i).getVehicleId().equals(updatedVehicle.getVehicleId())) {
                // Replace the old vehicle with the newly updated one
                vehicles.set(i, updatedVehicle);
                // Save the whole list back to vehicles.txt
                saveVehiclesToFile();
                return true;
            }
        }
        return false; // If the ID wasn't found
    }
    public String generateNextId(String type) {
        //select the correct prefix
        String prefix;
        if (type.equals("CAR")) {
            prefix = "CAR-";
        } else if (type.equals("MOTORCYCLE")) {
            prefix = "MOT-";
        } else if (type.equals("SUV")) {
            prefix = "SUV-";
        } else {
            prefix = "VAN-";
        }

        //Find the highest number currently in use for that vehicle type
        int highestNumber = 0;

        for (Vehicle v : vehicles) {
            String currentId = v.getVehicleId();

            // Only look at vehicles that start with this specific prefix
            if (currentId != null && currentId.startsWith(prefix)) {
                try {
                    // remove prefix
                    String numberPart = currentId.substring(prefix.length());
                    int number = Integer.parseInt(numberPart);

                    if (number > highestNumber) {
                        highestNumber = number;
                    }
                } catch (NumberFormatException e) {
                    // If number formats broken skip it
                    System.out.println("Skipping invalid ID format: " + currentId);
                }
            }
        }

        //Add 1 to the highest number found
        int nextNumber = highestNumber + 1;

        //Return the combined string (e.g., "CAR-1")
        return prefix + String.format("%04d", nextNumber); //%04d is for 4digit logic
    }
}