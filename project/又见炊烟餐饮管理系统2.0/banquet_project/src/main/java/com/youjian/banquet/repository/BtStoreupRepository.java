package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BtStoreup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 收藏 Repository
 */
@Repository
public interface BtStoreupRepository extends JpaRepository<BtStoreup, Long>, JpaSpecificationExecutor<BtStoreup> {

    Page<BtStoreup> findByUserid(Long userid, Pageable pageable);

    List<BtStoreup> findByUserid(Long userid);

    Optional<BtStoreup> findByUseridAndRefidAndType(Long userid, Long refid, String type);

    void deleteByUseridAndRefidAndType(Long userid, Long refid, String type);
}