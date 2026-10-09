package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.security.SessionAccess;
import com.example.vehiclerentalserviceplatform.service.LicenceSubmissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LicenceSubmissionController {
    private final LicenceSubmissionService licences;

    public LicenceSubmissionController(LicenceSubmissionService licences) { this.licences = licences; }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    String oversizedUpload(RedirectAttributes redirect) {
        redirect.addFlashAttribute("licenceError", "Each licence image must be 3 MB or less.");
        return "redirect:/profile#licence-verification";
    }

    @PostMapping("/profile/licence")
    String submit(@RequestParam MultipartFile frontImage, @RequestParam MultipartFile backImage,
                  HttpSession session, RedirectAttributes redirect) {
        String username = SessionAccess.customer(session);
        if (username == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        try {
            licences.submit(username, frontImage, backImage);
            redirect.addFlashAttribute("licenceMessage", "Your licence images were sent for review. We will notify you when a team member has checked them.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("licenceError", ex.getMessage());
        }
        return "redirect:/profile#licence-verification";
    }

    @GetMapping("/admin/customers/{username}/licence/{side}/image")
    ResponseEntity<Resource> image(@PathVariable String username, @PathVariable String side, HttpSession session) {
        if (!SessionAccess.isStaff(session)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        try {
            LicenceSubmissionService.Image image = licences.image(username, side);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(image.mime()))
                    .cacheControl(CacheControl.noStore())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .header("X-Content-Type-Options", "nosniff")
                    .body(image.resource());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/admin/customers/{username}/licence/review")
    String review(@PathVariable String username, @RequestParam String decision,
                  @RequestParam(required = false) String reason,
                  @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "") String q,
                  HttpSession session, RedirectAttributes redirect) {
        if (!SessionAccess.isStaff(session)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Object actor = session.getAttribute("loggedInAdmin");
        try {
            licences.review(username, decision, reason, actor instanceof String ? (String) actor : "system");
            redirect.addFlashAttribute("licenceReviewMessage", "approve".equalsIgnoreCase(decision)
                    ? "Licence approved for " + username + "." : "Licence rejected for " + username + ".");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("licenceReviewError", ex.getMessage());
        }
        redirect.addAttribute("page", page).addAttribute("q", q);
        return "redirect:/admin/customers";
    }
}
