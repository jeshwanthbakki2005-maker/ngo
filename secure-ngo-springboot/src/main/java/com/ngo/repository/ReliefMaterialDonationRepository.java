package com.ngo.repository;

import com.ngo.entity.ReliefMaterialDonation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReliefMaterialDonationRepository extends JpaRepository<ReliefMaterialDonation, Long> {
    @Query("select coalesce(sum(d.quantity), 0) from ReliefMaterialDonation d where d.requestId = :requestId and d.status <> 'cancelled'")
    Long getDonatedQuantity(@Param("requestId") Long requestId);
    List<ReliefMaterialDonation> findByDonorIdOrderByCreatedAtDesc(Long donorId);
    List<ReliefMaterialDonation> findByRequestIdInOrderByCreatedAtDesc(List<Long> requestIds);

    @Modifying
    @Query("delete from ReliefMaterialDonation d where d.donorId = :donorId")
    int deleteByDonorId(@Param("donorId") Long donorId);

    @Modifying
    @Query("delete from ReliefMaterialDonation d where d.requestId in :requestIds")
    int deleteByRequestIdIn(@Param("requestIds") List<Long> requestIds);
}
