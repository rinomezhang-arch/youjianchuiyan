package com.youjian.banquet.repository;

import com.youjian.banquet.entity.ChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChangeLogRepository extends JpaRepository<ChangeLog, Long> {
    List<ChangeLog> findByStoreId(Long storeId);
    List<ChangeLog> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<ChangeLog> findByOperatorId(Integer operatorId);
    List<ChangeLog> findByOperationType(String operationType);
    List<ChangeLog> findByTargetType(String targetType);
}
