package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BanquetTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BanquetTemplateRepository extends JpaRepository<BanquetTemplate, Integer> {
    List<BanquetTemplate> findByTemplateType(String templateType);
    List<BanquetTemplate> findByIsActive(Integer isActive);
    List<BanquetTemplate> findByStoreId(Long storeId);
    List<BanquetTemplate> findByStoreIdAndIsActive(Long storeId, Integer isActive);
    List<BanquetTemplate> findByStoreIdAndTemplateType(Long storeId, String templateType);
    Optional<BanquetTemplate> findByTemplateCode(String templateCode);
}
