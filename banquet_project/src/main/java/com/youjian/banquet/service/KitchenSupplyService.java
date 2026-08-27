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
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;
    private final MaterialRequisitionRepository materialRequisitionRepository;
    private final PreprocessingRecordRepository preprocessingRecordRepository;
    private final CostCardRepository costCardRepository;
    private final UnitConversionRepository unitConversionRepository;
    private final DishMasterRepository dishMasterRepository;
    private final DishRecipeRepository dishRecipeRepository;
    private final IngredientMasterRepository ingredientMasterRepository;
    private final IngredientInventoryLogRepository inventoryLogRepository;
    private final InventoryService inventoryService;

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
        request.setApproverName(approver);
        request.setApproveTime(LocalDateTime.now());
        return purchaseRequestRepository.save(request);
    }

    /**
     * 入库验收。之前这里只存一条没有明细行的"总单"，updateInventoryOnReceipt
     * 还硬编码了 ingredientId="I001"（不管实际收的是什么原料）、从不设置数量、
     * 从不真正改 IngredientMaster.currentStock——入库单存了个寂寞，跟真实库存
     * 完全脱节。现在改成接收明细行(每行原料+实收数量+单价)，验收通过(ACCEPTED)
     * 时逐行调用 InventoryService.stockIn(真实入库逻辑，已经在库存管理页验证过)，
     * 真正把货物计入库存。
     */
    @Transactional
    public GoodsReceipt createGoodsReceipt(GoodsReceipt receipt, List<GoodsReceiptItem> items) {
        if (receipt.getStoreId() == null) {
            receipt.setStoreId(1L);
        }
        if (receipt.getStatus() == null) {
            receipt.setStatus("PENDING");
        }
        if (receipt.getReceiptNo() == null) {
            receipt.setReceiptNo("GR" + System.currentTimeMillis());
        }
        if (receipt.getReceiptDate() == null) {
            receipt.setReceiptDate(LocalDate.now());
        }

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmt = BigDecimal.ZERO;
        if (items != null) {
            for (GoodsReceiptItem item : items) {
                BigDecimal qty = item.getActualQuantity() != null ? item.getActualQuantity() : BigDecimal.ZERO;
                BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                if (item.getAmount() == null) item.setAmount(qty.multiply(price));
                totalQty = totalQty.add(qty);
                totalAmt = totalAmt.add(item.getAmount());
            }
        }
        receipt.setTotalQuantity(totalQty);
        receipt.setTotalAmount(totalAmt);

        GoodsReceipt saved = goodsReceiptRepository.save(receipt);

        if (items != null) {
            int lineNo = 1;
            for (GoodsReceiptItem item : items) {
                item.setDetailId(null);
                item.setReceiptId(saved.getReceiptId());
                item.setStoreId(saved.getStoreId());
                item.setLineNo(lineNo++);
                if (item.getCreatedAt() == null) item.setCreatedAt(LocalDateTime.now());
                goodsReceiptItemRepository.save(item);
            }
        }

        if ("ACCEPTED".equals(saved.getStatus()) && items != null) {
            updateInventoryOnReceipt(saved, items);
        }

        // purchase_receipt 表通过 order_id 关联 purchase_order，不再直接关联 procurement_request
        // 如需关联更新采购申请状态，可通过 order_id → purchase_order → procurement_request 链路查询

        return saved;
    }

    public List<GoodsReceiptItem> getGoodsReceiptItems(Long receiptId) {
        return goodsReceiptItemRepository.findByReceiptId(receiptId);
    }

    private void updateInventoryOnReceipt(GoodsReceipt receipt, List<GoodsReceiptItem> items) {
        for (GoodsReceiptItem item : items) {
            if (item.getIngredientId() == null || item.getIngredientId().isBlank()) continue;
            com.youjian.banquet.dto.InventoryDTO dto = new com.youjian.banquet.dto.InventoryDTO();
            dto.setStoreId(String.valueOf(receipt.getStoreId()));
            dto.setIngredientId(item.getIngredientId());
            dto.setIngredientName(item.getIngredientName());
            dto.setQuantity(item.getActualQuantity());
            dto.setReferenceId(String.valueOf(receipt.getReceiptId()));
            dto.setReferenceType("GOODS_RECEIPT");
            dto.setOperator(receipt.getWarehouseKeeperName());
            dto.setNotes("入库单 " + receipt.getReceiptNo());
            inventoryService.stockIn(dto);
        }
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

        BigDecimal totalCost = materialCost;

        BigDecimal sellingPrice = dish.getSalePrice();
        BigDecimal grossMargin = BigDecimal.ZERO;
        if (sellingPrice != null && sellingPrice.compareTo(BigDecimal.ZERO) > 0) {
            grossMargin = sellingPrice.subtract(totalCost)
                    .divide(sellingPrice, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }

        CostCard costCard = costCardRepository.findByDishIdAndStoreId(dishId, storeId)
                .orElse(new CostCard());
        costCard.setStoreId(storeId);
        costCard.setDishId(dishId);
        costCard.setDishName(dish.getDishName());
        costCard.setStandardCost(materialCost);
        costCard.setActualCost(totalCost);
        costCard.setSellingPrice(sellingPrice);
        costCard.setGrossMargin(grossMargin);
        costCard.setStatus("active");

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
