package com.youjian.banquet.repository;

import com.youjian.banquet.entity.TableInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 餐桌信息 Repository
 */
@Repository
public interface TableInfoRepository extends JpaRepository<TableInfo, Long>, JpaSpecificationExecutor<TableInfo> {
}