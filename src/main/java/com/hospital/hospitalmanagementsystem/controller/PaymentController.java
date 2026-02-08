//package com.hospital.hospitalmanagementsystem.controller;
//
//import com.hospital.hospitalmanagementsystem.model.*;
//import com.hospital.hospitalmanagementsystem.service.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.multipart.MultipartFile;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//
//import java.math.BigDecimal;
//import java.security.Principal;
//import java.util.List;
//import java.util.Optional;
//
//@Controller
//public class PaymentController {
//
//    @Autowired
//    private PaymentService paymentService;
//
//    @Autowired
//    private InvoiceService invoiceService;
//
//    @Autowired
//    private PatientService patientService;
//
//    @Autowired
//    private UserService userService;
//
//    @Autowired
//    private PaymentPlanService paymentPlanService;
//
//    private Patient getPatientFromPrincipal(Principal principal) {
//        try {
//            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
//            if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_PATIENT) {
//                Optional<Patient> patientOpt = patientService.getPatientById(userOpt.get().getId());
//                if (patientOpt.isPresent()) {
//                    return patientOpt.get();
//                }
//            }
//
//            List<Patient> allPatients = patientService.getAllPatients();
//            return allPatients.stream()
//                    .filter(p -> p.getEmail() != null && p.getEmail().equals(principal.getName()))
//                    .findFirst()
//                    .orElseThrow(() -> new RuntimeException("Patient account not found"));
//        } catch (Exception e) {
//            throw new RuntimeException("Unable to access patient account", e);
//        }
//    }
//
//    @GetMapping("/patient/invoices/{invoiceId}/pay")
//    @PreAuthorize("hasRole('PATIENT')")
//    public String showPaymentMethods(@PathVariable Long invoiceId, Model model, Principal principal) {
//        try {
//            Invoice invoice = invoiceService.getInvoiceById(invoiceId)
//                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
//            Patient patient = getPatientFromPrincipal(principal);
//
//            validatePatientOwnership(invoice, patient);
//
//            if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
//                model.addAttribute("message", "This invoice has already been paid.");
//                return "redirect:/patient/invoices/" + invoiceId;
//            }
//
//            model.addAttribute("invoice", invoice);
//            model.addAttribute("patient", patient);
//            model.addAttribute("paymentAmount", invoice.getBalanceDue());
//            model.addAttribute("isInstallmentPayment", false);
//
//            return "home/billing/payment-method-selection";
//        } catch (Exception e) {
//            model.addAttribute("error", "Unable to load payment methods: " + e.getMessage());
//            return "home/error";
//        }
//    }
//
//    // FIXED: Added the missing GET mapping for card payment form
//    @GetMapping("/patient/invoices/{id}/pay/card")
//    @PreAuthorize("hasRole('PATIENT')")
//    public String showCardPaymentPage(@PathVariable Long id, Model model, Principal principal) {
//        try {
//            Invoice invoice = invoiceService.getInvoiceById(id)
//                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
//            Patient patient = getPatientFromPrincipal(principal);
//
//            validatePatientOwnership(invoice, patient);
//
//            model.addAttribute("invoice", invoice);
//            model.addAttribute("patient", patient);
//            model.addAttribute("paymentAmount", invoice.getBalanceDue());
//
//            return "home/billing/card-payment-portal";
//        } catch (Exception e) {
//            model.addAttribute("error", "Unable to load card payment: " + e.getMessage());
//            return "home/error";
//        }
//    }
//
//    // Card payment processing with proper database updates
//    @PostMapping("/patient/invoices/{id}/pay/card")
//    @PreAuthorize("hasRole('PATIENT')")
//    public String processCardPayment(@PathVariable Long id,
//                                     @RequestParam String cardNumber,
//                                     @RequestParam String cardName,
//                                     @RequestParam String cardExpiry,
//                                     @RequestParam String cardCvc,
//                                     @RequestParam String billingAddress,
//                                     @RequestParam String billingCity,
//                                     @RequestParam String billingZip,
//                                     RedirectAttributes redirectAttributes,
//                                     Principal principal) {
//        try {
//            // Log for debugging
//            System.out.println("Processing card payment for invoice ID: " + id);
//
//            Invoice invoice = invoiceService.getInvoiceById(id)
//                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
//            Patient patient = getPatientFromPrincipal(principal);
//
//            validatePatientOwnership(invoice, patient);
//
//            // Create payment request
//            Payment.CreateRequest request = new Payment.CreateRequest();
//            request.setInvoiceId(id);
//            request.setAmount(invoice.getBalanceDue());
//            request.setPaymentMethod("CREDIT_CARD");
//            request.setCardNumber(cardNumber);
//            request.setCardName(cardName);
//            request.setCardExpiry(cardExpiry);
//            request.setCardCvc(cardCvc);
//
//            // Process payment - this will update database
//            Payment payment = paymentService.processCardPayment(request);
//
//            // Log payment status
//            System.out.println("Payment processed with status: " + payment.getStatus());
//            System.out.println("Payment ID: " + payment.getId());
//
//            if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
//                redirectAttributes.addFlashAttribute("success", "Payment processed successfully!");
//                return "redirect:/patient/invoices/" + id + "/payment-success?paymentId=" + payment.getId();
//            } else {
//                redirectAttributes.addFlashAttribute("error", "Payment failed. Please try again.");
//                return "redirect:/patient/invoices/" + id + "/pay/card";
//            }
//        } catch (Exception e) {
//            e.printStackTrace(); // Add logging
//            redirectAttributes.addFlashAttribute("error", "Payment error: " + e.getMessage());
//            return "redirect:/patient/invoices/" + id + "/pay/card";
//        }
//    }
//    // Added GET mapping for bank transfer form
//    @GetMapping("/patient/invoices/{id}/pay/bank-transfer")
//    @PreAuthorize("hasRole('PATIENT')")
//    public String showBankTransferPage(@PathVariable Long id, Model model, Principal principal) {
//        try {
//            Invoice invoice = invoiceService.getInvoiceById(id)
//                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
//            Patient patient = getPatientFromPrincipal(principal);
//
//            validatePatientOwnership(invoice, patient);
//
//            model.addAttribute("invoice", invoice);
//            model.addAttribute("patient", patient);
//
//            return "home/billing/bank-transfer-payment";
//        } catch (Exception e) {
//            model.addAttribute("error", "Unable to load bank transfer: " + e.getMessage());
//            return "home/error";
//        }
//    }
//
//    // Bank transfer with proper form parameters matching the HTML form
//    @PostMapping("/patient/invoices/{id}/pay/bank-transfer")
//    @PreAuthorize("hasRole('PATIENT')")
//    public String processBankTransferPayment(@PathVariable Long id,
//                                             @RequestParam(required = false) MultipartFile receiptFile,
//                                             @RequestParam(required = false) String transferNotes,
//                                             RedirectAttributes redirectAttributes,
//                                             Principal principal) {
//        try {
//            Invoice invoice = invoiceService.getInvoiceById(id)
//                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
//            Patient patient = getPatientFromPrincipal(principal);
//
//            validatePatientOwnership(invoice, patient);
//
//            // Use invoice number as reference
//            String referenceNumber = invoice.getInvoiceNumber();
//            BigDecimal amount = invoice.getBalanceDue();
//
//            // Process bank transfer - saves to database with PENDING status
//            Payment payment = paymentService.processBankTransferPayment(
//                    id, amount, referenceNumber, receiptFile, transferNotes);
//
//            return "redirect:/patient/invoices/" + id + "/bank-transfer-pending?paymentId=" + payment.getId();
//
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
//            return "redirect:/patient/invoices/" + id + "/pay/bank-transfer";
//        }
//    }
//
//    @GetMapping("/patient/invoices/{id}/bank-transfer-pending")
//    @PreAuthorize("hasRole('PATIENT')")
//    public String showBankTransferPending(@PathVariable Long id,
//                                          @RequestParam Long paymentId,
//                                          Model model,
//                                          Principal principal) {
//        try {
//            Payment payment = paymentService.getPaymentById(paymentId);
//            Invoice invoice = payment.getInvoice();
//            Patient patient = getPatientFromPrincipal(principal);
//
//            if (!payment.getPatient().getId().equals(patient.getId())) {
//                throw new RuntimeException("Access denied");
//            }
//
//            model.addAttribute("payment", payment);
//            model.addAttribute("invoice", invoice);
//            model.addAttribute("patient", patient);
//
//            return "home/billing/bank-transfer-pending";
//        } catch (Exception e) {
//            model.addAttribute("error", "Unable to load: " + e.getMessage());
//            return "home/error";
//        }
//    }
//
//    @GetMapping("/patient/invoices/{id}/payment-success")
//    @PreAuthorize("hasRole('PATIENT')")
//    public String showPaymentSuccess(@PathVariable Long id,
//                                     @RequestParam Long paymentId,
//                                     Model model,
//                                     Principal principal) {
//        try {
//            Payment payment = paymentService.getPaymentById(paymentId);
//            Invoice invoice = payment.getInvoice();
//            Patient patient = getPatientFromPrincipal(principal);
//
//            if (!payment.getPatient().getId().equals(patient.getId())) {
//                throw new RuntimeException("Access denied");
//            }
//
//            model.addAttribute("payment", payment);
//            model.addAttribute("invoice", invoice);
//            model.addAttribute("patient", patient);
//
//            return "home/billing/payment-success";
//        } catch (Exception e) {
//            model.addAttribute("error", "Unable to load: " + e.getMessage());
//            return "home/error";
//        }
//    }
//
//    @GetMapping("/patient/invoices/{id}/receipt")
//    @PreAuthorize("hasRole('PATIENT')")
//    public String showReceipt(@PathVariable Long id,
//                              @RequestParam(required = false) Long paymentId,
//                              Model model,
//                              Principal principal) {
//        try {
//            Invoice invoice = invoiceService.getInvoiceById(id)
//                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
//            Patient patient = getPatientFromPrincipal(principal);
//
//            validatePatientOwnership(invoice, patient);
//
//            Payment payment = null;
//            if (paymentId != null) {
//                payment = paymentService.getPaymentById(paymentId);
//            } else {
//                List<Payment> payments = paymentService.getPaymentsByInvoiceId(id);
//                payment = payments.stream()
//                        .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
//                        .findFirst()
//                        .orElse(null);
//            }
//
//            if (payment == null) {
//                model.addAttribute("error", "No payment found");
//                return "home/error";
//            }
//
//            model.addAttribute("payment", payment);
//            model.addAttribute("invoice", invoice);
//            model.addAttribute("patient", patient);
//
//            return "home/billing/receipt-generator";
//        } catch (Exception e) {
//            model.addAttribute("error", "Unable to load receipt: " + e.getMessage());
//            return "home/error";
//        }
//    }
//
//    // Payment plan endpoints
//    @GetMapping("/patient/invoices/{id}/setup-payment-plan")
//    @PreAuthorize("hasRole('PATIENT')")
//    public String showPaymentPlanSetup(@PathVariable Long id, Model model, Principal principal) {
//        try {
//            Invoice invoice = invoiceService.getInvoiceById(id)
//                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
//            Patient patient = getPatientFromPrincipal(principal);
//
//            validatePatientOwnership(invoice, patient);
//
//            model.addAttribute("invoice", invoice);
//            model.addAttribute("patient", patient);
//
//            return "home/billing/payment-plan-setup";
//        } catch (Exception e) {
//            model.addAttribute("error", "Unable to load: " + e.getMessage());
//            return "home/error";
//        }
//    }
//
//    @PostMapping("/patient/invoices/{id}/setup-payment-plan")
//    @PreAuthorize("hasRole('PATIENT')")
//    public String createPaymentPlan(@PathVariable Long id,
//                                    @RequestParam String planType,
//                                    @RequestParam String paymentMethod,
//                                    @RequestParam(required = false) Integer customMonths,
//                                    RedirectAttributes redirectAttributes,
//                                    Principal principal) {
//        try {
//            Invoice invoice = invoiceService.getInvoiceById(id)
//                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
//            Patient patient = getPatientFromPrincipal(principal);
//
//            validatePatientOwnership(invoice, patient);
//
//            PaymentPlan.CreateRequest request = new PaymentPlan.CreateRequest();
//
//            if ("custom".equals(planType)) {
//                request.setNumberOfInstallments(customMonths);
//            } else {
//                request.setNumberOfInstallments(Integer.parseInt(planType));
//            }
//
//            request.setStartDate(java.time.LocalDate.now().plusMonths(1));
//            request.setPaymentMethod(paymentMethod);
//
//            PaymentPlan paymentPlan = paymentPlanService.createPaymentPlan(id, request);
//
//            return "redirect:/patient/invoices/" + id + "/payment-plan-confirmation?planId=" + paymentPlan.getId();
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
//            return "redirect:/patient/invoices/" + id + "/setup-payment-plan";
//        }
//    }
//
//    @GetMapping("/patient/invoices/{id}/payment-plan-confirmation")
//    @PreAuthorize("hasRole('PATIENT')")
//    public String showPaymentPlanConfirmation(@PathVariable Long id,
//                                              @RequestParam Long planId,
//                                              Model model,
//                                              Principal principal) {
//        try {
//            Invoice invoice = invoiceService.getInvoiceById(id)
//                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
//            Patient patient = getPatientFromPrincipal(principal);
//
//            validatePatientOwnership(invoice, patient);
//
//            PaymentPlan paymentPlan = paymentPlanService.getPaymentPlanById(planId)
//                    .orElseThrow(() -> new RuntimeException("Payment plan not found"));
//
//            model.addAttribute("invoice", invoice);
//            model.addAttribute("paymentPlan", paymentPlan);
//            model.addAttribute("patient", patient);
//
//            return "home/billing/payment-plan-confirmation";
//        } catch (Exception e) {
//            model.addAttribute("error", "Unable to load: " + e.getMessage());
//            return "home/error";
//        }
//    }
//
//    private void validatePatientOwnership(Invoice invoice, Patient patient) {
//        if (!invoice.getPatient().getId().equals(patient.getId())) {
//            throw new RuntimeException("Access denied");
//        }
//    }
//}

