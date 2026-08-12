package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PrMaterialReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrMaterialReviewRepository extends JpaRepository<PrMaterialReview, Long>, JpaSpecificationExecutor<PrMaterialReview> {

    List<PrMaterialReview> findByRefid(Long refid);

    List<PrMaterialReview> findByUserid(Long userid);
}