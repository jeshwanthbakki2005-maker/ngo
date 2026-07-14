package com.ngo.repository;

import com.ngo.entity.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, Long> {
    List<Allocation> findByBeneficiaryIdOrderByCreatedAtDesc(Long beneficiaryId);
    List<Allocation> findByDonationIdOrderByCreatedAtDesc(Long donationId);
    List<Allocation> findByStatusOrderByCreatedAtDesc(String status);
    List<Allocation> findAllByOrderByCreatedAtDesc();
    List<Allocation> findByDonationId(Long donationId);
    List<Allocation> findByDonationIdIn(List<Long> donationIds);
    List<Allocation> findTop5ByBeneficiaryIdOrderByCreatedAtDesc(Long beneficiaryId);
    List<Allocation> findByBeneficiaryIdAndStatusOrderByCreatedAtDesc(Long beneficiaryId, String status);
    Long countByStatus(String status);
    Long countByBeneficiaryId(Long beneficiaryId);
    Long countByBeneficiaryIdAndStatus(Long beneficiaryId, String status);
    
    @Query("SELECT SUM(a.amount) FROM Allocation a")
    Double getTotalAllocatedAmount();
    
    @Query("SELECT SUM(a.amount) FROM Allocation a WHERE a.status = :status")
    Double getTotalAllocatedByStatus(String status);
    
    @Query("SELECT SUM(a.amount) FROM Allocation a WHERE a.beneficiaryId = :beneficiaryId AND a.status = :status")
    Double getTotalByBeneficiaryIdAndStatus(Long beneficiaryId, String status);
    
    @Query("SELECT COUNT(DISTINCT a.beneficiaryId) FROM Allocation a JOIN Donation d ON a.donationId = d.id WHERE d.donorId = :donorId")
    Long countDistinctBeneficiariesByDonorId(Long donorId);
    
    @Modifying
    @Query("DELETE FROM Allocation a WHERE a.beneficiaryId = :beneficiaryId")
    void deleteByBeneficiaryId(Long beneficiaryId);
    
    @Modifying
    @Query("DELETE FROM Allocation a WHERE a.donationId IN :donationIds")
    void deleteByDonationIdIn(List<Long> donationIds);

    @Modifying
    @Query("UPDATE Allocation a SET a.approvedBy = null WHERE a.approvedBy = :adminId")
    int clearApprover(@Param("adminId") Long adminId);
}
