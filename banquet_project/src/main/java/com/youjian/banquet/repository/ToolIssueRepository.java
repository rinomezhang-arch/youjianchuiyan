package com.youjian.banquet.repository;

import com.youjian.banquet.entity.ToolIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolIssueRepository extends JpaRepository<ToolIssue, Long> {
    List<ToolIssue> findByStoreId(Long storeId);
    List<ToolIssue> findByStoreIdAndReturnStatus(Long storeId, String returnStatus);
    List<ToolIssue> findByStaffId(Integer staffId);
    List<ToolIssue> findByToolId(Long toolId);
    Optional<ToolIssue> findByIssueNo(String issueNo);
}
