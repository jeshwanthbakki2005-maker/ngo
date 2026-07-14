package com.ngo.controller;

import com.ngo.dto.FundAllocationRequest;
import com.ngo.entity.*;
import com.ngo.service.NFSAService;
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
@RequestMapping("/api/nfsa")
public class NFSAController {

    @Autowired
    private NFSAService nfsaService;

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

    // Get dashboard data
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            Map<String, Object> dashboardData = nfsaService.getDashboardData();
            return ResponseEntity.ok(dashboardData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Allocate funds
    @PostMapping("/allocate-fund")
    public ResponseEntity<?> allocateFund(@RequestBody FundAllocationRequest request) {
        try {
            Long nfsaId = getAuthenticatedUserId();
            if (nfsaId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            Allocation allocation = nfsaService.allocateFund(request, nfsaId);
            return ResponseEntity.ok(allocation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get all allocations
    @GetMapping("/allocations")
    public ResponseEntity<?> getAllAllocations() {
        try {
            List<Allocation> allocations = nfsaService.getAllAllocations();
            return ResponseEntity.ok(allocations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get available donations
    @GetMapping("/available-donations")
    public ResponseEntity<?> getAvailableDonations() {
        try {
            List<Donation> donations = nfsaService.getAvailableDonations();
            return ResponseEntity.ok(donations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get verified beneficiaries
    @GetMapping("/verified-beneficiaries")
    public ResponseEntity<?> getVerifiedBeneficiaries() {
        try {
            List<Beneficiary> beneficiaries = nfsaService.getVerifiedBeneficiaries();
            return ResponseEntity.ok(beneficiaries);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get all beneficiaries for verification
    @GetMapping("/beneficiaries")
    public ResponseEntity<?> getAllBeneficiaries() {
        try {
            List<Beneficiary> beneficiaries = nfsaService.getAllBeneficiaries();
            return ResponseEntity.ok(beneficiaries);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Verify beneficiary
    @PostMapping("/verify-beneficiary")
    public ResponseEntity<?> verifyBeneficiary(@RequestParam Long beneficiaryId,
                                                @RequestParam Boolean verify) {
        try {
            Long nfsaId = getAuthenticatedUserId();
            if (nfsaId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            nfsaService.verifyBeneficiary(beneficiaryId, verify, nfsaId);
            return ResponseEntity.ok(Map.of("message", verify ? "Beneficiary verified" : "Beneficiary rejected"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get all donors for verification
    @GetMapping("/donors")
    public ResponseEntity<?> getAllDonors() {
        try {
            List<Donor> donors = nfsaService.getAllDonors();
            return ResponseEntity.ok(donors);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Verify donor
    @PostMapping("/verify-donor")
    public ResponseEntity<?> verifyDonor(@RequestParam Long donorId,
                                          @RequestParam Boolean verify) {
        try {
            Long nfsaId = getAuthenticatedUserId();
            if (nfsaId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            nfsaService.verifyDonor(donorId, verify, nfsaId);
            return ResponseEntity.ok(Map.of("message", verify ? "Donor verified" : "Donor rejected"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get utilization reports
    @GetMapping("/utilization-reports")
    public ResponseEntity<?> getUtilizationReports() {
        try {
            List<UtilizationReport> reports = nfsaService.getUtilizationReports();
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Update utilization report status
    @PostMapping("/update-utilization-status")
    public ResponseEntity<?> updateUtilizationStatus(@RequestParam Long reportId,
                                                      @RequestParam String status) {
        try {
            Long nfsaId = getAuthenticatedUserId();
            if (nfsaId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            nfsaService.updateUtilizationReportStatus(reportId, status, nfsaId);
            return ResponseEntity.ok(Map.of("message", "Utilization report status updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Remove beneficiary
    @PostMapping("/remove-beneficiary/{beneficiaryId}")
    public ResponseEntity<?> removeBeneficiary(@PathVariable Long beneficiaryId) {
        try {
            Long nfsaId = getAuthenticatedUserId();
            if (nfsaId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            nfsaService.removeBeneficiary(beneficiaryId, nfsaId);
            return ResponseEntity.ok(Map.of("message", "Beneficiary removed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Remove donor
    @PostMapping("/remove-donor/{donorId}")
    public ResponseEntity<?> removeDonor(@PathVariable Long donorId) {
        try {
            Long nfsaId = getAuthenticatedUserId();
            if (nfsaId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            nfsaService.removeDonor(donorId, nfsaId);
            return ResponseEntity.ok(Map.of("message", "Donor removed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get financial reports
    @GetMapping("/financial-reports")
    public ResponseEntity<?> getFinancialReports() {
        try {
            Map<String, Object> reports = nfsaService.getFinancialReports();
            return ResponseEntity.ok(reports);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Get pending password resets
    @GetMapping("/password-requests")
    public ResponseEntity<?> getPendingPasswordResets() {
        try {
            List<PasswordResetRequest> requests = nfsaService.getPendingPasswordResets();
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Approve password reset
    @PostMapping("/approve-reset/{requestId}")
    public ResponseEntity<?> approvePasswordReset(@PathVariable Long requestId) {
        try {
            Long nfsaId = getAuthenticatedUserId();
            if (nfsaId == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
            }
            nfsaService.approvePasswordReset(requestId, nfsaId);
            return ResponseEntity.ok(Map.of("message", "Password reset approved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
