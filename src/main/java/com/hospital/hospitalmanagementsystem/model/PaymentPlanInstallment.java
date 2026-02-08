//package com.hospital.hospitalmanagementsystem.model;
//
//import javax.persistence.*;
//import javax.validation.constraints.DecimalMin;
//import javax.validation.constraints.Min;
//import javax.validation.constraints.NotNull;
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "payment_plan_installments")
//public class PaymentPlanInstallment {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "payment_plan_id", nullable = false)
//    private PaymentPlan paymentPlan;
//
//    @NotNull
//    @Min(value = 1, message = "Installment number must be at least 1")
//    @Column(name = "installment_number")
//    private Integer installmentNumber;
//
//    @NotNull
//    @Column(name = "due_date")
//    private LocalDate dueDate;
//
//    @NotNull
//    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
//    @Column(precision = 10, scale = 2)
//    private BigDecimal amount;
//
//    @DecimalMin(value = "0.00", message = "Amount paid must be non-negative")
//    @Column(name = "amount_paid", precision = 10, scale = 2)
//    private BigDecimal amountPaid = BigDecimal.ZERO;
//
//    @Column(name = "payment_date")
//    private LocalDate paymentDate;
//
//    @Enumerated(EnumType.STRING)
//    private InstallmentStatus status;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "payment_id")
//    private Payment payment;
//
//    @Column(columnDefinition = "TEXT")
//    private String notes;
//
//    @Column(name = "reminder_sent")
//    private Boolean reminderSent = false;
//
//    @Column(name = "reminder_sent_date")
//    private LocalDateTime reminderSentDate;
//
//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//
//    // Constructors
//    public PaymentPlanInstallment() {}
//
//    public PaymentPlanInstallment(PaymentPlan paymentPlan, Integer installmentNumber,
//                                 LocalDate dueDate, BigDecimal amount) {
//        this.paymentPlan = paymentPlan;
//        this.installmentNumber = installmentNumber;
//        this.dueDate = dueDate;
//        this.amount = amount;
//        this.status = InstallmentStatus.PENDING;
//        this.reminderSent = false;
//    }
//
//    // Enums
//    public enum InstallmentStatus {
//        PENDING("Pending"),
//        PAID("Paid"),
//        OVERDUE("Overdue"),
//        PARTIAL("Partial"),
//        CANCELLED("Cancelled");
//
//        private final String displayName;
//
//        InstallmentStatus(String displayName) {
//            this.displayName = displayName;
//        }
//
//        public String getDisplayName() {
//            return displayName;
//        }
//    }
//
//    // Utility methods
//    public boolean isOverdue() {
//        return status != InstallmentStatus.PAID &&
//               status != InstallmentStatus.CANCELLED &&
//               dueDate.isBefore(LocalDate.now());
//    }
//
//    public boolean isDueToday() {
//        return dueDate.equals(LocalDate.now()) && status == InstallmentStatus.PENDING;
//    }
//
//    public boolean isDueSoon(int days) {
//        return dueDate.isBefore(LocalDate.now().plusDays(days + 1)) &&
//               dueDate.isAfter(LocalDate.now()) &&
//               status == InstallmentStatus.PENDING;
//    }
//
//    public BigDecimal getRemainingAmount() {
//        return amount.subtract(amountPaid);
//    }
//
//    public void markAsPaid(Payment payment, LocalDate paymentDate) {
//        this.payment = payment;
//        this.paymentDate = paymentDate;
//        this.amountPaid = this.amount;
//        this.status = InstallmentStatus.PAID;
//    }
//
//    public void recordPartialPayment(BigDecimal paidAmount, Payment payment, LocalDate paymentDate) {
//        this.payment = payment;
//        this.paymentDate = paymentDate;
//        this.amountPaid = this.amountPaid.add(paidAmount);
//
//        if (this.amountPaid.compareTo(amount) >= 0) {
//            this.status = InstallmentStatus.PAID;
//        } else {
//            this.status = InstallmentStatus.PARTIAL;
//        }
//    }
//
//    @PrePersist
//    protected void onCreate() {
//        createdAt = LocalDateTime.now();
//        updatedAt = LocalDateTime.now();
//    }
//
//    @PreUpdate
//    protected void onUpdate() {
//        updatedAt = LocalDateTime.now();
//
//        // Auto-update status based on current state
//        if (status != InstallmentStatus.CANCELLED) {
//            if (amountPaid.compareTo(amount) >= 0) {
//                status = InstallmentStatus.PAID;
//            } else if (amountPaid.compareTo(BigDecimal.ZERO) > 0) {
//                status = InstallmentStatus.PARTIAL;
//            } else if (isOverdue()) {
//                status = InstallmentStatus.OVERDUE;
//            }
//        }
//    }
//
//    // Getters and Setters
//    public Long getId() { return id; }
//    public void setId(Long id) { this.id = id; }
//
//    public PaymentPlan getPaymentPlan() { return paymentPlan; }
//    public void setPaymentPlan(PaymentPlan paymentPlan) { this.paymentPlan = paymentPlan; }
//
//    public Integer getInstallmentNumber() { return installmentNumber; }
//    public void setInstallmentNumber(Integer installmentNumber) { this.installmentNumber = installmentNumber; }
//
//    public LocalDate getDueDate() { return dueDate; }
//    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
//
//    public BigDecimal getAmount() { return amount; }
//    public void setAmount(BigDecimal amount) { this.amount = amount; }
//
//    public BigDecimal getAmountPaid() { return amountPaid; }
//    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
//
//    public LocalDate getPaymentDate() { return paymentDate; }
//    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
//
//    public InstallmentStatus getStatus() { return status; }
//    public void setStatus(InstallmentStatus status) { this.status = status; }
//
//    public Payment getPayment() { return payment; }
//    public void setPayment(Payment payment) { this.payment = payment; }
//
//    public String getNotes() { return notes; }
//    public void setNotes(String notes) { this.notes = notes; }
//
//    public Boolean getReminderSent() { return reminderSent; }
//    public void setReminderSent(Boolean reminderSent) { this.reminderSent = reminderSent; }
//
//    public LocalDateTime getReminderSentDate() { return reminderSentDate; }
//    public void setReminderSentDate(LocalDateTime reminderSentDate) { this.reminderSentDate = reminderSentDate; }
//
//    public LocalDateTime getCreatedAt() { return createdAt; }
//    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
//
//    public LocalDateTime getUpdatedAt() { return updatedAt; }
//    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
//}

