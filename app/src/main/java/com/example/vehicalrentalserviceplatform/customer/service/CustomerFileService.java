package com.example.vehicalrentalserviceplatform.customer.service;

import com.example.vehicalrentalserviceplatform.customer.model.RegularCustomer;
import com.example.vehicalrentalserviceplatform.model.User;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerFileService {

    private static final String FILE_PATH = "customer.txt";

    private void initFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("Could not create customer.txt: " + e.getMessage());
            }
        }
    }

    private List<String> readAllLines() {
        initFile();
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("customer.txt not found: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Error reading customer.txt: " + e.getMessage());
        }
        return lines;
    }

    private void writeAllLines(List<String> lines) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, false))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error writing to customer.txt: " + e.getMessage());
        }
    }

    public boolean registerCustomer(User customer) {
        if (findCustomer(customer.getUsername()) != null) {
            return false;
        }
        String line = "";
        if (customer instanceof RegularCustomer) {
            line = ((RegularCustomer) customer).toFileString();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            initFile();
            writer.write(line);
            writer.newLine();
            return true;
        } catch (IOException e) {
            System.out.println("Error saving customer: " + e.getMessage());
            return false;
        }
    }

    public User findCustomer(String query) {
        List<String> lines = readAllLines();
        for (String line : lines) {
            String[] fields = line.split(",");
            if (fields.length >= 4) {
                if (fields[1].equalsIgnoreCase(query) ||
                        fields[3].equalsIgnoreCase(query)) {
                    return parseLineToCustomer(line);
                }
            }
        }
        return null;
    }

    public boolean updateCustomer(String username, String email, String phone) {
        List<String> lines = readAllLines();
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            String[] fields = lines.get(i).split(",");
            if (fields.length >= 6 && fields[1].equalsIgnoreCase(username)) {
                fields[4] = email;
                fields[5] = phone;
                lines.set(i, String.join(",", fields));
                found = true;
                break;
            }
        }
        if (found) {
            writeAllLines(lines);
        }
        return found;
    }

    public boolean changePassword(String username, String currentPassword, String newPassword) {
        User customer = findCustomer(username);
        if (customer == null) {
            return false;
        }
        if (!customer.validatePassword(currentPassword)) {
            return false;
        }
        List<String> lines = readAllLines();
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            String[] fields = lines.get(i).split(",");
            if (fields.length >= 6 && fields[1].equalsIgnoreCase(username)) {
                fields[2] = newPassword; // password is index 2
                lines.set(i, String.join(",", fields));
                found = true;
                break;
            }
        }
        if (found) {
            writeAllLines(lines);
        }
        return found;
    }

    public boolean loginCustomer(String username, String password) {
        User customer = findCustomer(username);
        if (customer == null) {
            return false;
        }
        return customer.validatePassword(password);
    }

    private User parseLineToCustomer(String line) {
        String[] fields = line.split(",");
        try {
            if (fields[0].equals("CUSTOMER") && fields.length == 6) {
                return new RegularCustomer(
                        fields[1], // username
                        fields[2], // password
                        fields[3], // licenseId
                        fields[4], // email
                        fields[5]  // phone
                );
            }
        } catch (Exception e) {
            System.out.println("Error parsing line: " + e.getMessage());
        }
        return null;
    }
}