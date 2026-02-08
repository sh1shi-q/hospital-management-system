//package com.hospital.hospitalmanagementsystem.service;
//
//import com.hospital.hospitalmanagementsystem.model.Invoice;
//import com.hospital.hospitalmanagementsystem.model.Patient;
//import com.hospital.hospitalmanagementsystem.repository.InvoiceRepository;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//import java.util.stream.Collectors;
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.stream.Collectors;
//import java.util.Objects;
//
///**
// * Service for managing invoices
// *
// * Current User's Login: IT24102083
// * Current Date and Time (UTC - YYYY-MM-DD HH:MM:SS formatted): 2025-08-11 05:00:22
// */
//@Service
//public class InvoiceService {
//
//    private final InvoiceRepository invoiceRepository;
//
//    public InvoiceService(InvoiceRepository invoiceRepository) {
//        this.invoiceRepository = invoiceRepository;
//    }
//
//    public Invoice saveInvoice(Invoice invoice) {
//        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isEmpty()) {
//            invoice.setInvoiceNumber(generateInvoiceNumber());
//        }
//        return invoiceRepository.save(invoice);
//    }
//
//    public List<Invoice> getAllInvoices() {
//        return invoiceRepository.findAll();
//    }
//
//    public Optional<Invoice> getInvoiceById(Long id) {
//        return invoiceRepository.findById(id);
//    }
//
//    public List<Invoice> getPatientInvoices(Patient patient) {
//        return invoiceRepository.findByPatient(patient);
//    }
//
//    public List<Invoice> getInvoicesByStatus(Invoice.InvoiceStatus status) {
//        return invoiceRepository.findByStatus(status);
//    }
//
//    public Invoice getInvoiceByNumber(String invoiceNumber) {
//        return invoiceRepository.findByInvoiceNumber(invoiceNumber);
//    }
//
//    public void deleteInvoice(Long id) {
//        invoiceRepository.deleteById(id);
//    }
//
//    private String generateInvoiceNumber() {
//        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
//    }
//
//    public void updateInvoiceStatus(Long invoiceId, Invoice.InvoiceStatus newStatus) {
//        Invoice invoice = invoiceRepository.findById(invoiceId)
//                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));
//
//        invoice.setStatus(newStatus);
//        invoiceRepository.save(invoice);
//    }
//
//    /**
//     * Get recent invoices for a patient with limit
//     * @param patient the patient
//     * @param limit maximum number of invoices to return
//     * @return list of recent invoices
//     */
//    public List<Invoice> getRecentInvoices(Patient patient, int limit) {
//        // Using findByPatient and sorting in memory since repository method is missing
//        return invoiceRepository.findByPatient(patient).stream()
//                .sorted((i1, i2) -> i2.getIssueDate().compareTo(i1.getIssueDate()))
//                .limit(limit)
//                .collect(Collectors.toList());
//    }
//
//    public void applyPayment(Long invoiceId, BigDecimal paymentAmount) {
//        // 1. Find the invoice or throw an error if it doesn't exist
//        Invoice invoice = invoiceRepository.findById(invoiceId)
//                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));
//
//        // 2. Update the paid amount and balance due
//        BigDecimal newAmountPaid = invoice.getAmountPaid().add(paymentAmount);
//        BigDecimal newBalanceDue = invoice.getTotal().subtract(newAmountPaid);
//
//        invoice.setAmountPaid(newAmountPaid);
//        invoice.setBalanceDue(newBalanceDue);
//
//        // 3. Update the invoice status based on the new balance
//        if (newBalanceDue.compareTo(BigDecimal.ZERO) <= 0) {
//            invoice.setStatus(Invoice.InvoiceStatus.PAID);
//            invoice.setBalanceDue(BigDecimal.ZERO); // Ensure balance isn't negative
//        } else {
//            invoice.setStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
//        }
//
//        // 4. Save the updated invoice
//        invoiceRepository.save(invoice);
//    }
//
//    public BigDecimal getTotalOutstandingBalance() {
//        return invoiceRepository.findAll().stream()
//                // Filter for invoices that have a balance
//                .filter(invoice ->
//                        invoice.getStatus() == Invoice.InvoiceStatus.PENDING ||
//                                invoice.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID ||
//                                invoice.getStatus() == Invoice.InvoiceStatus.OVERDUE)
//
//                .map(Invoice::getBalanceDue)
//                .filter(Objects::nonNull)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//    }
//
//    public List<Invoice> getOverdueInvoices() {
//        LocalDate today = LocalDate.now();
//        return invoiceRepository.findAll().stream()
//                // Filter for unpaid invoices where the due date is before today
//                .filter(invoice ->
//                        (invoice.getStatus() == Invoice.InvoiceStatus.PENDING ||
//                                invoice.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID)
//                                && invoice.getDueDate().isBefore(today))
//                .collect(Collectors.toList());
//    }
//}

