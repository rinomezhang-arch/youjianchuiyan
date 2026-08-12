package com.youjian.banquet.repository;

import com.youjian.banquet.entity.DishCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 购物车 Repository
 */
@Repository
public interface DishCartRepository extends JpaRepository<DishCart, Long>, JpaSpecificationExecutor<DishCart> {
}