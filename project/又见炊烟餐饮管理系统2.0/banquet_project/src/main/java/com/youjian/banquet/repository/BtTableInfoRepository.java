package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BtTableInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 餐桌信息 Repository
 */
@Repository
public interface BtTableInfoRepository extends JpaRepository<BtTableInfo, Long>, JpaSpecificationExecutor<BtTableInfo> {

    Page<BtTableInfo> findByCanzhuozhuangtai(String canzhuozhuangtai, Pageable pageable);

    Page<BtTableInfo> findByCanzhuohaomaContaining(String canzhuohaoma, Pageable pageable);

    BtTableInfo findByCanzhuohaoma(String canzhuohaoma);
}