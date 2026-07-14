package com.ngo.repository;

import com.ngo.entity.PasswordResetRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, Long> {
    Optional<PasswordResetRequest> findByEmail(String email);
    Optional<PasswordResetRequest> findByToken(String token);
    Optional<PasswordResetRequest> findByEmailAndStatus(String email, String status);
    Optional<PasswordResetRequest> findByTokenAndStatus(String token, String status);
    List<PasswordResetRequest> findByStatusOrderByCreatedAtDesc(String status);

    @Modifying
    @Query("DELETE FROM PasswordResetRequest p WHERE lower(p.userType) = lower(:userType) " +
            "AND (p.userId = :userId OR lower(p.email) = lower(:email))")
    int deleteForAccount(@Param("userType") String userType, @Param("userId") Long userId,
                         @Param("email") String email);
}
