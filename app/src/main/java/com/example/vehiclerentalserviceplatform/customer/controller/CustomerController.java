package com.example.vehiclerentalserviceplatform.customer.controller;

import com.example.vehiclerentalserviceplatform.customer.model.RegularCustomer;
import com.example.vehiclerentalserviceplatform.model.User;
import com.example.vehiclerentalserviceplatform.customer.service.CustomerFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import com.example.vehiclerentalserviceplatform.security.SessionAccess;
import com.example.vehiclerentalserviceplatform.security.InputValidation;
import com.example.vehiclerentalserviceplatform.model.Booking;
import com.example.vehiclerentalserviceplatform.service.BookingService;
import com.example.vehiclerentalserviceplatform.service.LicenceSubmissionService;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomerController {

    @Autowired
    private CustomerFileService customerFileService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private LicenceSubmissionService licenceSubmissionService;

    @ModelAttribute("licenceSubmission")
    public LicenceSubmissionService.Submission licenceSubmission(HttpSession session) {
        String username = SessionAccess.customer(session);
        return username == null ? null : licenceSubmissionService.forCustomer(username);
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("customer", new RegularCustomer());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@ModelAttribute RegularCustomer customer,
                                 @RequestParam String confirmPassword,
                                 Model model) {
        if (!InputValidation.isUsername(customer.getUsername())) {
            model.addAttribute("error", "Username must be 3 to 30 letters, numbers, dots, underscores, or hyphens.");
            return "register";
        }
        if (!InputValidation.isLicenseId(customer.getLicenseId())) {
            model.addAttribute("error", "License ID must be 4 to 25 letters, numbers, or hyphen-separated groups.");
            return "register";
        }
        if (customerFileService.isLicenseIdTaken(customer.getLicenseId())) {
            model.addAttribute("error", "That License ID is already registered.");
            return "register";
        }
        if (!InputValidation.isEmail(customer.getEmail())) {
            model.addAttribute("error", "Invalid email format. Please enter a valid email address.");
            return "register";
        }
        if (!InputValidation.isPhone(customer.getPhone())) {
            model.addAttribute("error", "Phone number must contain exactly 10 digits and begin with 0.");
            return "register";
        }
        if (!InputValidation.isPassword(customer.getPassword())) {
            model.addAttribute("error", "Password must be 6 to 64 characters and contain a letter and a number.");
            return "register";
        }
        if (!customer.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "Password and confirmation do not match.");
            return "register";
        }

        boolean success = customerFileService.registerCustomer(customer);
        if (success) {
            model.addAttribute("message", "Registration successful! Please sign in.");
            return "login";
        } else {
            model.addAttribute("error", "Username already exists. Please choose another.");
            return "register";
        }
    }


    @GetMapping("/login")
    public String showLoginPage(@RequestParam(required = false) String resetRequested,
                                @RequestParam(required = false) String deactivated,
                                @RequestParam(required = false) String switchAccount,
                                Model model) {
        if (resetRequested != null) {
            model.addAttribute("message", "Password reset. Sign in with your new password.");
        }
        if (deactivated != null) model.addAttribute("message", "Your account has been deactivated.");
        if (switchAccount != null) model.addAttribute("message", "Sign in with a customer account to open that page. This will switch your current session from staff to customer.");
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String username,
                              @RequestParam String password,
                              HttpServletRequest request,
                              Model model) {
        boolean valid = customerFileService.loginCustomer(username, password);
        if (valid) {
            HttpSession existing = request.getSession(false);
            if (existing != null) existing.invalidate();
            HttpSession session = request.getSession(true);
            session.setAttribute("loggedInUser", username.trim());
            return "redirect:/catalog";
        } else {
            model.addAttribute("error", customerFileService.isDisabledCustomer(username)
                    ? "Your account is disabled. Ask an administrator to enable it."
                    : "Invalid username or password. Please try again.");
            return "login";
        }
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@RequestParam String username,
                                       @RequestParam String email,
                                       @RequestParam String licenseId,
                                       @RequestParam String newPassword,
                                       @RequestParam String confirmPassword,
                                       Model model) {
        if (!InputValidation.isUsername(username) || !InputValidation.isEmail(email)
                || !InputValidation.isLicenseId(licenseId)) {
            model.addAttribute("error", "The account details are invalid.");
            return "forgot-password";
        }
        if (!InputValidation.isPassword(newPassword)) {
            model.addAttribute("error", "Password must be 6 to 64 characters and contain a letter and a number.");
            return "forgot-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Password and confirmation do not match.");
            return "forgot-password";
        }
        if (!customerFileService.resetPassword(username, email, licenseId, newPassword)) {
            model.addAttribute("error", customerFileService.isDisabledCustomer(username)
                    ? "Your account is disabled. Ask an administrator to enable it."
                    : "Password was not changed. Check the registered username, email and licence ID.");
            return "forgot-password";
        }
        return "redirect:/login?resetRequested=true";
    }


    @GetMapping("/profile")
    public String showProfilePage(HttpSession session, Model model) {

        String currentUser = (String) session.getAttribute("loggedInUser");

        if (currentUser == null) {
            return "redirect:/login";
        }

        User customer = customerFileService.findCustomer(currentUser);

        if (customer == null) {
            model.addAttribute("error", "Customer not found.");
            return "redirect:/login";
        }
        model.addAttribute("customer", customer);
        return "profile";
    }

    @PostMapping("/profile/update")
    public String handleUpdate(@RequestParam String username,
                               @RequestParam String email,
                               @RequestParam String phone,
                               HttpSession session,
                               Model model) {
        String currentUser = SessionAccess.customer(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        username = currentUser;
        if (!InputValidation.isEmail(email)) {
            User customer = customerFileService.findCustomer(username);
            model.addAttribute("customer", customer);
            model.addAttribute("error", "Update failed: Invalid email format.");
            return "profile";
        }
        if (!InputValidation.isPhone(phone)) {
            User customer = customerFileService.findCustomer(username);
            model.addAttribute("customer", customer);
            model.addAttribute("error", "Update failed: phone number must contain exactly 10 digits and begin with 0.");
            return "profile";
        }

        boolean updated = customerFileService.updateCustomer(username, email, phone);
        if (updated) {
            User customer = customerFileService.findCustomer(username);
            model.addAttribute("customer", customer);
            model.addAttribute("message", "Profile updated successfully.");
        } else {
            model.addAttribute("error", "Update failed. Customer not found.");
        }
        return "profile";
    }

    @PostMapping("/profile/password")
    public String handlePasswordChange(@RequestParam String username,
                                       @RequestParam String currentPassword,
                                       @RequestParam String newPassword,
                                       @RequestParam String confirmPassword,
                                       HttpSession session,
                                       Model model) {
        String currentUser = SessionAccess.customer(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        username = currentUser;
        User customer = customerFileService.findCustomer(username);
        model.addAttribute("customer", customer);

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("passwordError", "New passwords do not match.");
            return "profile";
        }

        if (!InputValidation.isPassword(newPassword)) {
            model.addAttribute("passwordError", "Password must be 6 to 64 characters and contain a letter and a number.");
            return "profile";
        }

        boolean changed = customerFileService.changePassword(username, currentPassword, newPassword);
        if (changed) {
            model.addAttribute("passwordMessage", "Password changed successfully.");
        } else {
            model.addAttribute("passwordError", "Current password is incorrect.");
        }
        return "profile";
    }

    @PostMapping("/profile/deactivate")
    public String deactivateAccount(@RequestParam String currentPassword,
                                    HttpSession session,
                                    Model model) {
        String username = SessionAccess.customer(session);
        if (username == null) return "redirect:/login";

        boolean hasOpenBooking = bookingService.getBookingsByCustomer(username).stream()
                .map(Booking::getBookingStatus)
                .filter(status -> status != null)
                .anyMatch(status -> status.equalsIgnoreCase("Pending")
                        || status.equalsIgnoreCase("Approved")
                        || status.equalsIgnoreCase("Paid")
                        || status.equalsIgnoreCase("Active"));
        if (hasOpenBooking) {
            model.addAttribute("customer", customerFileService.findCustomer(username));
            model.addAttribute("error", "Finish or cancel your open bookings before deactivating the account.");
            return "profile";
        }
        if (!customerFileService.deactivateCustomer(username, currentPassword)) {
            model.addAttribute("customer", customerFileService.findCustomer(username));
            model.addAttribute("error", "Account deactivation failed. Check your current password.");
            return "profile";
        }
        session.invalidate();
        return "redirect:/login?deactivated=true";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

}
