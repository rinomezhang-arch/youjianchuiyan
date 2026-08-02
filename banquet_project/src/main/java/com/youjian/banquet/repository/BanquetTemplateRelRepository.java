package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BanquetTemplateRel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BanquetTemplateRelRepository extends JpaRepository<BanquetTemplateRel, Integer> {
    List<BanquetTemplateRel> findByStoreId(Long storeId);
    List<BanquetTemplateRel> findByBanquetTypeId(Integer banquetTypeId);
    List<BanquetTemplateRel> findByTemplateId(Integer templateId);
    List<BanquetTemplateRel> findByStoreIdAndBanquetTypeId(Long storeId, Integer banquetTypeId);
}
