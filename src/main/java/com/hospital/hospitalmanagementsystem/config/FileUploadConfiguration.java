package com.hospital.hospitalmanagementsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileUploadConfiguration {

    @Value("${file.upload-dir:uploads/bank-slips/}")
    private String uploadDir;

    @Bean
    CommandLineRunner initUploadDirectory() {
        return args -> {
            try {
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                    System.out.println("✓ Created upload directory: " + uploadPath.toAbsolutePath());
                } else {
                    System.out.println("✓ Upload directory exists: " + uploadPath.toAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println("✗ Failed to create upload directory: " + e.getMessage());
                throw new RuntimeException("Could not create upload directory", e);
            }
        };
    }
}