package com.youjian.banquet.repository;

import com.youjian.banquet.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {
    List<AuditLog> findByStoreId(Long storeId);
    List<AuditLog> findByUserId(String userId);
    List<AuditLog> findByAction(String action);
}
