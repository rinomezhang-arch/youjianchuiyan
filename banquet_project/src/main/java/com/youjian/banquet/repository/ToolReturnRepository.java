package com.youjian.banquet.repository;

import com.youjian.banquet.entity.ToolReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolReturnRepository extends JpaRepository<ToolReturn, Long> {
    List<ToolReturn> findByIssueId(Long issueId);
    List<ToolReturn> findByToolId(Long toolId);
    List<ToolReturn> findByStaffId(Integer staffId);
    Optional<ToolReturn> findByReturnNo(String returnNo);
}
