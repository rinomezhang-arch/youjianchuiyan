package com.youjian.banquet.service;

import com.youjian.banquet.entity.InventorySummary;
import com.youjian.banquet.projection.InventorySummaryProjection;
import com.youjian.banquet.repository.InventorySummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventorySummaryService {

    private final InventorySummaryRepository summaryRepository;

    @Transactional(rollbackFor = Exception.class)
    public void refreshSummary(Long storeId, String ingredientId, BigDecimal deltaQuantity, BigDecimal deltaCost) {
        InventorySummary summary = summaryRepository
                .findByStoreIdAndIngredientIdForUpdate(storeId, ingredientId)
                .orElseGet(() -> {
                    InventorySummary newOne = new InventorySummary();
                    newOne.setStoreId(storeId);
                    newOne.setIngredientId(ingredientId);
                    newOne.setTotalQuantity(BigDecimal.ZERO);
                    newOne.setTotalCost(BigDecimal.ZERO);
                    newOne.setAvgUnitPrice(BigDecimal.ZERO);
                    return newOne;
                });

        BigDecimal newTotalQty = summary.getTotalQuantity().add(deltaQuantity);
        BigDecimal newTotalCost = summary.getTotalCost().add(deltaCost);
        BigDecimal newAvgPrice = BigDecimal.ZERO;
        if (newTotalQty.compareTo(BigDecimal.ZERO) != 0) {
            newAvgPrice = newTotalCost.divide(newTotalQty, 4, RoundingMode.HALF_UP);
        }

        if (newTotalQty.compareTo(BigDecimal.ZERO) < 0) {
            log.error("库存汇总更新后为负数！门店: {}, 食材: {}, 当前数量: {}", storeId, ingredientId, newTotalQty);
            throw new RuntimeException("库存数量不能为负数，请检查单据数据");
        }

        summary.setTotalQuantity(newTotalQty);
        summary.setTotalCost(newTotalCost);
        summary.setAvgUnitPrice(newAvgPrice);
        summaryRepository.save(summary);
        log.info("库存汇总更新成功: {} -> 数量={}, 均价={}", ingredientId, newTotalQty, newAvgPrice);
    }

    @Transactional(readOnly = true)
    public List<InventorySummaryProjection> getSummaryByStore(Long storeId) {
        return summaryRepository.findSummaryWithNameByStoreId(storeId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void rebuildAllSummary() {
        log.info("开始全量重建库存汇总...");
        summaryRepository.deleteAll();
        log.info("全量重建完成");
    }
}
