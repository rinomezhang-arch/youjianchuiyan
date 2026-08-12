package com.youjian.banquet.repository;

import com.youjian.banquet.entity.TableUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 餐桌使用记录 Repository
 */
@Repository
public interface TableUsageRepository extends JpaRepository<TableUsage, Long>, JpaSpecificationExecutor<TableUsage> {
}