package com.youjian.banquet.repository;

import com.youjian.banquet.entity.DishReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 菜品评论 Repository
 */
@Repository
public interface DishReviewRepository extends JpaRepository<DishReview, Long>, JpaSpecificationExecutor<DishReview> {
}