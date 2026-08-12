package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BtCart;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 购物车 Repository
 */
@Repository
public interface BtCartRepository extends JpaRepository<BtCart, Long>, JpaSpecificationExecutor<BtCart> {

    Page<BtCart> findByUserid(Long userid, Pageable pageable);

    List<BtCart> findByUserid(Long userid);

    List<BtCart> findByUseridAndGoodid(Long userid, Long goodid);

    void deleteByUseridAndGoodid(Long userid, Long goodid);
}