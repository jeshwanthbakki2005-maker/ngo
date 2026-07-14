package com.ngo.repository;

import com.ngo.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    Optional<Beneficiary> findByEmail(String email);
    Optional<Beneficiary> findByEmailIgnoreCase(String email);
    Optional<Beneficiary> findFirstByPhone(String phone);
    List<Beneficiary> findByIsVerified(Boolean isVerified);
    List<Beneficiary> findAllByOrderByCreatedAtDesc();
    Long countByIsVerified(Boolean isVerified);
}
