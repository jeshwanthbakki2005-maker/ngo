package com.ngo.controller;

import com.ngo.dto.LoginRequest;
import com.ngo.dto.LoginResponse;
import com.ngo.entity.*;
import com.ngo.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    // ===== PUBLIC ENDPOINTS =====

    @GetMapping("/")
    public ResponseEntity<Map<String, String>> home() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Welcome to Secure NGO Donation Tracking System");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        Map<String, String> result = new HashMap<>();
        result.put("message", "You have been logged out");
        return ResponseEntity.ok(result);
    }

    // ===== NFSA ENDPOINTS =====

    @PostMapping("/nfsa/signup")
    public ResponseEntity<?> nfsaSignup(@RequestBody NFSA nfsa) {
        try {
            LoginResponse response = authService.nfsaSignup(nfsa);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/nfsa/login")
    public ResponseEntity<?> nfsaLogin(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.nfsaLogin(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // ===== BENEFICIARY ENDPOINTS =====

    @PostMapping("/beneficiary/signup")
    public ResponseEntity<?> beneficiarySignup(@RequestBody Beneficiary beneficiary) {
        try {
            LoginResponse response = authService.beneficiarySignup(beneficiary);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/beneficiary/login")
    public ResponseEntity<?> beneficiaryLogin(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.beneficiaryLogin(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}
