package com.ngo.service;

import com.ngo.dto.FundAllocationRequest;
import com.ngo.entity.*;
import com.ngo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class NFSAService {

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private AllocationRepository allocationRepository;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private DisasterReportRepository disasterReportRepository;

    @Autowired
    private NFSARepository nfsaRepository;

    @Autowired
    private UtilizationReportRepository utilizationReportRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordResetRequestRepository passwordResetRequestRepository;

    @Autowired
    private ReliefMaterialRequestRepository reliefMaterialRequestRepository;

    @Autowired
    private ReliefMaterialDonationRepository reliefMaterialDonationRepository;

    // Get Dashboard Data
    public Map<String, Object> getDashboardData() {
        Double totalDonations = donationRepository.getTotalAmount();
        Double totalAllocated = allocationRepository.getTotalAllocatedAmount();
        Long pendingAllocations = allocationRepository.countByStatus("pending");
        Long totalBeneficiaries = beneficiaryRepository.count();
        Long totalDonors = donorRepository.count();
        Long unverifiedDonors = donorRepository.countByIsVerified(false);
        Long unverifiedBeneficiaries = beneficiaryRepository.countByIsVerified(false);
        List<Donation> recentDonations = donationRepository.findTop5ByOrderByCreatedAtDesc();

        Long totalAdmins = nfsaRepository.count();
        Long totalCampaigns = disasterReportRepository.count();
        Long pendingRequests = disasterReportRepository.countByStatus("pending");
        Long activeCampaigns = disasterReportRepository.countByStatus("active");
        Long resolvedReports = disasterReportRepository.countByStatus("resolved");
        Long totalUsers = totalDonors + totalBeneficiaries + totalAdmins;
        List<DisasterReport> recentReports = disasterReportRepository.findAllByOrderByCreatedAtDesc();
        if (recentReports.size() > 5) {
            recentReports = recentReports.subList(0, 5);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("totalDonations", totalDonations != null ? totalDonations : 0);
        response.put("totalAllocated", totalAllocated != null ? totalAllocated : 0);
        response.put("pendingAllocations", pendingAllocations);
        response.put("totalBeneficiaries", totalBeneficiaries);
        response.put("totalDonors", totalDonors);
        response.put("totalAdmins", totalAdmins);
        response.put("totalUsers", totalUsers);
        response.put("totalCampaigns", totalCampaigns);
        response.put("pendingRequests", pendingRequests);
        response.put("activeCampaigns", activeCampaigns);
        response.put("resolvedReports", resolvedReports);
        response.put("unverifiedDonors", unverifiedDonors);
        response.put("unverifiedBeneficiaries", unverifiedBeneficiaries);
        response.put("recentDonations", recentDonations);
        response.put("recentReports", recentReports);

        return response;
    }

    // Allocate Funds
    @Transactional
    public Allocation allocateFund(FundAllocationRequest request, Long nfsaId) {
        Donation donation = donationRepository.findById(request.getDonationId())
                .orElseThrow(() -> new RuntimeException("Donation not found"));
        
        Beneficiary beneficiary = beneficiaryRepository.findById(request.getBeneficiaryId())
                .orElseThrow(() -> new RuntimeException("Beneficiary not found"));

        Double available = donation.getAmount() - donation.getAllocatedAmount();
        if (request.getAmount() > available) {
            throw new RuntimeException("Allocation amount exceeds available funds");
        }

        Allocation allocation = new Allocation();
        allocation.setDonationId(request.getDonationId());
        allocation.setBeneficiaryId(request.getBeneficiaryId());
        allocation.setAmount(request.getAmount());
        allocation.setPurpose(request.getPurpose());
        allocation.setStatus("approved");
        allocation.setApprovedBy(nfsaId);
        allocation.setDisbursedAt(LocalDateTime.now());
        allocation.setCreatedAt(LocalDateTime.now());

        Allocation saved = allocationRepository.save(allocation);
        
        // Update donation allocated amount
        donation.setAllocatedAmount(donation.getAllocatedAmount() + request.getAmount());
        donationRepository.save(donation);

        logAction("FUND_ALLOCATION", "nfsa", nfsaId, 
                  "Allocated " + request.getAmount() + " to beneficiary " + request.getBeneficiaryId());

        return saved;
    }

    // Get all allocations
    public List<Allocation> getAllAllocations() {
        return allocationRepository.findAllByOrderByCreatedAtDesc();
    }

    // Get available donations for allocation
    public List<Donation> getAvailableDonations() {
        return donationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .filter(d -> d.getAmount() > d.getAllocatedAmount())
                .collect(Collectors.toList());
    }

    // Get verified beneficiaries
    public List<Beneficiary> getVerifiedBeneficiaries() {
        return beneficiaryRepository.findByIsVerified(true);
    }

    // Get verified donors
    public List<Donor> getVerifiedDonors() {
        return donorRepository.findByIsVerified(true);
    }

    // Verify beneficiary
    @Transactional
    public void verifyBeneficiary(Long beneficiaryId, Boolean verify, Long nfsaId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new RuntimeException("Beneficiary not found"));

        beneficiary.setIsVerified(verify);
        beneficiaryRepository.save(beneficiary);

        String action = verify ? "BENEFICIARY_VERIFIED" : "BENEFICIARY_REJECTED";
        logAction(action, "nfsa", nfsaId, 
                  (verify ? "Verified" : "Rejected") + " beneficiary " + beneficiaryId);
    }

    // Verify donor
    @Transactional
    public void verifyDonor(Long donorId, Boolean verify, Long nfsaId) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new RuntimeException("Donor not found"));

        donor.setIsVerified(verify);
        donorRepository.save(donor);

        String action = verify ? "DONOR_VERIFIED" : "DONOR_REJECTED";
        logAction(action, "nfsa", nfsaId, 
                  (verify ? "Verified" : "Rejected") + " donor " + donorId);
    }

    // Get all donors for verification
    public List<Donor> getAllDonors() {
        return donorRepository.findAllByOrderByCreatedAtDesc();
    }

    // Get all beneficiaries for verification
    public List<Beneficiary> getAllBeneficiaries() {
        return beneficiaryRepository.findAllByOrderByCreatedAtDesc();
    }

    // Get utilization reports
    public List<UtilizationReport> getUtilizationReports() {
        return utilizationReportRepository.findAllByOrderByCreatedAtDesc();
    }

    // Approve/Reject utilization report
    @Transactional
    public void updateUtilizationReportStatus(Long reportId, String status, Long nfsaId) {
        UtilizationReport report = utilizationReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found"));

        report.setStatus(status);
        report.setVerifiedBy(nfsaId.toString());
        report.setVerifiedAt(LocalDateTime.now());
        utilizationReportRepository.save(report);

        String action = "verified".equals(status) ? "UTILIZATION_APPROVED" : "UTILIZATION_REJECTED";
        logAction(action, "nfsa", nfsaId, 
                  ("verified".equals(status) ? "Approved" : "Rejected") + " utilization report " + reportId);
    }

    // Update disaster report status
    @Transactional
    public void updateDisasterReportStatus(Long reportId, String status, Long nfsaId) {
        DisasterReport report = disasterReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Disaster report not found"));

        status = status == null ? "" : status.trim().toLowerCase();
        if (!"active".equals(status) && !"resolved".equals(status) && !"pending".equals(status) && !"rejected".equals(status)) {
            throw new RuntimeException("Invalid disaster report status: " + status);
        }

        report.setStatus(status);
        disasterReportRepository.save(report);

        String actionType;
        if ("active".equals(status)) {
            actionType = "DISASTER_REPORT_APPROVED";
        } else if ("rejected".equals(status)) {
            actionType = "DISASTER_REPORT_REJECTED";
        } else if ("resolved".equals(status)) {
            actionType = "DISASTER_REPORT_RESOLVED";
        } else {
            actionType = "DISASTER_REPORT_STATUS_UPDATED";
        }

        logAction(actionType, "nfsa", nfsaId, "Set disaster report " + reportId + " status to " + status);
    }

    // Remove beneficiary
    @Transactional
    public void removeBeneficiary(Long beneficiaryId, Long nfsaId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new RuntimeException("Beneficiary not found"));

        List<Long> requestIds = reliefMaterialRequestRepository
                .findByBeneficiaryIdOrderByCreatedAtDesc(beneficiaryId).stream()
                .map(ReliefMaterialRequest::getId)
                .collect(Collectors.toList());
        if (!requestIds.isEmpty()) reliefMaterialDonationRepository.deleteByRequestIdIn(requestIds);
        reliefMaterialRequestRepository.deleteByBeneficiaryId(beneficiaryId);
        disasterReportRepository.deleteByReportedByBeneficiaryId(beneficiaryId);
        utilizationReportRepository.deleteByBeneficiaryId(beneficiaryId);
        allocationRepository.deleteByBeneficiaryId(beneficiaryId);
        passwordResetRequestRepository.deleteForAccount("beneficiary", beneficiaryId, beneficiary.getEmail());
        auditLogRepository.deleteForAccount("beneficiary", beneficiaryId);
        
        String name = beneficiary.getFullName();
        beneficiaryRepository.delete(beneficiary);

        logAction("BENEFICIARY_REMOVED", "nfsa", nfsaId, 
                  "Removed beneficiary " + beneficiaryId + " - " + name);
    }

    // Remove donor
    @Transactional
    public void removeDonor(Long donorId, Long nfsaId) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new RuntimeException("Donor not found"));

        List<Donation> donations = donationRepository.findByDonorId(donorId);
        List<Long> donationIds = donations.stream().map(Donation::getId).collect(Collectors.toList());

        if (!donationIds.isEmpty()) {
            List<Long> allocationIds = allocationRepository.findByDonationIdIn(donationIds).stream()
                    .map(Allocation::getId)
                    .collect(Collectors.toList());
            if (!allocationIds.isEmpty()) utilizationReportRepository.deleteByAllocationIdIn(allocationIds);
            allocationRepository.deleteByDonationIdIn(donationIds);
        }
        donationRepository.deleteAll(donations);
        reliefMaterialDonationRepository.deleteByDonorId(donorId);
        passwordResetRequestRepository.deleteForAccount("donor", donorId, donor.getEmail());
        auditLogRepository.deleteForAccount("donor", donorId);
        
        String name = donor.getFullName();
        donorRepository.delete(donor);

        logAction("DONOR_REMOVED", "nfsa", nfsaId, 
                  "Removed donor " + donorId + " - " + name);
    }

    // Removing the current/last-used administrator is treated as a complete
    // platform reset because platform totals are global rather than admin-owned.
    @Transactional
    public void removeNfsaAdmin(Long adminId, Long removedByAdminId, boolean clearAllPlatformData) {
        NFSA admin = nfsaRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("NFSA administrator not found."));

        if (clearAllPlatformData) {
            // Delete dependent/transactional rows before their parent accounts.
            reliefMaterialDonationRepository.deleteAllInBatch();
            reliefMaterialRequestRepository.deleteAllInBatch();
            utilizationReportRepository.deleteAllInBatch();
            allocationRepository.deleteAllInBatch();
            donationRepository.deleteAllInBatch();
            disasterReportRepository.deleteAllInBatch();
            passwordResetRequestRepository.deleteAllInBatch();
            auditLogRepository.deleteAllInBatch();
            beneficiaryRepository.deleteAllInBatch();
            donorRepository.deleteAllInBatch();
            nfsaRepository.deleteAllInBatch();
            return;
        }

        allocationRepository.clearApprover(adminId);
        utilizationReportRepository.clearVerifier(adminId.toString());
        passwordResetRequestRepository.deleteForAccount("nfsa", adminId, admin.getEmail());
        auditLogRepository.deleteForAccount("nfsa", adminId);
        nfsaRepository.delete(admin);

        // A self-deletion leaves no audit record tied to the removed account.
        if (removedByAdminId != null && !removedByAdminId.equals(adminId)) {
            logAction("NFSA_ADMIN_REMOVED", "nfsa", removedByAdminId,
                    "Removed NFSA administrator account " + adminId);
        }
    }

    // Clear records left by a previously deleted setup while retaining the
    // currently registered administrator account.
    @Transactional
    public void clearPreviousPlatformData(Long currentAdminId) {
        nfsaRepository.findById(currentAdminId)
                .orElseThrow(() -> new RuntimeException("Current NFSA administrator not found."));

        reliefMaterialDonationRepository.deleteAllInBatch();
        reliefMaterialRequestRepository.deleteAllInBatch();
        utilizationReportRepository.deleteAllInBatch();
        allocationRepository.deleteAllInBatch();
        donationRepository.deleteAllInBatch();
        disasterReportRepository.deleteAllInBatch();
        passwordResetRequestRepository.deleteAllInBatch();
        auditLogRepository.deleteAllInBatch();
        beneficiaryRepository.deleteAllInBatch();
        donorRepository.deleteAllInBatch();

        List<Long> otherAdminIds = nfsaRepository.findAll().stream()
                .map(NFSA::getId)
                .filter(id -> !currentAdminId.equals(id))
                .collect(Collectors.toList());
        if (!otherAdminIds.isEmpty()) nfsaRepository.deleteAllByIdInBatch(otherAdminIds);
    }

    // Get financial reports
    public Map<String, Object> getFinancialReports() {
        Double totalDonations = donationRepository.getTotalAmount();
        Double totalAllocated = allocationRepository.getTotalAllocatedByStatus("approved");
        Double totalUtilized = utilizationReportRepository.getTotalUtilizedByStatus("verified");

        Map<String, Object> response = new HashMap<>();
        response.put("totalDonations", totalDonations != null ? totalDonations : 0);
        response.put("totalAllocated", totalAllocated != null ? totalAllocated : 0);
        response.put("totalUtilized", totalUtilized != null ? totalUtilized : 0);

        return response;
    }

    // Approve password reset request
    @Transactional
    public void approvePasswordReset(Long requestId, Long nfsaId) {
        PasswordResetRequest resetReq = passwordResetRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Reset request not found"));

        resetReq.setStatus("approved");
        resetReq.setApprovedAt(LocalDateTime.now());
        passwordResetRequestRepository.save(resetReq);

        logAction("PASSWORD_RESET_APPROVED", "nfsa", nfsaId, 
                  "Approved reset for " + resetReq.getEmail());
    }

    // Get pending password reset requests
    public List<PasswordResetRequest> getPendingPasswordResets() {
        return passwordResetRequestRepository.findByStatusOrderByCreatedAtDesc("pending");
    }

    private void logAction(String action, String userType, Long userId, String details) {
        try {
            AuditLog log = new AuditLog();
            log.setAction(action);
            log.setUserType(userType);
            log.setUserId(userId != null ? userId : 0L);
            log.setDetails(details);
            log.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Logging failure must never roll back the main transaction
            System.err.println("Audit log failed: " + e.getMessage());
        }
    }
}
