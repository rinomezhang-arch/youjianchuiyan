package com.youjian.banquet.repository;

import com.youjian.banquet.entity.DishStoreup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 收藏 Repository
 */
@Repository
public interface DishStoreupRepository extends JpaRepository<DishStoreup, Long>, JpaSpecificationExecutor<DishStoreup> {
}