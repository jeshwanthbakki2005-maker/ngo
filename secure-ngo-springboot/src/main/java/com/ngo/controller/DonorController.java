package com.ngo.controller;

import com.ngo.dto.DonationRequest;
import com.ngo.entity.*;
import com.ngo.service.DonorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/donor")
public class DonorController {

    @Autowired
    private DonorService donorService;

    private Long getAuthenticatedUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null
                && !auth.getPrincipal().equals("anonymousUser")) {
            try {
                return Long.parseLong(auth.getPrincipal().toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // Get donor dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            Long donorId = getAuthenticatedUserId();
            if (donorId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            Map<String, Object> dashboardData = donorService.getDonorDashboard(donorId);
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Make donation
    @PostMapping("/make-donation")
    public ResponseEntity<?> makeDonation(@RequestBody DonationRequest request) {
        try {
            Long donorId = getAuthenticatedUserId();
            if (donorId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            Donation donation = donorService.makeDonation(request, donorId);
            return ResponseEntity.ok(donation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get donation history
    @GetMapping("/donation-history")
    public ResponseEntity<?> getDonationHistory() {
        try {
            Long donorId = getAuthenticatedUserId();
            if (donorId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            List<Map<String, Object>> history = donorService.getDonationHistory(donorId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get impact reports
    @GetMapping("/impact-reports")
    public ResponseEntity<?> getImpactReports() {
        try {
            Long donorId = getAuthenticatedUserId();
            if (donorId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            Map<String, Object> impactReport = donorService.getImpactReports(donorId);
            return ResponseEntity.ok(impactReport);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
