package com.ngo.repository;

import com.ngo.entity.ReliefMaterialRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReliefMaterialRequestRepository extends JpaRepository<ReliefMaterialRequest, Long> {
    List<ReliefMaterialRequest> findByBeneficiaryIdOrderByCreatedAtDesc(Long beneficiaryId);
    List<ReliefMaterialRequest> findAllByOrderByCreatedAtDesc();

    @Modifying
    @Query("delete from ReliefMaterialRequest r where r.beneficiaryId = :beneficiaryId")
    int deleteByBeneficiaryId(@Param("beneficiaryId") Long beneficiaryId);
}
