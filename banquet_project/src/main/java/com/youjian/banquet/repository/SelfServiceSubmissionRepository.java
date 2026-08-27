package com.youjian.banquet.repository;

import com.youjian.banquet.entity.SelfServiceSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SelfServiceSubmissionRepository extends JpaRepository<SelfServiceSubmission, Long> {
    List<SelfServiceSubmission> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<SelfServiceSubmission> findAllByOrderByCreatedAtDesc();
}
