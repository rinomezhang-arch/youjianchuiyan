package com.youjian.banquet.repository;

import com.youjian.banquet.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    List<JobPosting> findByStatusOrderByCreatedAtDesc(String status);
    List<JobPosting> findAllByOrderByCreatedAtDesc();
}
