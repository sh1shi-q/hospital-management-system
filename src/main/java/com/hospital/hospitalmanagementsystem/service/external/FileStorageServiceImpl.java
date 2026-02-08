package com.hospital.hospitalmanagementsystem.service.external;

import com.hospital.hospitalmanagementsystem.model.Invoice;
import com.hospital.hospitalmanagementsystem.model.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${file.upload-dir:uploads/bank-slips/}")
    private String UPLOAD_DIR;

    @Override
    public String storeBankSlip(MultipartFile file, Long paymentId) {
        try {
            // Validate file before storing
            validateFile(file);

            // Create directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename with proper extension
            String originalExtension = getFileExtension(file.getOriginalFilename());
            String fileName = "bankslip_" + paymentId + "_" +
                    UUID.randomUUID().toString() + originalExtension;

            Path filePath = uploadPath.resolve(fileName);

            // Store file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Log success
            System.out.println("Bank slip stored successfully: " + fileName);

            return fileName;

        } catch (IllegalArgumentException e) {
            // Re-throw validation errors
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store bank slip file: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] getBankSlipFile(String fileName) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName);

            // Check if file exists
            if (!Files.exists(filePath)) {
                throw new RuntimeException("Bank slip file not found: " + fileName);
            }

            // Check if it's a regular file (not a directory)
            if (!Files.isRegularFile(filePath)) {
                throw new RuntimeException("Invalid file: " + fileName);
            }

            return Files.readAllBytes(filePath);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read bank slip file: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] generateInvoicePDF(Invoice invoice) {
        // Simplified PDF generation - you would use a library like iText or PDFBox
        String pdfContent = "Invoice PDF for " + invoice.getInvoiceNumber();
        return pdfContent.getBytes();
    }

    @Override
    public byte[] generateReceiptPDF(Payment payment) {
        // Simplified PDF generation - you would use a library like iText or PDFBox
        String pdfContent = "Receipt PDF for " + payment.getTransactionId();
        return pdfContent.getBytes();
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return "";
    }

    private void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        // Check file size (5MB max)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }

        // Check file type
        String contentType = file.getContentType();
        if (contentType == null ||
                (!contentType.equals("application/pdf") &&
                        !contentType.startsWith("image/"))) {
            throw new IllegalArgumentException("Only PDF, JPG, and PNG files are allowed");
        }

        // Check file extension
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("File name is required");
        }

        String extension = getFileExtension(fileName).toLowerCase();
        if (!extension.matches("\\.(pdf|jpg|jpeg|png)")) {
            throw new IllegalArgumentException("Only PDF, JPG, and PNG files are allowed");
        }
    }

    @Override
    public String getContentType(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        switch (extension) {
            case ".pdf":
                return "application/pdf";
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            default:
                return "application/octet-stream";
        }
    }

    @Override
    public void deleteBankSlip(String fileName) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete bank slip file", e);
        }
    }
}