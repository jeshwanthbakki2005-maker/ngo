package com.ngo.repository;

import com.ngo.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByDonorIdOrderByCreatedAtDesc(Long donorId);
    List<Donation> findAllByOrderByCreatedAtDesc();
    List<Donation> findTop5ByOrderByCreatedAtDesc();
    List<Donation> findTop5ByDonorIdOrderByCreatedAtDesc(Long donorId);
    List<Donation> findByDonorId(Long donorId);
    Long countByDonorId(Long donorId);
    List<Donation> findByPurpose(String purpose);
    
    @Query("SELECT SUM(d.amount) FROM Donation d")
    Double getTotalAmount();
    
    @Query("SELECT SUM(d.amount) FROM Donation d WHERE d.donorId = :donorId")
    Double getTotalByDonorId(@org.springframework.data.repository.query.Param("donorId") Long donorId);

    @Query("SELECT SUM(d.amount) FROM Donation d WHERE d.purpose = :purpose")
    Double getTotalByPurpose(@org.springframework.data.repository.query.Param("purpose") String purpose);

    @Query("SELECT SUM(d.amount) FROM Donation d WHERE LOWER(d.purpose) = LOWER(:purpose) AND d.createdAt >= :createdAt")
    Double getTotalByPurposeSince(@org.springframework.data.repository.query.Param("purpose") String purpose,
                                  @org.springframework.data.repository.query.Param("createdAt") java.time.LocalDateTime createdAt);
}
