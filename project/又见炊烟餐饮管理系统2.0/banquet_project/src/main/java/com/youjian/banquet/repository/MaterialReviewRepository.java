package com.youjian.banquet.repository;

import com.youjian.banquet.entity.MaterialReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaterialReviewRepository extends JpaRepository<MaterialReview, Long>, JpaSpecificationExecutor<MaterialReview> {

    List<MaterialReview> findByRefId(Long refId);

    List<MaterialReview> findByUserId(Long userId);
}