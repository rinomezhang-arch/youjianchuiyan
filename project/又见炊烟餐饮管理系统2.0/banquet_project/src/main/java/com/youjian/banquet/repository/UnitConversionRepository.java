package com.youjian.banquet.repository;

import com.youjian.banquet.entity.UnitConversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UnitConversionRepository extends JpaRepository<UnitConversion, Long> {
    List<UnitConversion> findByStoreId(Long storeId);
    List<UnitConversion> findByFromUnitAndToUnit(String fromUnit, String toUnit);
}
