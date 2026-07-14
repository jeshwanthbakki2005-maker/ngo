package com.ngo.repository;

import com.ngo.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByCreatedAtDesc();
    List<AuditLog> findByUserTypeOrderByCreatedAtDesc(String userType);

    @Modifying
    @Query("DELETE FROM AuditLog a WHERE lower(a.userType) = lower(:userType) AND a.userId = :userId")
    int deleteForAccount(@Param("userType") String userType, @Param("userId") Long userId);
}
