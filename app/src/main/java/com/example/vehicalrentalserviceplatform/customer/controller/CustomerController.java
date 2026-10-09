package com.example.vehicalrentalserviceplatform.customer.controller;

import com.example.vehicalrentalserviceplatform.customer.model.RegularCustomer;
import com.example.vehicalrentalserviceplatform.model.User;
import com.example.vehicalrentalserviceplatform.customer.service.CustomerFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.servlet.http.HttpSession;
import java.util.regex.Pattern; // ADDED: Import for Regex

@Controller
public class CustomerController {

    @Autowired
    private CustomerFileService customerFileService;

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-zA-Z]{2,}$";

    private boolean isValidEmail(String email) {
        if (email == null) return false;
        return Pattern.compile(EMAIL_REGEX).matcher(email).matches();
    }


    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("customer", new RegularCustomer());
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@ModelAttribute RegularCustomer customer, Model model) {
        if (!isValidEmail(customer.getEmail())) {
            model.addAttribute("error", "Invalid email format. Please enter a valid email address.");
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
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String username,
                              @RequestParam String password,
                              HttpSession session,
                              Model model) {
        boolean valid = customerFileService.loginCustomer(username, password);
        if (valid) {

            session.setAttribute("loggedInUser", username);
            return "redirect:/catalog";
        } else {
            model.addAttribute("error", "Invalid username or password. Please try again.");
            return "login";
        }
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
                               Model model) {
        if (!isValidEmail(email)) {
            User customer = customerFileService.findCustomer(username);
            model.addAttribute("customer", customer);
            model.addAttribute("error", "Update failed: Invalid email format.");
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
                                       Model model) {
        User customer = customerFileService.findCustomer(username);
        model.addAttribute("customer", customer);

        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("passwordError", "New passwords do not match.");
            return "profile";
        }

        if (newPassword.length() < 6) {
            model.addAttribute("passwordError", "Password must be at least 6 characters.");
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

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();

        return "redirect:/login";
    }

}