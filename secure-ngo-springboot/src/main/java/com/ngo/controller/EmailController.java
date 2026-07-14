package com.ngo.controller;

import com.ngo.exception.EmailDeliveryException;
import com.ngo.service.EmailService;
import jakarta.mail.internet.InternetAddress;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email")
public class EmailController {
    private static final Logger log = LoggerFactory.getLogger(EmailController.class);
    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        var status = emailService.getConnectionStatus();
        return ResponseEntity.ok(Map.of(
                "connected", status.connected(),
                "label", status.label(),
                "detail", status.detail(),
                "account", emailService.getConfiguredAddress()));
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestEmail(@RequestBody TestEmailRequest request) {
        if (request == null || !isValidEmail(request.email())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "error", "message", "Enter a valid recipient email address."));
        }
        try {
            emailService.sendTestEmail(request.email());
            return ResponseEntity.ok(Map.of("status", "success", "message", "Test email sent successfully."));
        } catch (EmailDeliveryException exception) {
            log.warn("Test email request failed for {}: {}", request.email(), exception.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("status", "error", "message", exception.getMessage()));
        }
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) return false;
        try {
            InternetAddress address = new InternetAddress(email.trim(), true);
            address.validate();
            return address.getAddress().contains("@");
        } catch (Exception exception) {
            return false;
        }
    }

    public record TestEmailRequest(String email) {}
}
