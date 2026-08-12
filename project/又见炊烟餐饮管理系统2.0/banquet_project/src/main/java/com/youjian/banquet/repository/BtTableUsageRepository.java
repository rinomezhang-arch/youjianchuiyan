package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BtTableUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 餐桌使用 Repository
 */
@Repository
public interface BtTableUsageRepository extends JpaRepository<BtTableUsage, Long>, JpaSpecificationExecutor<BtTableUsage> {

    Page<BtTableUsage> findByYonghuming(String yonghuming, Pageable pageable);

    Page<BtTableUsage> findByCanzhuohaoma(String canzhuohaoma, Pageable pageable);

    List<BtTableUsage> findByYonghuming(String yonghuming);
}