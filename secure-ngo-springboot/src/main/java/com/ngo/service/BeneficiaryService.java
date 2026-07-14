package com.ngo.service;

import com.ngo.dto.UtilizationReportRequest;
import com.ngo.dto.UtilizationReportRequest;
import com.ngo.entity.*;
import com.ngo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BeneficiaryService {

    @Autowired
    private AllocationRepository allocationRepository;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private UtilizationReportRepository utilizationReportRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    // Get beneficiary dashboard data
    public Map<String, Object> getBeneficiaryDashboard(Long beneficiaryId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new RuntimeException("Beneficiary not found"));

        List<Allocation> approvedAllocations = allocationRepository.findByBeneficiaryIdAndStatusOrderByCreatedAtDesc(beneficiaryId, "approved");
        Double totalReceived = approvedAllocations.stream()
                .mapToDouble(a -> a.getAmount() != null ? a.getAmount() : 0.0)
                .sum();
        Double totalUtilized = utilizationReportRepository.getTotalByBeneficiaryIdAndStatus(beneficiaryId, "verified");
        Long pendingReports = utilizationReportRepository.countByBeneficiaryIdAndStatus(beneficiaryId, "submitted");
        List<Allocation> recentAllocations = allocationRepository.findTop5ByBeneficiaryIdOrderByCreatedAtDesc(beneficiaryId);
        Long totalRequests = allocationRepository.countByBeneficiaryId(beneficiaryId);
        Long approvedRequests = allocationRepository.countByBeneficiaryIdAndStatus(beneficiaryId, "approved");

        Map<String, Object> response = new HashMap<>();
        response.put("beneficiary", beneficiary);
        response.put("totalReceived", totalReceived);
        response.put("totalUtilized", totalUtilized != null ? totalUtilized : 0);
        response.put("pendingReports", pendingReports);
        response.put("recentAllocations", recentAllocations);
        response.put("totalRequests", totalRequests != null ? totalRequests : 0);
        response.put("approvedRequests", approvedRequests != null ? approvedRequests : 0);

        return response;
    }

    // Get allocated funds with remaining amount
    public List<Map<String, Object>> getAllocatedFunds(Long beneficiaryId) {
        List<Allocation> allocations = allocationRepository.findByBeneficiaryIdAndStatusOrderByCreatedAtDesc(beneficiaryId, "approved");
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Allocation allocation : allocations) {
            Double utilized = utilizationReportRepository.getTotalByAllocationId(allocation.getId());
            utilized = utilized != null ? utilized : 0;
            
            Map<String, Object> item = new HashMap<>();
            item.put("allocation", allocation);
            item.put("utilized", utilized);
            item.put("remaining", allocation.getAmount() - utilized);
            item.put("percentUsed", allocation.getAmount() > 0 ? (utilized / allocation.getAmount() * 100) : 0);

            Donation donation = donationRepository.findById(allocation.getDonationId()).orElse(null);
            if (donation != null) {
                item.put("donationPurpose", donation.getPurpose());
                item.put("donationAmount", donation.getAmount());
                item.put("donationDate", donation.getCreatedAt());
                item.put("donorId", donation.getDonorId());
                item.put("transactionId", donation.getTransactionId());
            }
            
            result.add(item);
        }
        
        return result;
    }

    // Submit utilization report
    @Transactional
    public UtilizationReport submitUtilizationReport(UtilizationReportRequest request, Long beneficiaryId) {
        Allocation allocation = allocationRepository.findById(request.getAllocationId())
                .orElseThrow(() -> new RuntimeException("Allocation not found"));

        Double utilized = utilizationReportRepository.getTotalByAllocationId(allocation.getId());
        utilized = utilized != null ? utilized : 0;
        Double remaining = allocation.getAmount() - utilized;

        if (request.getAmountUsed() > remaining) {
            throw new RuntimeException("Amount exceeds remaining allocation");
        }

        UtilizationReport report = new UtilizationReport();
        report.setAllocationId(request.getAllocationId());
        report.setBeneficiaryId(beneficiaryId);
        report.setAmountUsed(request.getAmountUsed());
        report.setDescription(request.getDescription());
        report.setReceiptReference(request.getReceiptReference());
        report.setStatus("submitted");
        report.setCreatedAt(LocalDateTime.now());

        UtilizationReport saved = utilizationReportRepository.save(report);
        logAction("UTILIZATION_SUBMITTED", "beneficiary", beneficiaryId, 
                  "Submitted utilization of " + request.getAmountUsed());

        return saved;
    }

    // Get utilization reports submitted by beneficiary
    public List<UtilizationReport> getUtilizationReports(Long beneficiaryId) {
        return utilizationReportRepository.findByBeneficiaryIdOrderByCreatedAtDesc(beneficiaryId);
    }

    // Get available allocations for reporting
    public List<Map<String, Object>> getAvailableAllocationsForReporting(Long beneficiaryId) {
        List<Allocation> allocations = allocationRepository.findByBeneficiaryIdAndStatusOrderByCreatedAtDesc(beneficiaryId, "approved");
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Allocation allocation : allocations) {
            Double utilized = utilizationReportRepository.getTotalByAllocationId(allocation.getId());
            utilized = utilized != null ? utilized : 0;
            Double remaining = allocation.getAmount() - utilized;
            
            if (remaining > 0) {
                Map<String, Object> item = new HashMap<>();
                item.put("allocation", allocation);
                item.put("remaining", remaining);
                result.add(item);
            }
        }
        
        return result;
    }

    // Get assistance history
    public Map<String, Object> getAssistanceHistory(Long beneficiaryId) {
        List<Allocation> allocations = allocationRepository.findByBeneficiaryIdOrderByCreatedAtDesc(beneficiaryId);
        List<UtilizationReport> reports = utilizationReportRepository.findByBeneficiaryIdOrderByCreatedAtDesc(beneficiaryId);

        Double totalReceived = allocations.stream()
                .filter(a -> "approved".equals(a.getStatus()))
                .mapToDouble(Allocation::getAmount)
                .sum();

        Double totalUtilized = reports.stream()
                .filter(r -> "verified".equals(r.getStatus()))
                .mapToDouble(UtilizationReport::getAmountUsed)
                .sum();

        Map<String, Object> response = new HashMap<>();
        response.put("allocations", allocations);
        response.put("reports", reports);
        response.put("totalReceived", totalReceived);
        response.put("totalUtilized", totalUtilized);

        return response;
    }

    public void submitUtilizationReport(Long userId, Long allocationId, Double amountUsed, String receiptReference, String description) {
        Allocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new RuntimeException("Allocation not found"));
        
        if (!allocation.getBeneficiaryId().equals(userId)) {
            throw new RuntimeException("Not authorized to report on this allocation");
        }
        
        UtilizationReport report = new UtilizationReport(allocationId, userId, amountUsed, description, receiptReference);
        utilizationReportRepository.save(report);
        
        logAction("Utilization Report Submitted", "Beneficiary", userId, "Submitted report for allocation ID " + allocationId + " for amount " + amountUsed);
    }

    private void logAction(String action, String userType, Long userId, String details) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setUserType(userType);
        log.setUserId(userId);
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }
}
