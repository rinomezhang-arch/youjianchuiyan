package com.youjian.banquet.repository;

import com.youjian.banquet.entity.InventorySummary;
import com.youjian.banquet.projection.InventorySummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventorySummaryRepository extends JpaRepository<InventorySummary, Long> {

    @Query(value = "SELECT s.ingredient_id AS ingredientId, " +
                   "m.ingredient_name AS ingredientName, " +
                   "s.total_quantity AS totalQuantity, " +
                   "s.total_cost AS totalCost, " +
                   "s.avg_unit_price AS avgUnitPrice " +
                   "FROM inventory_summary s " +
                   "LEFT JOIN ingredient_master m ON s.ingredient_id = m.ingredient_id AND s.store_id = m.store_id " +
                   "WHERE s.store_id = :storeId", nativeQuery = true)
    List<InventorySummaryProjection> findSummaryWithNameByStoreId(@Param("storeId") Long storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InventorySummary s WHERE s.storeId = :storeId AND s.ingredientId = :ingredientId")
    Optional<InventorySummary> findByStoreIdAndIngredientIdForUpdate(@Param("storeId") Long storeId,
                                                                     @Param("ingredientId") String ingredientId);

    Optional<InventorySummary> findByStoreIdAndIngredientId(Long storeId, String ingredientId);

    List<InventorySummary> findByStoreId(Long storeId);
}
