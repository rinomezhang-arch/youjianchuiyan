package com.youjian.banquet.repository;

import com.youjian.banquet.entity.ApprovalTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApprovalTemplateRepository extends JpaRepository<ApprovalTemplate, Long> {

    /** 按类型查找启用的全局模板（store_id = 0） */
    Optional<ApprovalTemplate> findFirstByTemplateTypeAndStoreIdAndIsActive(
            String templateType, Long storeId, Integer isActive);

    /** 按类型查找任一启用的模板（回退） */
    Optional<ApprovalTemplate> findFirstByTemplateTypeAndIsActive(
            String templateType, Integer isActive);
}
