package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BtOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 订单 Repository
 */
@Repository
public interface BtOrderRepository extends JpaRepository<BtOrder, Long>, JpaSpecificationExecutor<BtOrder> {

    Page<BtOrder> findByUserid(Long userid, Pageable pageable);

    List<BtOrder> findByUseridAndTablenameOrderByAddtimeDesc(Long userid, String tablename);

    Page<BtOrder> findByStatus(String status, Pageable pageable);

    @Query("SELECT o FROM BtOrder o WHERE o.userid = :userid AND o.status IN :statuses")
    List<BtOrder> findByUseridAndStatusIn(@Param("userid") Long userid, @Param("statuses") List<String> statuses);

    BtOrder findByOrderid(String orderid);

    @Query("SELECT o FROM BtOrder o WHERE o.status IN :statuses")
    List<BtOrder> findByStatusIn(@Param("statuses") List<String> statuses);

    @Query(value = "SELECT DATE_FORMAT(addtime, '%Y-%m-%d') as xColumn, COALESCE(SUM(total),0) as yColumn FROM orders WHERE status IN ('已支付','已发货','已完成') GROUP BY DATE_FORMAT(addtime, '%Y-%m-%d') ORDER BY addtime", nativeQuery = true)
    List<Map<String, Object>> selectDailyTotalValue();

    @Query(value = "SELECT goodtype as xColumn, COALESCE(SUM(total),0) as yColumn FROM orders WHERE status IN ('已支付','已发货','已完成') GROUP BY goodtype", nativeQuery = true)
    List<Map<String, Object>> selectGroupByGoodtype();

    @Query(value = "SELECT status as xColumn, COUNT(*) as yColumn FROM orders GROUP BY status", nativeQuery = true)
    List<Map<String, Object>> selectGroupByStatus();
}