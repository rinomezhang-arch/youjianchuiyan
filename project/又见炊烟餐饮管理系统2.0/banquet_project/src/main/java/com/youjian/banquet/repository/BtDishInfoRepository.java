package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BtDishInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 菜品信息 Repository
 */
@Repository
public interface BtDishInfoRepository extends JpaRepository<BtDishInfo, Long>, JpaSpecificationExecutor<BtDishInfo> {

    Page<BtDishInfo> findByCaipinleixing(String caipinleixing, Pageable pageable);

    Page<BtDishInfo> findByCaipinmingchengContaining(String caipinmingcheng, Pageable pageable);

    @Query("SELECT d FROM BtDishInfo d WHERE (:caipinmingcheng IS NULL OR d.caipinmingcheng LIKE %:caipinmingcheng%) AND (:caipinleixing IS NULL OR d.caipinleixing = :caipinleixing)")
    Page<BtDishInfo> search(@Param("caipinmingcheng") String caipinmingcheng, @Param("caipinleixing") String caipinleixing, Pageable pageable);

    @Query("SELECT d FROM BtDishInfo d WHERE (:pricestart IS NULL OR d.price >= :pricestart) AND (:priceend IS NULL OR d.price <= :priceend)")
    Page<BtDishInfo> findByPriceBetween(@Param("pricestart") Double pricestart, @Param("priceend") Double priceend, Pageable pageable);

    @Query("SELECT d FROM BtDishInfo d WHERE (:caipinmingcheng IS NULL OR d.caipinmingcheng LIKE %:caipinmingcheng%) AND (:caipinleixing IS NULL OR d.caipinleixing = :caipinleixing) AND (:pricestart IS NULL OR d.price >= :pricestart) AND (:priceend IS NULL OR d.price <= :priceend)")
    Page<BtDishInfo> searchWithPrice(@Param("caipinmingcheng") String caipinmingcheng,
                                     @Param("caipinleixing") String caipinleixing,
                                     @Param("pricestart") Double pricestart,
                                     @Param("priceend") Double priceend,
                                     Pageable pageable);

    List<BtDishInfo> findByOrderByClicktimeDesc(Pageable pageable);

    List<BtDishInfo> findByCaipinleixingOrderByClicktimeDesc(String caipinleixing, Pageable pageable);

    @Query("SELECT d FROM BtDishInfo d WHERE d.caipinleixing = :caipinleixing ORDER BY d.id DESC")
    List<BtDishInfo> findByCaipinleixingList(@Param("caipinleixing") String caipinleixing, Pageable pageable);
}