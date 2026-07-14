package com.ngo.controller;

import com.ngo.dto.UtilizationReportRequest;
import com.ngo.entity.*;
import com.ngo.service.BeneficiaryService;
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
@RequestMapping("/api/beneficiary")
public class BeneficiaryController {

    @Autowired
    private BeneficiaryService beneficiaryService;

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

    // Get beneficiary dashboard
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            Long beneficiaryId = getAuthenticatedUserId();
            if (beneficiaryId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            Map<String, Object> dashboardData = beneficiaryService.getBeneficiaryDashboard(beneficiaryId);
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get allocated funds
    @GetMapping("/allocated-funds")
    public ResponseEntity<?> getAllocatedFunds() {
        try {
            Long beneficiaryId = getAuthenticatedUserId();
            if (beneficiaryId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            List<Map<String, Object>> allocatedFunds = beneficiaryService.getAllocatedFunds(beneficiaryId);
            return ResponseEntity.ok(allocatedFunds);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Submit utilization report
    @PostMapping("/submit-utilization")
    public ResponseEntity<?> submitUtilization(@RequestBody UtilizationReportRequest request) {
        try {
            Long beneficiaryId = getAuthenticatedUserId();
            if (beneficiaryId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            UtilizationReport report = beneficiaryService.submitUtilizationReport(request, beneficiaryId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get utilization reports
    @GetMapping("/utilization-reports")
    public ResponseEntity<?> getUtilizationReports() {
        try {
            Long beneficiaryId = getAuthenticatedUserId();
            if (beneficiaryId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            List<UtilizationReport> reports = beneficiaryService.getUtilizationReports(beneficiaryId);
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get available allocations for reporting
    @GetMapping("/available-allocations")
    public ResponseEntity<?> getAvailableAllocations() {
        try {
            Long beneficiaryId = getAuthenticatedUserId();
            if (beneficiaryId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            List<Map<String, Object>> allocations = beneficiaryService.getAvailableAllocationsForReporting(beneficiaryId);
            return ResponseEntity.ok(allocations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get assistance history
    @GetMapping("/assistance-history")
    public ResponseEntity<?> getAssistanceHistory() {
        try {
            Long beneficiaryId = getAuthenticatedUserId();
            if (beneficiaryId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            Map<String, Object> history = beneficiaryService.getAssistanceHistory(beneficiaryId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
