package com.ngo.repository;

import com.ngo.entity.UtilizationReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UtilizationReportRepository extends JpaRepository<UtilizationReport, Long> {
    List<UtilizationReport> findByBeneficiaryIdOrderByCreatedAtDesc(Long beneficiaryId);
    List<UtilizationReport> findByAllocationIdOrderByCreatedAtDesc(Long allocationId);
    List<UtilizationReport> findByStatusOrderByCreatedAtDesc(String status);
    List<UtilizationReport> findAllByOrderByCreatedAtDesc();
    Long countByBeneficiaryIdAndStatus(Long beneficiaryId, String status);
    
    @Query("SELECT SUM(u.amountUsed) FROM UtilizationReport u WHERE u.status = :status")
    Double getTotalUtilizedByStatus(String status);
    
    @Query("SELECT SUM(u.amountUsed) FROM UtilizationReport u WHERE u.allocationId = :allocationId AND u.status = :status")
    Double getTotalByAllocationIdAndStatus(Long allocationId, String status);
    
    @Query("SELECT SUM(u.amountUsed) FROM UtilizationReport u WHERE u.allocationId = :allocationId")
    Double getTotalByAllocationId(Long allocationId);
    
    @Query("SELECT SUM(u.amountUsed) FROM UtilizationReport u WHERE u.beneficiaryId = :beneficiaryId AND u.status = :status")
    Double getTotalByBeneficiaryIdAndStatus(Long beneficiaryId, String status);
    
    @Modifying
    @Query("DELETE FROM UtilizationReport u WHERE u.beneficiaryId = :beneficiaryId")
    void deleteByBeneficiaryId(Long beneficiaryId);
    
    @Modifying
    @Query("DELETE FROM UtilizationReport u WHERE u.allocationId IN :allocationIds")
    void deleteByAllocationIdIn(List<Long> allocationIds);
    
    Optional<UtilizationReport> findByIdAndStatus(Long id, String status);

    @Modifying
    @Query("UPDATE UtilizationReport u SET u.verifiedBy = null, u.verifiedAt = null WHERE u.verifiedBy = :adminId")
    int clearVerifier(@Param("adminId") String adminId);
}   
