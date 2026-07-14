package com.ngo.repository;

import com.ngo.entity.NFSA;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NFSARepository extends JpaRepository<NFSA, Long> {
    Optional<NFSA> findByEmail(String email);
    Optional<NFSA> findByEmailIgnoreCase(String email);
    Optional<NFSA> findFirstByPhone(String phone);
}
