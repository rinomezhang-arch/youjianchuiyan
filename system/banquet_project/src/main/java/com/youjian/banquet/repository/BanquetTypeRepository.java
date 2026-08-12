package com.youjian.banquet.repository;

import com.youjian.banquet.entity.BanquetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BanquetTypeRepository extends JpaRepository<BanquetType, Integer> {
    List<BanquetType> findByStoreId(Long storeId);
    List<BanquetType> findByStoreIdAndIsActive(Long storeId, Integer isActive);
    Optional<BanquetType> findByTypeCode(String typeCode);
}
