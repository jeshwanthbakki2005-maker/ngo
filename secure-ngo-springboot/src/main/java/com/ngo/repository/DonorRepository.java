package com.ngo.repository;

import com.ngo.entity.Donor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonorRepository extends JpaRepository<Donor, Long> {
    Optional<Donor> findByEmail(String email);
    Optional<Donor> findByEmailIgnoreCase(String email);
    Optional<Donor> findFirstByPhone(String phone);
    List<Donor> findByIsVerified(Boolean isVerified);
    List<Donor> findAllByOrderByCreatedAtDesc();
    Long countByIsVerified(Boolean isVerified);
}