package com.hospital.hospitalmanagementsystem.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_plan_installments")
public class PaymentPlanInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_plan_id", nullable = false)
    private PaymentPlan paymentPlan;

    @NotNull
    @Min(value = 1, message = "Installment number must be at least 1")
    @Column(name = "installment_number")
    private Integer installmentNumber;

    @NotNull
    @Column(name = "due_date")
    private LocalDate dueDate;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @DecimalMin(value = "0.00", message = "Amount paid must be non-negative")
    @Column(name = "amount_paid", precision = 10, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    private InstallmentStatus status = InstallmentStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;

    @Column(name = "reminder_sent_date")
    private LocalDateTime reminderSentDate;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum InstallmentStatus {
        PENDING("Pending"),
        PAID("Paid"),
        OVERDUE("Overdue"),
        PARTIAL("Partial"),
        CANCELLED("Cancelled");

        private final String displayName;

        InstallmentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public String toString() {
        return "PaymentPlanInstallment{" +
                "id=" + id +
                ", paymentPlanId=" + (paymentPlan != null ? paymentPlan.getId() : null) +
                ", installmentNumber=" + installmentNumber +
                ", dueDate=" + dueDate +
                ", amount=" + amount +
                ", amountPaid=" + amountPaid +
                ", paymentDate=" + paymentDate +
                ", status=" + status +
                ", paymentId=" + (payment != null ? payment.getId() : null) +
                ", notes='" + notes + '\'' +
                ", reminderSent=" + reminderSent +
                ", reminderSentDate=" + reminderSentDate +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}