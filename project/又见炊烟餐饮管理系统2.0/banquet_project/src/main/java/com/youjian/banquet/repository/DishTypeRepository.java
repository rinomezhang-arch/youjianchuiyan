package com.youjian.banquet.repository;

import com.youjian.banquet.entity.DishType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 菜品类型 Repository
 */
@Repository
public interface DishTypeRepository extends JpaRepository<DishType, Long>, JpaSpecificationExecutor<DishType> {
}