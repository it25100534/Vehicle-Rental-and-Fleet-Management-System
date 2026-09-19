package com.example.vehicalrentalserviceplatform.service;

import com.example.vehicalrentalserviceplatform.model.Booking;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private static final Path FILE_PATH = Paths.get("booking.txt").toAbsolutePath();

    public boolean isVehicleAvailable(String vehicleId, String requestedStartDate, String requestedReturnDate){

        List<Booking> activeBookings = getAllBookings();

        LocalDate reqStart = LocalDate.parse(requestedStartDate);
        LocalDate reqReturn = LocalDate.parse(requestedReturnDate);

        for(Booking x : activeBookings){
            if (x.getVehicleId().equals(vehicleId) && !x.getBookingStatus().equals("Cancelled")){

                LocalDate bookedStart = LocalDate.parse(x.getStartDate());
                LocalDate bookedReturn = LocalDate.parse(x.getReturnDate());


                if (!reqStart.isAfter(bookedReturn) && !reqReturn.isBefore(bookedStart)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isVehicleAvailableForUpdate(String transactionId, String vehicleId, String requestedStartDate, String requestedReturnDate) {
        List<Booking> activeBookings = getAllBookings();
        LocalDate reqStart = LocalDate.parse(requestedStartDate);
        LocalDate reqReturn = LocalDate.parse(requestedReturnDate);

        for (Booking x : activeBookings) {
            if (x.getTransactionId().equals(transactionId)) {
                continue;
            }

            if (x.getVehicleId().equals(vehicleId) && !x.getBookingStatus().equals("Cancelled")) {
                LocalDate bookedStart = LocalDate.parse(x.getStartDate());
                LocalDate bookedReturn = LocalDate.parse(x.getReturnDate());

                if (!reqStart.isAfter(bookedReturn) && !reqReturn.isBefore(bookedStart)) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean createBooking(Booking newBooking){

        if(!isVehicleAvailable(newBooking.getVehicleId(),newBooking.getStartDate(),newBooking.getReturnDate())){
            System.out.println("Error: Vehicle is already booked for those dates.");
            return false;
        }

        try(FileWriter fileWriter = new FileWriter(FILE_PATH.toFile(), true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            PrintWriter out = new PrintWriter(bufferedWriter)){

            out.println(newBooking.toString());
            System.out.println("Booking Successful");
            return true;

        }catch(IOException e){
            System.out.println("An error occurred");
            e.printStackTrace();
            return false;
        }
    }

    public List<Booking> getAllBookings(){
        List<Booking> bookingList = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(FILE_PATH.toFile()))){
            String line;
            while ((line = bufferedReader.readLine()) != null){
                String[] data = line.split(",");

                if(data.length == 6){
                    Booking booking = new Booking(data[0],data[1],data[2],data[3],data[4],data[5]);
                    bookingList.add(booking);
                }
            }
        }catch (IOException e){
            System.out.println("No existing bookings found or error reading file.");
        }
        return bookingList;

    }

    public List<Booking> getBookingsByCustomer(String customerName) {
        List<Booking> allBookings = getAllBookings();

        List<Booking> userBookings = new ArrayList<>();

        for (Booking x : allBookings) {
            if (x.getCustomerName().equalsIgnoreCase(customerName)) {
                userBookings.add(x);
            }
        }

        return userBookings;
    }

    private void overWriteBookingFile(List<Booking> updatedBookings){

        try(FileWriter fileWriter = new FileWriter(FILE_PATH.toFile(),false);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            PrintWriter out = new PrintWriter(bufferedWriter)){

            for (Booking x : updatedBookings){
                out.println(x.toString());
            }
        }catch (IOException e){
            System.out.println("Error updating the bookings file");
            e.printStackTrace();
        }

    }

    public boolean updateBooking(String transactionId,String newVehicleID, String newStartDate, String newReturnDate, String newStatus){

        if(!isVehicleAvailableForUpdate(transactionId, newVehicleID,newStartDate, newReturnDate)){
            System.out.println("Error: Vehicle is already booked for those updated dates.");
            return false;
        }

        List<Booking> allBookings = getAllBookings();

        boolean isUpdated = false;

        for (Booking x : allBookings){
            if(x.getTransactionId().equals(transactionId)){
                x.setVehicleId(newVehicleID);
                x.setStartDate(newStartDate);
                x.setReturnDate(newReturnDate);
                x.setBookingStatus(newStatus);
                isUpdated = true;
                break;
            }
        }

        if (isUpdated){
            overWriteBookingFile(allBookings);
            System.out.println("Booking updated successfully");
            return true;
        }else {
            System.out.println("Error: Booking updated failed");
            return false;
        }
    }

    public void updateBookingStatus(String transactionId, String newStatus) {
        List<Booking> allBookings = getAllBookings();
        boolean isUpdated = false;

        for (Booking x : allBookings) {
            if (x.getTransactionId().equals(transactionId)) {
                x.setBookingStatus(newStatus);
                isUpdated = true;
                break;
            }
        }

        if (isUpdated) {
            overWriteBookingFile(allBookings);
            System.out.println("Booking " + transactionId + " status updated to: " + newStatus);
        } else {
            System.out.println("Error: Could not find booking to update status.");
        }
    }

    public void deleteBooking(String transactionId){
        List<Booking> allBookings = getAllBookings();

        boolean isRemoved = allBookings.removeIf(x -> x.getTransactionId().equals(transactionId) && x.getBookingStatus().equals("Pending"));

        if (isRemoved){
            overWriteBookingFile(allBookings);
        }
    }

    public Booking getBookingById(String transactionId) {
        for (Booking b : getAllBookings()) {
            if (b.getTransactionId().equals(transactionId)) {
                return b;
            }
        }
        return null;
    }
}
