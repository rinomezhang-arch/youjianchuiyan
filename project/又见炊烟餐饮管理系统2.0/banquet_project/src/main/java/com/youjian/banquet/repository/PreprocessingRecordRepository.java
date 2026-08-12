package com.youjian.banquet.repository;

import com.youjian.banquet.entity.PreprocessingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PreprocessingRecordRepository extends JpaRepository<PreprocessingRecord, Long> {
    List<PreprocessingRecord> findByStoreId(Long storeId);
    List<PreprocessingRecord> findByIngredientId(String ingredientId);
}
