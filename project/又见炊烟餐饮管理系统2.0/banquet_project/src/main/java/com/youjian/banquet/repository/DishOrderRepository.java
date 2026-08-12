package com.youjian.banquet.repository;

import com.youjian.banquet.entity.DishOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 订单 Repository
 */
@Repository
public interface DishOrderRepository extends JpaRepository<DishOrder, Long>, JpaSpecificationExecutor<DishOrder> {
}