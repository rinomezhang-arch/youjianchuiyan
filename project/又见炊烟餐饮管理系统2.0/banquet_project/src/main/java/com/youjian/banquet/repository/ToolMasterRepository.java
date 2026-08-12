package com.youjian.banquet.repository;

import com.youjian.banquet.entity.ToolMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolMasterRepository extends JpaRepository<ToolMaster, Long> {
    List<ToolMaster> findByStoreId(Long storeId);
    List<ToolMaster> findByStoreIdAndStatus(Long storeId, String status);
    List<ToolMaster> findByStoreIdAndCategoryId(Long storeId, Long categoryId);
    Optional<ToolMaster> findByToolNo(String toolNo);
}