package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Patient;
import com.hospital.hospitalmanagementsystem.repository.InvoiceItemRepository;
import com.hospital.hospitalmanagementsystem.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private InvoiceItemRepository invoiceItemRepository;

    // ========== CRUD OPERATIONS ==========

    public Invoice createInvoice(Invoice invoice) {
        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isEmpty()) {
            invoice.setInvoiceNumber(generateInvoiceNumber());
        }

        // Calculate totals
        calculateInvoiceTotals(invoice);

        // Set initial status
        if (invoice.getStatus() == null) {
            invoice.setStatus(Invoice.InvoiceStatus.PENDING);
        }

        return invoiceRepository.save(invoice);
    }

    public Invoice saveInvoice(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    public Invoice updateInvoice(Long id, Invoice updatedInvoice) {
        Invoice existing = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + id));

        existing.setDescription(updatedInvoice.getDescription());
        existing.setDueDate(updatedInvoice.getDueDate());
        existing.setNotes(updatedInvoice.getNotes());

        // Recalculate if amounts changed
        if (!existing.getSubtotal().equals(updatedInvoice.getSubtotal())) {
            existing.setSubtotal(updatedInvoice.getSubtotal());
            existing.setTax(updatedInvoice.getTax());
            existing.setDiscount(updatedInvoice.getDiscount());
            calculateInvoiceTotals(existing);
        }

        return invoiceRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Optional<Invoice> getInvoiceById(Long id) {
        return invoiceRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Invoice> getAllInvoicesPaginated(Pageable pageable) {
        return invoiceRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getPatientInvoices(Patient patient) {
        return invoiceRepository.findByPatientOrderByIssueDateDesc(patient);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getRecentInvoices(Patient patient, int limit) {
        return invoiceRepository.findByPatientOrderByIssueDateDesc(patient).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public void deleteInvoice(Long id) {
        invoiceRepository.deleteById(id);
    }

    // ========== STATUS MANAGEMENT ==========

    public void updateInvoiceStatus(Long invoiceId, Invoice.InvoiceStatus newStatus) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

        invoice.setStatus(newStatus);
        invoiceRepository.save(invoice);
    }

    public void applyPayment(Long invoiceId, BigDecimal paymentAmount) {
        // 1. Find the invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with ID: " + invoiceId));

        // 2. Update the paid amount and balance due
        BigDecimal newAmountPaid = invoice.getAmountPaid().add(paymentAmount);
        BigDecimal newBalanceDue = invoice.getTotal().subtract(newAmountPaid);

        invoice.setAmountPaid(newAmountPaid);
        invoice.setBalanceDue(newBalanceDue.max(BigDecimal.ZERO));

        // 3. Update the invoice status
        if (newBalanceDue.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PAID);
        } else if (newAmountPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
        }

        // 4. Save the updated invoice
        invoiceRepository.save(invoice);
    }

    // ========== QUERIES ==========

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByStatus(Invoice.InvoiceStatus status) {
        return invoiceRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public Invoice getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getOverdueInvoices() {
        LocalDate today = LocalDate.now();
        return invoiceRepository.findAll().stream()
                .filter(invoice ->
                        (invoice.getStatus() == Invoice.InvoiceStatus.PENDING ||
                                invoice.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID ||
                                invoice.getStatus() == Invoice.InvoiceStatus.SENT) &&
                                invoice.getDueDate() != null &&
                                invoice.getDueDate().isBefore(today))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public int getPaidInvoicesCount() {
        return invoiceRepository.findByStatus(Invoice.InvoiceStatus.PAID).size();
    }

    // ========== STATISTICS ==========

    @Transactional(readOnly = true)
    public BigDecimal getTotalOutstandingBalance() {
        return invoiceRepository.findAll().stream()
                .filter(invoice ->
                        invoice.getStatus() == Invoice.InvoiceStatus.PENDING ||
                                invoice.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID ||
                                invoice.getStatus() == Invoice.InvoiceStatus.OVERDUE ||
                                invoice.getStatus() == Invoice.InvoiceStatus.SENT)
                .map(Invoice::getBalanceDue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getAgingReport() {
        Map<String, BigDecimal> agingReport = new HashMap<>();
        LocalDate today = LocalDate.now();

        List<Invoice> unpaidInvoices = invoiceRepository.findAll().stream()
                .filter(invoice -> invoice.getBalanceDue().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        BigDecimal current = BigDecimal.ZERO;
        BigDecimal thirtyDays = BigDecimal.ZERO;
        BigDecimal sixtyDays = BigDecimal.ZERO;
        BigDecimal ninetyDaysPlus = BigDecimal.ZERO;

        for (Invoice invoice : unpaidInvoices) {
            if (invoice.getDueDate() == null) continue;

            long daysPastDue = java.time.temporal.ChronoUnit.DAYS.between(invoice.getDueDate(), today);

            if (daysPastDue <= 0) {
                current = current.add(invoice.getBalanceDue());
            } else if (daysPastDue <= 30) {
                thirtyDays = thirtyDays.add(invoice.getBalanceDue());
            } else if (daysPastDue <= 60) {
                sixtyDays = sixtyDays.add(invoice.getBalanceDue());
            } else {
                ninetyDaysPlus = ninetyDaysPlus.add(invoice.getBalanceDue());
            }
        }

        agingReport.put("current", current);
        agingReport.put("30days", thirtyDays);
        agingReport.put("60days", sixtyDays);
        agingReport.put("90plus", ninetyDaysPlus);

        return agingReport;
    }

    // ========== HELPER METHODS ==========

    private void calculateInvoiceTotals(Invoice invoice) {
        if (invoice.getSubtotal() == null) {
            invoice.setSubtotal(BigDecimal.ZERO);
        }
        if (invoice.getTax() == null) {
            invoice.setTax(BigDecimal.ZERO);
        }
        if (invoice.getDiscount() == null) {
            invoice.setDiscount(BigDecimal.ZERO);
        }

        BigDecimal total = invoice.getSubtotal()
                .add(invoice.getTax())
                .subtract(invoice.getDiscount());

        invoice.setTotal(total);

        if (invoice.getAmountPaid() == null) {
            invoice.setAmountPaid(BigDecimal.ZERO);
        }

        BigDecimal balanceDue = total.subtract(invoice.getAmountPaid());
        invoice.setBalanceDue(balanceDue.max(BigDecimal.ZERO));
    }

    private String generateInvoiceNumber() {
        String prefix = "INV";
        String year = String.valueOf(LocalDate.now().getYear());
        String month = String.format("%02d", LocalDate.now().getMonthValue());
        String sequence = String.format("%05d", invoiceRepository.count() + 1);
        return prefix + "-" + year + "-" + month + sequence;
    }

}