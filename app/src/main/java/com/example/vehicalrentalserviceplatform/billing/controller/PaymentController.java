package com.example.vehicalrentalserviceplatform.billing.controller;

import com.example.vehicalrentalserviceplatform.billing.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping
    public String makePayment(@RequestParam Double amount,
                              @RequestParam String method) {
        return paymentService.processPayment(amount, method);
    }
}