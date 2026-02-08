package com.hospital.hospitalmanagementsystem.repository;

import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByPatient(Patient patient);

    List<Invoice> findByStatus(Invoice.InvoiceStatus status);

    Invoice findByInvoiceNumber(String invoiceNumber);

    // Add this method to support sorting by issueDate in the database
    List<Invoice> findByPatientOrderByIssueDateDesc(Patient patient);

}