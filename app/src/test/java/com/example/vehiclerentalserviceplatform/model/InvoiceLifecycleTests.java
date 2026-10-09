package com.example.vehiclerentalserviceplatform.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvoiceLifecycleTests {

    @Test
    void recordsPartialAndFullRefundsWithoutLosingHistory() {
        Invoice invoice = paidInvoice();
        invoice.recordRefund(2500, "Rate correction");
        assertEquals(InvoiceStatus.PARTIALLY_REFUNDED, invoice.getStatus());
        assertEquals(7500, invoice.getTotalAmount());

        invoice.recordRefund(7500, "Rental cancelled");
        assertEquals(InvoiceStatus.REFUNDED, invoice.getStatus());
        assertEquals(10000, invoice.getRefundAmount());
        assertEquals(0, invoice.getTotalAmount());
    }

    @Test
    void rejectsRefundAboveRemainingPaidAmount() {
        Invoice invoice = paidInvoice();
        invoice.recordRefund(2000, "Partial refund");
        assertThrows(IllegalArgumentException.class,
                () -> invoice.recordRefund(8001, "Too much"));
    }

    @Test
    void voidRetainsInvoiceButRemovesRecognizedRevenue() {
        Invoice invoice = paidInvoice();
        invoice.voidInvoice("Duplicate payment");
        assertEquals(InvoiceStatus.VOID, invoice.getStatus());
        assertEquals(0, invoice.getRecognizedRevenue());
        assertEquals("Duplicate payment", invoice.getVoidReason());
    }

    @Test
    void voidedInvoiceCannotBeRefunded() {
        Invoice invoice = paidInvoice();
        invoice.voidInvoice("Duplicate");
        assertThrows(IllegalStateException.class,
                () -> invoice.recordRefund(100, "Invalid refund"));
    }

    private Invoice paidInvoice() {
        Invoice invoice = new Invoice();
        invoice.setRentalDays(2);
        invoice.setAmountPerDay(5000.0);
        invoice.markPaid();
        return invoice;
    }
}