package com.hospital.hospitalmanagementsystem.controller;

import com.hospital.hospitalmanagementsystem.model.*;
import com.hospital.hospitalmanagementsystem.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private UserService userService;

    @Autowired
    private PaymentPlanService paymentPlanService;

    private Patient getPatientFromPrincipal(Principal principal) {
        try {
            Optional<User> userOpt = userService.getUserByUsername(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getRole() == User.Role.ROLE_PATIENT) {
                Optional<Patient> patientOpt = patientService.getPatientById(userOpt.get().getId());
                if (patientOpt.isPresent()) {
                    return patientOpt.get();
                }
            }

            List<Patient> allPatients = patientService.getAllPatients();
            return allPatients.stream()
                    .filter(p -> p.getEmail() != null && p.getEmail().equals(principal.getName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Patient account not found"));
        } catch (Exception e) {
            throw new RuntimeException("Unable to access patient account", e);
        }
    }

//    @GetMapping("/patient/invoices/{invoiceId}/pay")
//    @PreAuthorize("hasRole('PATIENT')")
//    public String showPaymentMethods(@PathVariable Long invoiceId, Model model, Principal principal) {
//        try {
//            Invoice invoice = invoiceService.getInvoiceById(invoiceId)
//                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
//            Patient patient = getPatientFromPrincipal(principal);
//
//            validatePatientOwnership(invoice, patient);
//
//            if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
//                model.addAttribute("message", "This invoice has already been paid.");
//                return "redirect:/patient/invoices/" + invoiceId;
//            }
//
//            model.addAttribute("invoice", invoice);
//            model.addAttribute("patient", patient);
//            model.addAttribute("paymentAmount", invoice.getBalanceDue());
//            model.addAttribute("isInstallmentPayment", false);
//
//            return "home/billing/payment-method-selection";
//        } catch (Exception e) {
//            model.addAttribute("error", "Unable to load payment methods: " + e.getMessage());
//            return "home/error";
//        }
//    }

    @GetMapping("/patient/invoices/{invoiceId}/pay")
    @PreAuthorize("hasRole('PATIENT')")
    public String showPaymentMethods(@PathVariable Long invoiceId, Model model, Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(invoiceId)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
            Patient patient = getPatientFromPrincipal(principal);

            validatePatientOwnership(invoice, patient);

            if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
                model.addAttribute("message", "This invoice has already been paid.");
                return "redirect:/patient/invoices/" + invoiceId;
            }

            model.addAttribute("invoice", invoice);
            model.addAttribute("patient", patient);
            model.addAttribute("paymentAmount", invoice.getBalanceDue());
            model.addAttribute("isInstallmentPayment", false);

            return "home/billing/payment-method-selection";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load payment methods: " + e.getMessage());
            return "home/error";
        }
    }

    @GetMapping("/patient/invoices/{id}/pay/card")
    @PreAuthorize("hasRole('PATIENT')")
    public String showCardPaymentPage(@PathVariable Long id, Model model, Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
            Patient patient = getPatientFromPrincipal(principal);

            validatePatientOwnership(invoice, patient);

            model.addAttribute("invoice", invoice);
            model.addAttribute("patient", patient);
            model.addAttribute("paymentAmount", invoice.getBalanceDue());

            return "home/billing/card-payment-portal";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load card payment: " + e.getMessage());
            return "home/error";
        }
    }

    @PostMapping("/patient/invoices/{id}/pay/card")
    @PreAuthorize("hasRole('PATIENT')")
    public String processCardPayment(@PathVariable Long id,
                                     @RequestParam String cardNumber,
                                     @RequestParam String cardName,
                                     @RequestParam String cardExpiry,
                                     @RequestParam String cardCvc,
                                     @RequestParam String billingAddress,
                                     @RequestParam String billingCity,
                                     @RequestParam String billingZip,
                                     RedirectAttributes redirectAttributes,
                                     Principal principal) {
        try {
            System.out.println("Processing card payment for invoice ID: " + id);

            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
            Patient patient = getPatientFromPrincipal(principal);

            validatePatientOwnership(invoice, patient);

            Payment.CreateRequest request = new Payment.CreateRequest();
            request.setInvoiceId(id);
            request.setAmount(invoice.getBalanceDue());
            request.setPaymentMethod("CREDIT_CARD");
            request.setCardNumber(cardNumber);
            request.setCardName(cardName);
            request.setCardExpiry(cardExpiry);
            request.setCardCvc(cardCvc);

            Payment payment = paymentService.processCardPayment(request);

            System.out.println("Payment processed with status: " + payment.getStatus());
            System.out.println("Payment ID: " + payment.getId());

            if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
                redirectAttributes.addFlashAttribute("success", "Payment processed successfully!");
                return "redirect:/patient/invoices/" + id + "/payment-success?paymentId=" + payment.getId();
            } else {
                redirectAttributes.addFlashAttribute("error", "Payment failed. Please try again.");
                return "redirect:/patient/invoices/" + id + "/pay/card";
            }
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Payment error: " + e.getMessage());
            return "redirect:/patient/invoices/" + id + "/pay/card";
        }
    }

    @GetMapping("/patient/invoices/{id}/pay/bank-transfer")
    @PreAuthorize("hasRole('PATIENT')")
    public String showBankTransferPage(@PathVariable Long id, Model model, Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
            Patient patient = getPatientFromPrincipal(principal);

            validatePatientOwnership(invoice, patient);

            model.addAttribute("invoice", invoice);
            model.addAttribute("patient", patient);

            return "home/billing/bank-transfer-payment";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load bank transfer: " + e.getMessage());
            return "home/error";
        }
    }

    @PostMapping("/patient/invoices/{id}/pay/bank-transfer")
    @PreAuthorize("hasRole('PATIENT')")
    public String processBankTransferPayment(@PathVariable Long id,
                                             @RequestParam(required = false) MultipartFile receiptFile,
                                             @RequestParam(required = false) String transferNotes,
                                             RedirectAttributes redirectAttributes,
                                             Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
            Patient patient = getPatientFromPrincipal(principal);

            validatePatientOwnership(invoice, patient);

            // CRITICAL VALIDATION: Check if file was uploaded
            if (receiptFile == null || receiptFile.isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "Please upload your bank transfer receipt");
                return "redirect:/patient/invoices/" + id + "/pay/bank-transfer";
            }

            // Validate file size (5MB max)
            long maxSize = 5 * 1024 * 1024; // 5MB
            if (receiptFile.getSize() > maxSize) {
                redirectAttributes.addFlashAttribute("error",
                        "File size must be less than 5MB. Your file: " +
                                String.format("%.2f MB", receiptFile.getSize() / (1024.0 * 1024.0)));
                return "redirect:/patient/invoices/" + id + "/pay/bank-transfer";
            }

            // Validate file type
            String contentType = receiptFile.getContentType();
            if (contentType == null ||
                    (!contentType.equals("application/pdf") &&
                            !contentType.startsWith("image/"))) {
                redirectAttributes.addFlashAttribute("error",
                        "Only PDF, JPG, and PNG files are allowed. Your file type: " + contentType);
                return "redirect:/patient/invoices/" + id + "/pay/bank-transfer";
            }

            // Validate file extension
            String fileName = receiptFile.getOriginalFilename();
            if (fileName != null) {
                String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();
                if (!extension.matches("\\.(pdf|jpg|jpeg|png)")) {
                    redirectAttributes.addFlashAttribute("error",
                            "Only PDF, JPG, and PNG files are allowed");
                    return "redirect:/patient/invoices/" + id + "/pay/bank-transfer";
                }
            }

            String referenceNumber = invoice.getInvoiceNumber();
            BigDecimal amount = invoice.getBalanceDue();

            // Process bank transfer - saves to database with PENDING status
            Payment payment = paymentService.processBankTransferPayment(
                    id, amount, referenceNumber, receiptFile, transferNotes);

            redirectAttributes.addFlashAttribute("success",
                    "Bank transfer receipt uploaded successfully! Your payment is pending verification.");

            return "redirect:/patient/invoices/" + id + "/bank-transfer-pending?paymentId=" + payment.getId();

        } catch (Exception e) {
            System.err.println("Error processing bank transfer: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error",
                    "Error processing your payment: " + e.getMessage());
            return "redirect:/patient/invoices/" + id + "/pay/bank-transfer";
        }
    }

    @GetMapping("/patient/invoices/{id}/bank-transfer-pending")
    @PreAuthorize("hasRole('PATIENT')")
    public String showBankTransferPending(@PathVariable Long id,
                                          @RequestParam Long paymentId,
                                          Model model,
                                          Principal principal) {
        try {
            Payment payment = paymentService.getPaymentById(paymentId);
            Invoice invoice = payment.getInvoice();
            Patient patient = getPatientFromPrincipal(principal);

            if (!payment.getPatient().getId().equals(patient.getId())) {
                throw new RuntimeException("Access denied");
            }

            model.addAttribute("payment", payment);
            model.addAttribute("invoice", invoice);
            model.addAttribute("patient", patient);

            return "home/billing/bank-transfer-pending";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load: " + e.getMessage());
            return "home/error";
        }
    }

    // UPDATED: Payment success page with proper null checks
    @GetMapping("/patient/invoices/{id}/payment-success")
    @PreAuthorize("hasRole('PATIENT')")
    public String showPaymentSuccess(@PathVariable Long id,
                                     @RequestParam Long paymentId,
                                     Model model,
                                     Principal principal) {
        try {
            Payment payment = paymentService.getPaymentById(paymentId);
            Invoice invoice = payment.getInvoice();
            Patient patient = getPatientFromPrincipal(principal);

            if (!payment.getPatient().getId().equals(patient.getId())) {
                throw new RuntimeException("Access denied");
            }

            model.addAttribute("payment", payment);
            model.addAttribute("invoice", invoice);
            model.addAttribute("patient", patient);

            return "home/billing/payment-success";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load: " + e.getMessage());
            return "home/error";
        }
    }

    // UPDATED: Receipt page using receipt-generator.html
    @GetMapping("/patient/invoices/{id}/receipt")
    @PreAuthorize("hasRole('PATIENT')")
    public String showReceipt(@PathVariable Long id,
                              @RequestParam(required = false) Long paymentId,
                              Model model,
                              Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
            Patient patient = getPatientFromPrincipal(principal);

            validatePatientOwnership(invoice, patient);

            Payment payment = null;
            if (paymentId != null) {
                payment = paymentService.getPaymentById(paymentId);
            } else {
                List<Payment> payments = paymentService.getPaymentsByInvoiceId(id);
                payment = payments.stream()
                        .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                        .findFirst()
                        .orElse(null);
            }

            if (payment == null) {
                model.addAttribute("error", "No payment found");
                return "home/error";
            }

            model.addAttribute("payment", payment);
            model.addAttribute("invoice", invoice);
            model.addAttribute("patient", patient);

            return "home/billing/receipt-generator";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load receipt: " + e.getMessage());
            return "home/error";
        }
    }

    @GetMapping("/patient/invoices/{id}/setup-payment-plan")
    @PreAuthorize("hasRole('PATIENT')")
    public String showPaymentPlanSetup(@PathVariable Long id, Model model, Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
            Patient patient = getPatientFromPrincipal(principal);

            validatePatientOwnership(invoice, patient);

            model.addAttribute("invoice", invoice);
            model.addAttribute("patient", patient);

            return "home/billing/payment-plan-setup";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load: " + e.getMessage());
            return "home/error";
        }
    }

    @PostMapping("/patient/invoices/{id}/setup-payment-plan")
    @PreAuthorize("hasRole('PATIENT')")
    public String createPaymentPlan(@PathVariable Long id,
                                    @RequestParam String planType,
                                    @RequestParam String paymentMethod,
                                    @RequestParam(required = false) Integer customMonths,
                                    RedirectAttributes redirectAttributes,
                                    Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
            Patient patient = getPatientFromPrincipal(principal);

            validatePatientOwnership(invoice, patient);

            PaymentPlan.CreateRequest request = new PaymentPlan.CreateRequest();

            if ("custom".equals(planType)) {
                request.setNumberOfInstallments(customMonths);
            } else {
                request.setNumberOfInstallments(Integer.parseInt(planType));
            }

            request.setStartDate(java.time.LocalDate.now().plusMonths(1));
            request.setPaymentMethod(paymentMethod);

            PaymentPlan paymentPlan = paymentPlanService.createPaymentPlan(id, request);

            return "redirect:/patient/invoices/" + id + "/payment-plan-confirmation?planId=" + paymentPlan.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/patient/invoices/" + id + "/setup-payment-plan";
        }
    }

    @GetMapping("/patient/invoices/{id}/payment-plan-confirmation")
    @PreAuthorize("hasRole('PATIENT')")
    public String showPaymentPlanConfirmation(@PathVariable Long id,
                                              @RequestParam Long planId,
                                              Model model,
                                              Principal principal) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(id)
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
            Patient patient = getPatientFromPrincipal(principal);

            validatePatientOwnership(invoice, patient);

            PaymentPlan paymentPlan = paymentPlanService.getPaymentPlanById(planId)
                    .orElseThrow(() -> new RuntimeException("Payment plan not found"));

            model.addAttribute("invoice", invoice);
            model.addAttribute("paymentPlan", paymentPlan);
            model.addAttribute("patient", patient);

            return "home/billing/payment-plan-confirmation";
        } catch (Exception e) {
            model.addAttribute("error", "Unable to load: " + e.getMessage());
            return "home/error";
        }
    }

    private void validatePatientOwnership(Invoice invoice, Patient patient) {
        if (!invoice.getPatient().getId().equals(patient.getId())) {
            throw new RuntimeException("Access denied");
        }
    }
}