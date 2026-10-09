package com.example.vehicalrentalserviceplatform.maintenance;

import java.io.*;
import java.util.*;

// FILE HANDLER LAYER: Handles all maintenance.txt read/write operations separately from the controller.
public class MaintenanceFileHandler {

    // DATA SOURCE: Text file used to persist maintenance records.
    private static final String FILE_PATH = "maintenance.txt";

    // READ: Loads all saved records from maintenance.txt and converts each valid line into a MaintenanceRecord object.
    public static List<MaintenanceRecord> loadAllRecords() {
        List<MaintenanceRecord> records = new ArrayList<>();
        File file = new File(FILE_PATH);

        if (!file.exists()) {
            return records;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 7);
                if (parts.length == 7) {
                    MaintenanceRecord record = new MaintenanceRecord(
                            parts[0], parts[1], parts[2], parts[3],
                            parts[4], parts[5], parts[6]
                    );
                    records.add(record);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return records;
    }

    // WRITE: Rewrites the complete record list back to maintenance.txt after update or delete operations.
    public static void saveAllRecords(List<MaintenanceRecord> records) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, false))) {
            for (MaintenanceRecord record : records) {
                writer.write(record.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // CREATE: Appends one new MaintenanceRecord to the end of maintenance.txt.
    public static void addRecord(MaintenanceRecord record) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            writer.write(record.toString());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // UPDATE: Loads records, finds the matching record ID, changes its status, and saves the updated list.
    public static void updateStatus(String recordId, String newStatus) {
        List<MaintenanceRecord> records = loadAllRecords();
        for (MaintenanceRecord record : records) {
            if (record.getRecordId().equals(recordId)) {
                record.setStatus(newStatus);
                break;
            }
        }
        saveAllRecords(records);
    }

    // DELETE: Removes the matching record from the loaded list and saves the remaining records.
    public static void deleteRecord(String recordId) {
        List<MaintenanceRecord> records = loadAllRecords();
        records.removeIf(record -> record.getRecordId().equals(recordId));
        saveAllRecords(records);
    }

    // SEARCH: Finds one maintenance record by ID; used by the alert feature.
    public static MaintenanceRecord findRecordById(String recordId) {
        List<MaintenanceRecord> records = loadAllRecords();
        for (MaintenanceRecord record : records) {
            if (record.getRecordId().equals(recordId)) {
                return record;
            }
        }
        return null;
    }
}