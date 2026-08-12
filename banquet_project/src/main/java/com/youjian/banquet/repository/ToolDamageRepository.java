package com.youjian.banquet.repository;

import com.youjian.banquet.entity.ToolDamage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolDamageRepository extends JpaRepository<ToolDamage, Long> {
    List<ToolDamage> findByStoreId(Long storeId);
    List<ToolDamage> findByStoreIdAndStatus(Long storeId, String status);
    List<ToolDamage> findByToolId(Long toolId);
    List<ToolDamage> findByStaffId(Integer staffId);
    Optional<ToolDamage> findByDamageNo(String damageNo);
}
