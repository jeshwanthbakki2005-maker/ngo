package com.ngo.controller;

import com.ngo.dto.PasswordResetRequestDTO;
import com.ngo.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/password-reset")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    // Request password reset
    @PostMapping("/request")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequestDTO request) {
        try {
            com.ngo.entity.PasswordResetRequest resetReq = passwordResetService
                    .requestPasswordReset(request.getEmail(), request.getUserType());
            return ResponseEntity.ok(Map.of(
                    "message", "Password reset request submitted. Please wait for admin approval.",
                    "requestId", resetReq.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Generate reset token (for user after NFSA approval)
    @PostMapping("/generate-token/{requestId}")
    public ResponseEntity<?> generateResetToken(@PathVariable Long requestId) {
        try {
            String token = passwordResetService.generateResetToken(requestId);
            return ResponseEntity.ok(Map.of(
                    "message", "Reset token generated",
                    "token", token
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Complete password reset
    @PostMapping("/complete")
    public ResponseEntity<?> completePasswordReset(@RequestParam String token,
                                                    @RequestParam String newPassword) {
        try {
            passwordResetService.completePasswordReset(token, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successful! Please login with your new password."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
