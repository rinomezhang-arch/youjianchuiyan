package com.youjian.banquet.service;

import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KitchenSupplyService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final MaterialRequisitionRepository materialRequisitionRepository;
    private final PreprocessingRecordRepository preprocessingRecordRepository;
    private final CostCardRepository costCardRepository;
    private final UnitConversionRepository unitConversionRepository;
    private final DishMasterRepository dishMasterRepository;
    private final DishRecipeRepository dishRecipeRepository;
    private final IngredientMasterRepository ingredientMasterRepository;
    private final IngredientInventoryLogRepository inventoryLogRepository;

    @Transactional
    public PurchaseRequest createPurchaseRequest(PurchaseRequest request) {
        if (request.getStoreId() == null) {
            request.setStoreId(1L);
        }
        if (request.getStatus() == null) {
            request.setStatus("PENDING");
        }
        if (request.getRequestDate() == null) {
            request.setRequestDate(LocalDate.now());
        }
        if (request.getRequestNo() == null) {
            request.setRequestNo("PR" + System.currentTimeMillis());
        }
        return purchaseRequestRepository.save(request);
    }

    @Transactional
    public PurchaseRequest approvePurchaseRequest(Long requestId, String approver) {
        PurchaseRequest request = purchaseRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("采购申请不存在"));
        request.setStatus("APPROVED");
        request.setRequestedBy(approver);
        return purchaseRequestRepository.save(request);
    }

    @Transactional
    public GoodsReceipt createGoodsReceipt(GoodsReceipt receipt) {
        if (receipt.getStoreId() == null) {
            receipt.setStoreId(1L);
        }
        if (receipt.getStatus() == null) {
            receipt.setStatus("PENDING");
        }
        GoodsReceipt saved = goodsReceiptRepository.save(receipt);

        if ("ACCEPTED".equals(receipt.getStatus())) {
            updateInventoryOnReceipt(saved);
        }

        if (receipt.getRequestId() != null) {
            PurchaseRequest request = purchaseRequestRepository.findById(receipt.getRequestId())
                    .orElse(null);
            if (request != null) {
                request.setStatus("RECEIVED");
                purchaseRequestRepository.save(request);
            }
        }

        return saved;
    }

    private void updateInventoryOnReceipt(GoodsReceipt receipt) {
        IngredientInventoryLog log = new IngredientInventoryLog();
        log.setStoreId(receipt.getStoreId());
        log.setIngredientId("I001");
        log.setChangeType("IN");
        log.setReferenceType("GOODS_RECEIPT");
        log.setReferenceId(String.valueOf(receipt.getReceiptId()));
        inventoryLogRepository.save(log);
    }

    @Transactional
    public MaterialRequisition createRequisition(MaterialRequisition requisition) {
        if (requisition.getStoreId() == null) {
            requisition.setStoreId(1L);
        }
        if (requisition.getStatus() == null) {
            requisition.setStatus("PENDING");
        }
        if (requisition.getRequisitionDate() == null) {
            requisition.setRequisitionDate(LocalDate.now());
        }
        return materialRequisitionRepository.save(requisition);
    }

    @Transactional
    public MaterialRequisition approveRequisition(Long requisitionId, String approver) {
        MaterialRequisition requisition = materialRequisitionRepository.findById(requisitionId)
                .orElseThrow(() -> new RuntimeException("领料单不存在"));
        requisition.setStatus("APPROVED");
        requisition.setApprovedBy(approver);
        return materialRequisitionRepository.save(requisition);
    }

    @Transactional
    public PreprocessingRecord createPreprocessingRecord(PreprocessingRecord record) {
        if (record.getStoreId() == null) {
            record.setStoreId(1L);
        }
        if (record.getRawQty() != null && record.getProcessedQty() != null) {
            BigDecimal yieldRate = record.getProcessedQty()
                    .divide(record.getRawQty(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            record.setYieldRate(yieldRate);
        }
        return preprocessingRecordRepository.save(record);
    }

    @Transactional
    public CostCard calculateAndSaveCostCard(String dishId, Long storeId) {
        DishMaster dish = dishMasterRepository.findById(
                new DishMaster.DishMasterId(dishId, storeId))
                .orElseThrow(() -> new RuntimeException("菜品不存在"));

        List<DishRecipe> recipes = dishRecipeRepository.findByDishIdAndStoreId(dishId, storeId);

        BigDecimal materialCost = BigDecimal.ZERO;
        for (DishRecipe recipe : recipes) {
            IngredientMaster ingredient = ingredientMasterRepository.findById(
                    new IngredientMaster.IngredientMasterId(recipe.getIngredientId(), storeId))
                    .orElse(null);
            if (ingredient != null && ingredient.getUnitPrice() != null) {
                BigDecimal cost = ingredient.getUnitPrice().multiply(recipe.getQuantity());
                materialCost = materialCost.add(cost);
            }
        }

        BigDecimal laborCost = materialCost.multiply(new BigDecimal("0.15"));
        BigDecimal overheadCost = materialCost.multiply(new BigDecimal("0.10"));
        BigDecimal totalCost = materialCost.add(laborCost).add(overheadCost);

        BigDecimal calculatedPrice = totalCost.divide(new BigDecimal("0.4"), 2, RoundingMode.HALF_UP);
        BigDecimal costRate = totalCost.divide(calculatedPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        CostCard costCard = costCardRepository.findByDishIdAndStoreId(dishId, storeId)
                .orElse(new CostCard());
        costCard.setStoreId(storeId);
        costCard.setDishId(dishId);
        costCard.setDishName(dish.getDishName());
        costCard.setMaterialCost(materialCost);
        costCard.setLaborCost(laborCost);
        costCard.setOverheadCost(overheadCost);
        costCard.setTotalCost(totalCost);
        costCard.setCalculatedPrice(calculatedPrice);
        costCard.setCostRate(costRate);
        costCard.setSellPrice(dish.getSalePrice());
        costCard.setStatus("ACTIVE");

        return costCardRepository.save(costCard);
    }

    @Transactional
    public UnitConversion addUnitConversion(UnitConversion conversion) {
        if (conversion.getStoreId() == null) {
            conversion.setStoreId(1L);
        }
        return unitConversionRepository.save(conversion);
    }

    public List<PurchaseRequest> getPurchaseRequests(Long storeId, String status) {
        if (status != null && !status.isEmpty()) {
            return purchaseRequestRepository.findByStoreIdAndStatus(storeId, status);
        }
        return purchaseRequestRepository.findByStoreId(storeId);
    }

    public List<GoodsReceipt> getGoodsReceipts(Long storeId, String status) {
        if (status != null && !status.isEmpty()) {
            return goodsReceiptRepository.findByStoreIdAndStatus(storeId, status);
        }
        return goodsReceiptRepository.findByStoreId(storeId);
    }

    public List<MaterialRequisition> getRequisitions(Long storeId, String status) {
        if (status != null && !status.isEmpty()) {
            return materialRequisitionRepository.findByStoreIdAndStatus(storeId, status);
        }
        return materialRequisitionRepository.findByStoreId(storeId);
    }

    public List<PreprocessingRecord> getPreprocessingRecords(Long storeId, String ingredientId) {
        if (ingredientId != null && !ingredientId.isEmpty()) {
            return preprocessingRecordRepository.findByIngredientId(ingredientId);
        }
        return preprocessingRecordRepository.findByStoreId(storeId);
    }

    public List<CostCard> getCostCards(Long storeId) {
        return costCardRepository.findByStoreId(storeId);
    }

    public Optional<CostCard> getCostCard(String dishId, Long storeId) {
        return costCardRepository.findByDishIdAndStoreId(dishId, storeId);
    }

    public List<UnitConversion> getUnitConversions(Long storeId) {
        return unitConversionRepository.findByStoreId(storeId);
    }
}
