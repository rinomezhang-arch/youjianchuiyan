package com.youjian.banquet.repository;

import com.youjian.banquet.entity.ToolCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolCategoryRepository extends JpaRepository<ToolCategory, Long> {
    List<ToolCategory> findByParentId(Long parentId);
    List<ToolCategory> findByParentIdIsNull();
    Optional<ToolCategory> findByCategoryName(String categoryName);
}
