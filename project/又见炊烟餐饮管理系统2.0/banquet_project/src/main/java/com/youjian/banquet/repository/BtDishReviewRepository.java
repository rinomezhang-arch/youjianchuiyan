package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BtDishReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 菜品评论 Repository
 */
@Repository
public interface BtDishReviewRepository extends JpaRepository<BtDishReview, Long>, JpaSpecificationExecutor<BtDishReview> {

    Page<BtDishReview> findByRefid(Long refid, Pageable pageable);

    List<BtDishReview> findByRefidOrderByAddtimeDesc(Long refid);

    Page<BtDishReview> findByUserid(Long userid, Pageable pageable);
}