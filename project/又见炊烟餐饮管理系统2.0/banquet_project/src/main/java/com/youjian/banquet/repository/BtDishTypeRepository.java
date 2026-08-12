package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BtDishType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 菜品类型 Repository
 */
@Repository
public interface BtDishTypeRepository extends JpaRepository<BtDishType, Long>, JpaSpecificationExecutor<BtDishType> {
}