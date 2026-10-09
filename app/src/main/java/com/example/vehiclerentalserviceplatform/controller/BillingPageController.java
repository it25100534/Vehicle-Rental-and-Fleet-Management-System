package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.model.Invoice;
import com.example.vehiclerentalserviceplatform.security.SessionAccess;
import com.example.vehiclerentalserviceplatform.service.BillingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class BillingPageController {

    private final BillingService billingService;

    public BillingPageController(BillingService billingService) {
        this.billingService = billingService;
    }

    @GetMapping("/admin/invoices/{id}")
    public String adminInvoice(@PathVariable Long id, Model model) {
        addInvoice(model, id);
        model.addAttribute("staffView", true);
        return "invoice-detail";
    }

    @GetMapping("/admin/invoices/{id}/edit")
    public String editInvoice(@PathVariable Long id, Model model) {
        addInvoice(model, id);
        return "edit-invoice";
    }

    @PostMapping("/admin/invoices/{id}/edit")
    public String saveInvoice(@PathVariable Long id,
                              @RequestParam double discount,
                              @RequestParam double lateFee,
                              @RequestParam double damageFee,
                              @RequestParam double fuelCharge,
                              @RequestParam double mileageCharge,
                              @RequestParam double alternateDropoffCharge,
                              @RequestParam double depositAmount,
                              RedirectAttributes redirect) {
        billingService.updateAmounts(id, discount, lateFee, damageFee, fuelCharge,
                mileageCharge, alternateDropoffCharge, depositAmount);
        redirect.addFlashAttribute("message", "Invoice amounts were updated and audited.");
        return "redirect:/admin/invoices/" + id;
    }

    @PostMapping("/admin/invoices/{id}/void")
    public String voidInvoice(@PathVariable Long id, @RequestParam String reason,
                              RedirectAttributes redirect) {
        billingService.voidInvoice(id, reason);
        redirect.addFlashAttribute("message", "Invoice was voided. Its audit record was retained.");
        return "redirect:/admin/invoices/" + id;
    }

    @PostMapping("/admin/invoices/{id}/refund")
    public String refundInvoice(@PathVariable Long id, @RequestParam double amount,
                                @RequestParam String reason, RedirectAttributes redirect) {
        billingService.refundInvoice(id, amount, reason);
        redirect.addFlashAttribute("message", "Refund was recorded and included in revenue totals.");
        return "redirect:/admin/invoices/" + id;
    }

    @GetMapping("/admin/invoices/{id}/print")
    public String printInvoice(@PathVariable Long id, Model model) {
        addInvoice(model, id);
        return "invoice-print";
    }

    @GetMapping("/payments/history")
    public String paymentHistory(HttpSession session, Model model) {
        String customer = SessionAccess.customer(session);
        List<Invoice> invoices = billingService.getInvoicesForCustomer(customer);
        model.addAttribute("customerName", customer);
        model.addAttribute("invoices", invoices);
        model.addAttribute("billingService", billingService);
        model.addAttribute("paidTotal", invoices.stream().mapToDouble(Invoice::getAmountBeforeRefund).sum());
        model.addAttribute("refundTotal", invoices.stream().mapToDouble(i -> safe(i.getRefundAmount())).sum());
        return "payment-history";
    }

    @GetMapping("/invoices/{id}")
    public String customerInvoice(@PathVariable Long id, HttpSession session, Model model) {
        Invoice invoice = findInvoice(id);
        String customer = SessionAccess.customer(session);
        if (customer == null || !customer.equalsIgnoreCase(invoice.getCustomerName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        model.addAttribute("invoice", invoice);
        model.addAttribute("paymentLabel", billingService.getSafePaymentLabel(invoice));
        model.addAttribute("staffView", false);
        return "invoice-detail";
    }

    @GetMapping("/invoices/{id}/print")
    public String customerPrint(@PathVariable Long id, HttpSession session, Model model) {
        customerInvoice(id, session, model);
        return "invoice-print";
    }

    @GetMapping("/admin/reports")
    public String revenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            Model model) {
        LocalDate effectiveStart = start == null ? LocalDate.now().withDayOfMonth(1) : start;
        LocalDate effectiveEnd = end == null ? LocalDate.now() : end;
        if (effectiveEnd.isBefore(effectiveStart)) {
            throw new IllegalArgumentException("Report end date cannot be before the start date.");
        }
        List<Invoice> invoices = billingService.getAllInvoices().stream()
                .filter(invoice -> invoice.getCreatedAt() != null)
                .filter(invoice -> !invoice.getCreatedAt().toLocalDate().isBefore(effectiveStart))
                .filter(invoice -> !invoice.getCreatedAt().toLocalDate().isAfter(effectiveEnd))
                .toList();
        Map<String, Double> byBranch = new LinkedHashMap<>();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Invoice invoice : invoices) {
            String branch = invoice.getBranchId() == null ? "Unassigned / legacy" : invoice.getBranchId();
            byBranch.merge(branch, invoice.getRecognizedRevenue(), Double::sum);
            byStatus.merge(invoice.getStatus().name(), 1L, Long::sum);
        }
        model.addAttribute("start", effectiveStart);
        model.addAttribute("end", effectiveEnd);
        model.addAttribute("invoices", invoices);
        model.addAttribute("byBranch", byBranch);
        model.addAttribute("byStatus", byStatus);
        model.addAttribute("grossTotal", invoices.stream().mapToDouble(Invoice::getAmountBeforeRefund).sum());
        model.addAttribute("refundTotal", invoices.stream().mapToDouble(i -> safe(i.getRefundAmount())).sum());
        model.addAttribute("netRevenue", invoices.stream().mapToDouble(Invoice::getRecognizedRevenue).sum());
        return "revenue-report";
    }

    private void addInvoice(Model model, Long id) {
        Invoice invoice = findInvoice(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("paymentLabel", billingService.getSafePaymentLabel(invoice));
    }

    private Invoice findInvoice(Long id) {
        return billingService.getInvoiceById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
    }

    private static double safe(Double value) {
        return value == null ? 0.0 : value;
    }
}
