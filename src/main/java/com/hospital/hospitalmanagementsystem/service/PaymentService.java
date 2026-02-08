package com.hospital.hospitalmanagementsystem.service;

import com.hospital.hospitalmanagementsystem.exception.PaymentProcessingException;
import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.repository.PaymentRepository;
import com.hospital.hospitalmanagementsystem.service.external.FileStorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.time.format.DateTimeFormatter;
import java.io.IOException;

@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private FileStorageService fileStorageService;

    // FIXED: Card payment with complete database updates
    @Transactional
    public Payment processCardPayment(Payment.CreateRequest request) {
        try {
            System.out.println("PaymentService: Processing card payment for invoice ID: " + request.getInvoiceId());

            Invoice invoice = invoiceService.getInvoiceById(request.getInvoiceId())
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));

            System.out.println("Invoice found - Balance Due: " + invoice.getBalanceDue());

            validatePaymentAmount(invoice, request.getAmount());

            // Create payment record
            Payment payment = createBasePayment(invoice, request.getAmount(), Payment.PaymentMethod.CREDIT_CARD);

            // Simulate card processing (90% success for demo)
            boolean paymentSuccess = simulateCardPayment(request);

            System.out.println("Payment simulation result: " + paymentSuccess);

            if (paymentSuccess) {
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.setNotes("Card payment processed successfully");

                // CRITICAL: Save payment first
                payment = paymentRepository.save(payment);
                System.out.println("Payment saved with ID: " + payment.getId());

                // CRITICAL: Update invoice with payment
                invoiceService.applyPayment(invoice.getId(), request.getAmount());
                System.out.println("Invoice updated");

                // Flush to ensure database update
                paymentRepository.flush();

                return payment;
            } else {
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.setNotes("Card payment failed");
                return paymentRepository.save(payment);
            }
        } catch (Exception e) {
            System.err.println("Error processing card payment: " + e.getMessage());
            e.printStackTrace();
            throw new PaymentProcessingException("Card payment failed: " + e.getMessage(), e);
        }
    }

    // FIXED: Bank transfer with complete database updates
    public Payment processBankTransferPayment(Long invoiceId, BigDecimal amount, String referenceNumber,
                                              MultipartFile receiptFile, String transferNotes) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));

            validatePaymentAmount(invoice, amount);

            // Create payment record
            Payment payment = createBasePayment(invoice, amount, Payment.PaymentMethod.BANK_TRANSFER);
            payment.setStatus(Payment.PaymentStatus.PENDING); // Needs verification
            payment.setReferenceNumber(referenceNumber);

            // Build notes
            StringBuilder notes = new StringBuilder("Bank transfer - Reference: " + referenceNumber);
            if (transferNotes != null && !transferNotes.isEmpty()) {
                notes.append(" | Notes: ").append(transferNotes);
            }

            // CRITICAL: Save payment FIRST to get the ID
            payment.setNotes(notes.toString());
            payment = paymentRepository.save(payment);

            // Store bank slip file AFTER saving payment (so we have the ID)
            if (receiptFile != null && !receiptFile.isEmpty()) {
                try {
                    String fileName = fileStorageService.storeBankSlip(receiptFile, payment.getId());
                    payment.setBankSlipPath(fileName);
                    payment.setNotes(payment.getNotes() + " | Bank slip uploaded: " + fileName);

                    // Update payment with file path
                    payment = paymentRepository.save(payment);

                    System.out.println("Bank slip saved for payment ID: " + payment.getId());

                } catch (IllegalArgumentException e) {
                    // Validation error from FileStorageService
                    payment.setNotes(payment.getNotes() + " | Bank slip upload failed: " + e.getMessage());
                    payment = paymentRepository.save(payment);
                    throw new RuntimeException("File validation failed: " + e.getMessage(), e);
                } catch (Exception e) {
                    // Other errors
                    payment.setNotes(payment.getNotes() + " | Bank slip upload failed: " + e.getMessage());
                    payment = paymentRepository.save(payment);
                    System.err.println("Failed to store bank slip: " + e.getMessage());
                }
            } else {
                // No file uploaded - this should be caught by controller validation
                payment.setNotes(payment.getNotes() + " | WARNING: No bank slip uploaded");
                payment = paymentRepository.save(payment);
            }

            return payment;

        } catch (Exception e) {
            throw new PaymentProcessingException("Bank transfer failed: " + e.getMessage(), e);
        }
    }

    // FIXED: Bank slip approval with database updates
    public Payment approveBankSlipPayment(Long paymentId, String verificationNotes) {
        Payment payment = getPaymentById(paymentId);

        if (payment.getPaymentMethod() != Payment.PaymentMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException("Payment is not a bank transfer");
        }

        // Update payment status
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setVerificationNotes(verificationNotes);
        payment.setVerificationDate(LocalDateTime.now());

        // CRITICAL: Save payment first
        payment = paymentRepository.save(payment);

        // CRITICAL: Update invoice
        if (payment.getInvoice() != null) {
            invoiceService.applyPayment(payment.getInvoice().getId(), payment.getAmount());
        }

        return payment;
    }

    public Payment rejectBankSlipPayment(Long paymentId, String rejectionReason) {
        Payment payment = getPaymentById(paymentId);

        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setVerificationNotes(rejectionReason);
        payment.setVerificationDate(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    public Payment approvePartialBankSlipPayment(Long paymentId, BigDecimal verifiedAmount, String verificationNotes) {
        Payment payment = getPaymentById(paymentId);

        if (verifiedAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Verified amount cannot exceed payment amount");
        }

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setAmount(verifiedAmount);
        payment.setVerificationNotes(verificationNotes);
        payment.setVerificationDate(LocalDateTime.now());

        // CRITICAL: Save payment first
        payment = paymentRepository.save(payment);

        // CRITICAL: Update invoice with partial payment
        if (payment.getInvoice() != null) {
            invoiceService.applyPayment(payment.getInvoice().getId(), verifiedAmount);
        }

        return payment;
    }

    @Transactional(readOnly = true)
    public List<Payment> getPendingBankSlipVerifications() {
        return paymentRepository.findByPaymentMethodAndStatus(
                Payment.PaymentMethod.BANK_TRANSFER,
                Payment.PaymentStatus.PENDING
        );
    }

    @Transactional(readOnly = true)
    public Page<Payment> getPendingBankSlipsPaginated(Pageable pageable) {
        return paymentRepository.findByPaymentMethodAndStatusOrderByPaymentDateDesc(
                Payment.PaymentMethod.BANK_TRANSFER,
                Payment.PaymentStatus.PENDING,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByInvoiceId(Long invoiceId) {
        return paymentRepository.findByInvoiceIdOrderByPaymentDateDesc(invoiceId);
    }

    @Transactional(readOnly = true)
    public List<Payment> getRecentPOSTransactions(int limit) {
        return paymentRepository.findTop10ByOrderByPaymentDateDesc();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        return paymentRepository.findByStatus(Payment.PaymentStatus.COMPLETED)
                .stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int sendBankSlipReminders() {
        List<Payment> pendingPayments = getPendingBankSlipVerifications();
        int remindersSent = 0;

        for (Payment payment : pendingPayments) {
            if (payment.getPaymentDate().isBefore(LocalDateTime.now().minusHours(24))) {
                try {
                    // Add reminder logic here
                    remindersSent++;
                } catch (Exception e) {
                    System.err.println("Failed to send reminder for payment " + payment.getId());
                }
            }
        }

        return remindersSent;
    }

    public void contactPatientRegardingBankSlip(Long paymentId, String contactReason, String message) {
        Payment payment = getPaymentById(paymentId);

        if (payment.getInvoice() != null && payment.getInvoice().getPatient() != null) {
            payment.setNotes(payment.getNotes() + " | Contact made: " + contactReason + " - " + message);
            paymentRepository.save(payment);
        }
    }

    private Payment createBasePayment(Invoice invoice, BigDecimal amount, Payment.PaymentMethod paymentMethod) {
        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setPatient(invoice.getPatient());
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionId(generateTransactionId());
        payment.setReceiptNumber(generateReceiptNumber()); // ADD THIS LINE
        payment.setPaymentDate(LocalDateTime.now());
        return payment;
    }

    private void validatePaymentAmount(Invoice invoice, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        if (amount.compareTo(invoice.getBalanceDue()) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed invoice balance");
        }
    }

    private String generateTransactionId() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String generateReceiptNumber() {
        return "RCP-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) +
                "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private boolean simulateCardPayment(Payment.CreateRequest request) {
        // 90% success rate for demo
        return Math.random() > 0.1;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    public Payment processPOSPayment(String customerType, String patientSearch, String customerName,
                                     String customerPhone, String paymentType, String invoiceNumber,
                                     String paymentDescription, BigDecimal amount, String paymentMethod) {
        try {
            Payment payment = new Payment();
            payment.setAmount(amount);
            payment.setPaymentMethod(Payment.PaymentMethod.valueOf(paymentMethod));
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setTransactionId(generateTransactionId());
            payment.setNotes("POS Payment: " + paymentDescription);
            payment.setPaymentDate(LocalDateTime.now());

            if (invoiceNumber != null && !invoiceNumber.isEmpty()) {
                try {
                    Invoice invoice = invoiceService.getInvoiceById(Long.valueOf(invoiceNumber))
                            .orElseThrow(() -> new RuntimeException("Invoice not found"));

                    payment.setInvoice(invoice);
                    payment.setPatient(invoice.getPatient());

                    invoiceService.applyPayment(invoice.getId(), amount);
                } catch (Exception e) {
                    payment.setNotes(payment.getNotes() + " | Customer: " + customerName);
                }
            }

            return paymentRepository.save(payment);
        } catch (Exception e) {
            throw new PaymentProcessingException("POS payment failed: " + e.getMessage(), e);
        }
    }

    public org.springframework.http.ResponseEntity<byte[]> downloadBankSlipFile(Long paymentId) {
        try {
            Payment payment = getPaymentById(paymentId);

            if (payment.getBankSlipPath() == null || payment.getBankSlipPath().isEmpty()) {
                return org.springframework.http.ResponseEntity.notFound().build();
            }

            byte[] fileData = fileStorageService.getBankSlipFile(payment.getBankSlipPath());
            String contentType = fileStorageService.getContentType(payment.getBankSlipPath());

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType(contentType));

            // For images, display inline; for PDFs, allow download
            if (contentType.startsWith("image/")) {
                headers.setContentDisposition(
                        org.springframework.http.ContentDisposition.inline()
                                .filename(payment.getBankSlipPath())
                                .build()
                );
            } else {
                headers.setContentDisposition(
                        org.springframework.http.ContentDisposition.attachment()
                                .filename(payment.getBankSlipPath())
                                .build()
                );
            }

            return org.springframework.http.ResponseEntity.ok()
                    .headers(headers)
                    .body(fileData);

        } catch (Exception e) {
            System.err.println("Error downloading bank slip: " + e.getMessage());
            return org.springframework.http.ResponseEntity.notFound().build();
        }
    }

    // Add this to your PaymentService class
    public ResponseEntity<byte[]> viewBankSlipFile(Long paymentId) throws IOException {
        try {
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

            if (payment.getBankSlipPath() == null || payment.getBankSlipPath().isEmpty()) {
                throw new RuntimeException("No bank slip found for this payment");
            }

            // Use FileStorageService (same as downloadBankSlipFile)
            byte[] fileData = fileStorageService.getBankSlipFile(payment.getBankSlipPath());
            String contentType = fileStorageService.getContentType(payment.getBankSlipPath());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(fileData.length);
            // INLINE instead of ATTACHMENT
            headers.setContentDisposition(
                    ContentDisposition.inline()
                            .filename(payment.getBankSlipPath())
                            .build()
            );

            return new ResponseEntity<>(fileData, headers, HttpStatus.OK);

        } catch (Exception e) {
            System.err.println("Error viewing bank slip: " + e.getMessage());
            throw new IOException("Failed to view bank slip: " + e.getMessage(), e);
        }
    }




}