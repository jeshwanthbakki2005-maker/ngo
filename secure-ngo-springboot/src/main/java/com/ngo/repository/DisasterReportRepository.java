package com.ngo.repository;

import com.ngo.entity.DisasterReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisasterReportRepository extends JpaRepository<DisasterReport, Long> {
    List<DisasterReport> findAllByOrderByCreatedAtDesc();
    List<DisasterReport> findByStatusOrderByCreatedAtDesc(String status);
    List<DisasterReport> findByReportedByBeneficiaryIdOrderByCreatedAtDesc(Long beneficiaryId);
    Long countByStatus(String status);

    @Modifying
    @Query("delete from DisasterReport d where d.reportedByBeneficiaryId = :beneficiaryId")
    int deleteByReportedByBeneficiaryId(@Param("beneficiaryId") Long beneficiaryId);
}
