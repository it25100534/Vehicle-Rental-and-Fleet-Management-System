package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.fleet.Branch;
import com.example.vehiclerentalserviceplatform.fleet.BranchService;
import com.example.vehiclerentalserviceplatform.fleet.VehicleCategoryService;
import com.example.vehiclerentalserviceplatform.service.VehicleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FleetConfigurationController {
    private final BranchService branchService;
    private final VehicleCategoryService categoryService;
    private final VehicleService vehicleService;
    private final com.example.vehiclerentalserviceplatform.fleet.BranchImageStorage branchImages;

    public FleetConfigurationController(BranchService branchService, VehicleCategoryService categoryService,
                                        VehicleService vehicleService,
                                        com.example.vehiclerentalserviceplatform.fleet.BranchImageStorage branchImages) {
        this.branchService = branchService;
        this.categoryService = categoryService;
        this.vehicleService = vehicleService;
        this.branchImages = branchImages;
    }

    @GetMapping("/api/branches")
    @ResponseBody
    public List<Branch> openBranches() { return branchService.getOpenBranches(); }

    @GetMapping("/api/branches/all")
    @ResponseBody
    public List<Branch> allBranches() { return branchService.getAll(); }

    @GetMapping("/api/vehicle-categories")
    @ResponseBody
    public List<com.example.vehiclerentalserviceplatform.fleet.VehicleCategory> activeCategories() {
        return categoryService.getActive();
    }

    @GetMapping("/admin/branches")
    public String branches(Model model) {
        model.addAttribute("branches", branchService.getAll());
        model.addAttribute("vehicleService", vehicleService);
        return "branches";
    }

    @GetMapping({"/admin/branches/new", "/admin/branches/{id}/edit"})
    public String branchForm(@PathVariable(required = false) String id, Model model) {
        Branch branch = id == null ? null : branchService.find(id);
        model.addAttribute("branch", branch);
        return "branch-form";
    }

    @PostMapping("/admin/branches/save")
    public String saveBranch(@RequestParam String branchId, @RequestParam String name,
                             @RequestParam String address, @RequestParam String phone,
                             @RequestParam(required = false) org.springframework.web.multipart.MultipartFile image,
                             RedirectAttributes redirect) {
        try {
            byte[] bytes = branchImages.validate(image);
            String imagePath = branchImages.store(bytes);
            branchService.save(branchId, name, address, phone);
            if (imagePath != null) branchService.setImage(branchId, imagePath);
            redirect.addFlashAttribute("message", "Branch saved successfully.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        } catch (java.io.IOException ex) {
            redirect.addFlashAttribute("error", "The branch photo could not be saved. Please try again.");
        }
        return "redirect:/admin/branches";
    }

    @PostMapping("/admin/branches/{id}/status")
    public String branchStatus(@PathVariable String id, @RequestParam boolean open,
                               RedirectAttributes redirect) {
        if (branchService.setOpen(id, open)) {
            redirect.addFlashAttribute("message", open
                    ? "Branch reopened. Its assigned vehicles can be booked again."
                    : "Branch closed. Its assigned vehicles are unavailable until the branch reopens or they are relocated.");
        } else redirect.addFlashAttribute("error", "Branch was not found.");
        return "redirect:/admin/branches";
    }

    @PostMapping("/admin/branches/{id}/delete")
    public String deleteBranch(@PathVariable String id, RedirectAttributes redirect) {
        if (vehicleService.countVehiclesAtBranch(id) > 0) {
            redirect.addFlashAttribute("error", "This branch cannot be deleted while vehicles are assigned to it. Relocate them first.");
        } else if (branchService.delete(id)) {
            redirect.addFlashAttribute("message", "Branch deleted.");
        } else {
            redirect.addFlashAttribute("error", "Branch was not found.");
        }
        return "redirect:/admin/branches";
    }

    @GetMapping("/admin/vehicle-categories")
    public String categories(Model model) {
        model.addAttribute("categories", categoryService.getAll());
        return "vehicle-categories";
    }

    @PostMapping("/admin/vehicle-categories/save")
    public String saveCategory(@RequestParam String code, @RequestParam String name,
                               RedirectAttributes redirect) {
        try {
            categoryService.save(code, name);
            redirect.addFlashAttribute("message", "Category saved successfully.");
        } catch (IllegalArgumentException ex) { redirect.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/vehicle-categories";
    }

    @PostMapping("/admin/vehicle-categories/{code}/status")
    public String categoryStatus(@PathVariable String code, @RequestParam boolean active,
                                 RedirectAttributes redirect) {
        if (!active && vehicleService.countVehiclesInCategory(code) > 0) {
            redirect.addFlashAttribute("error", "Reassign every vehicle before disabling this category.");
        } else if (categoryService.setActive(code, active)) {
            redirect.addFlashAttribute("message", active ? "Category enabled." : "Category disabled.");
        } else redirect.addFlashAttribute("error", "Category was not found.");
        return "redirect:/admin/vehicle-categories";
    }
}
