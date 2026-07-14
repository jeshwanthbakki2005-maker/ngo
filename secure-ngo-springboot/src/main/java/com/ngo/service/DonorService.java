package com.ngo.service;

import com.ngo.dto.DonationRequest;
import com.ngo.entity.*;
import com.ngo.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DonorService {

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private AllocationRepository allocationRepository;

    @Autowired
    private DonorRepository donorRepository;

    @Autowired
    private UtilizationReportRepository utilizationReportRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    // Get donor dashboard data
    public Map<String, Object> getDonorDashboard(Long donorId) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new RuntimeException("Donor not found"));

        Double totalDonated = donationRepository.getTotalByDonorId(donorId);
        Long donationCount = donationRepository.countByDonorId(donorId);
        List<Donation> recentDonations = donationRepository.findTop5ByDonorIdOrderByCreatedAtDesc(donorId);

        Map<String, Object> response = new HashMap<>();
        response.put("donor", donor);
        response.put("totalDonated", totalDonated != null ? totalDonated : 0);
        response.put("donationCount", donationCount);
        response.put("recentDonations", recentDonations);

        return response;
    }

    // Make a donation
    @Transactional
    public Donation makeDonation(DonationRequest request, Long donorId) {
        Donor donor = donorRepository.findById(donorId)
                .orElseThrow(() -> new RuntimeException("Donor not found"));

        // verification check removed - all donors can donate

        Donation donation = new Donation();
        donation.setDonorId(donorId);
        donation.setAmount(request.getAmount());
        donation.setPurpose(request.getPurpose());
        donation.setPaymentMethod(request.getPaymentMethod());
        donation.setTransactionId(generateTransactionId());
        donation.setStatus("completed");
        donation.setAllocatedAmount(0.0);
        donation.setCreatedAt(LocalDateTime.now());

        Donation saved = donationRepository.save(donation);
        logAction("DONATION_MADE", "donor", donorId, 
                  "Donated " + request.getAmount() + " for " + request.getPurpose());

        return saved;
    }

    // Get donation history with impact
    public List<Map<String, Object>> getDonationHistory(Long donorId) {
        List<Donation> donations = donationRepository.findByDonorIdOrderByCreatedAtDesc(donorId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Donation donation : donations) {
            Map<String, Object> item = new HashMap<>();
            item.put("donation", donation);
            
            List<Allocation> allocations = allocationRepository.findByDonationId(donation.getId());
            item.put("allocations", allocations);
            
            Double utilization = 0.0;
            for (Allocation a : allocations) {
                Double util = utilizationReportRepository.getTotalByAllocationIdAndStatus(a.getId(), "verified");
                utilization += util != null ? util : 0;
            }
            item.put("utilization", utilization);
            item.put("utilizationPercent", donation.getAmount() > 0 ? (utilization / donation.getAmount() * 100) : 0);
            
            result.add(item);
        }
        
        return result;
    }

    // Get impact reports
    public Map<String, Object> getImpactReports(Long donorId) {
        List<Donation> donations = donationRepository.findByDonorId(donorId);
        
        Double totalDonated = donations.stream().mapToDouble(Donation::getAmount).sum();
        Double totalAllocated = donations.stream().mapToDouble(Donation::getAllocatedAmount).sum();
        
        Long beneficiariesHelped = allocationRepository.countDistinctBeneficiariesByDonorId(donorId);

        Map<String, Object> response = new HashMap<>();
        response.put("totalDonated", totalDonated);
        response.put("totalAllocated", totalAllocated);
        response.put("beneficiariesHelped", beneficiariesHelped);

        return response;
    }

    // Get available campaigns (placeholder list)
    public List<String> getAvailableCampaigns() {
        // In future this should come from a Campaign repository; hardcoded for now
        return Arrays.asList(
                "Flood Relief - Andhra Pradesh",
                "Cyclone Relief - Odisha",
                "Earthquake Relief - Assam",
                "Drought Support - Maharashtra",
                "General Disaster Relief Fund"
        );
    }

    private String generateTransactionId() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
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
