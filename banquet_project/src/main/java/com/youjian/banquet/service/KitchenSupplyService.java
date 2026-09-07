package com.youjian.banquet.service;

import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import lombok.RequiredArgsConstructor;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KitchenSupplyService {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MaterialRequisitionItemRepository requisitionItems;

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestItemRepository purchaseRequestItemRepository;
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
    public PurchaseRequest createPurchaseRequestWithItems(PurchaseRequest request, List<PurchaseRequestItem> items) {
        PurchaseRequest saved = createPurchaseRequest(request);
        if (items != null) {
            for (PurchaseRequestItem item : items) {
                item.setItemId(null); // 防止调用方误传ID，强制按新增处理
                item.setRequestId(saved.getRequestId());
                purchaseRequestItemRepository.save(item);
            }
        }
        return saved;
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
        if (receipt == null) throw new IllegalArgumentException("请填写入库单");
        if (receipt.getReceiptId() != null) throw new IllegalArgumentException("新增入库单不能覆盖已有单据，请使用验收操作");
        UserContext.assertStoreAccess(receipt.getStoreId());
        if (jdbc.queryForList("SELECT store_id FROM store_info WHERE store_id=? FOR UPDATE", receipt.getStoreId()).isEmpty())
            throw new IllegalArgumentException("门店不存在");
        if (receipt.getSupplierId() != null) {
            var suppliers=jdbc.queryForList("SELECT supplier_name FROM supplier_master WHERE supplier_id=? AND store_id=? AND COALESCE(is_active,1)=1",receipt.getSupplierId(),receipt.getStoreId());
            if(suppliers.isEmpty()) throw new IllegalArgumentException("当前门店没有该有效供应商");
            receipt.setSupplierName(Objects.toString(suppliers.get(0).get("supplier_name"),""));
        }
        if (receipt.getOrderId() != null && jdbc.queryForList("SELECT order_id FROM purchase_order WHERE order_id=? AND store_id=?",receipt.getOrderId(),receipt.getStoreId()).isEmpty())
            throw new IllegalArgumentException("采购订单不存在或不属于当前门店");
        if (receipt.getStatus() == null) {
            receipt.setStatus("PENDING");
        }
        if(receipt.getWarehouseKeeperName()==null || receipt.getWarehouseKeeperName().isBlank())receipt.setWarehouseKeeperName(UserContext.getUsername());
        if (!Set.of("PENDING","ACCEPTED").contains(receipt.getStatus())) throw new IllegalArgumentException("入库单状态无效");
        if (receipt.getReceiptNo() == null || receipt.getReceiptNo().isBlank()) {
            receipt.setReceiptNo("GR" + UUID.randomUUID().toString().replace("-",""));
        }
        if (goodsReceiptRepository.findByStoreId(receipt.getStoreId()).stream().anyMatch(r->receipt.getReceiptNo().equals(r.getReceiptNo())))
            throw new IllegalArgumentException("入库单号已存在，请查看已有单据，避免重复入库");
        if (receipt.getReceiptDate() == null) {
            receipt.setReceiptDate(LocalDate.now());
        }

        validateReceiptItems(receipt,items);

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
        GoodsReceipt receipt=goodsReceiptRepository.findById(receiptId).orElseThrow(()->new IllegalArgumentException("入库单不存在"));
        UserContext.assertStoreAccess(receipt.getStoreId());
        return goodsReceiptItemRepository.findByReceiptId(receiptId);
    }

    private void validateReceiptItems(GoodsReceipt receipt,List<GoodsReceiptItem> items) {
        if(items==null || items.isEmpty())throw new IllegalArgumentException("入库单至少需要一条有效原料明细");
        for(var item:items) if(item==null || item.getIngredientId()==null || item.getIngredientId().isBlank())
            throw new IllegalArgumentException("请为每条入库明细选择原料");
        BigDecimal quantity=BigDecimal.ZERO, amount=BigDecimal.ZERO;
        for(var item:items.stream().sorted(Comparator.comparing(GoodsReceiptItem::getIngredientId)).toList()) {
            var ingredient=ingredientMasterRepository.findForStockUpdate(item.getIngredientId(),receipt.getStoreId())
                .orElseThrow(()->new IllegalArgumentException("当前门店没有该原料，请先建立有效档案"));
            if(Integer.valueOf(0).equals(ingredient.getIsActive()))throw new IllegalArgumentException("入库原料已停用");
            checkDecimal(item.getActualQuantity(),"实收数量",2,true,new BigDecimal("99999999.99"));
            checkDecimal(item.getUnitPrice(),"采购单价",8,false,new BigDecimal("9999999.99999999"));
            String unit=ingredient.getPurchaseUnit()!=null?ingredient.getPurchaseUnit():ingredient.getUnit();
            if(unit==null || unit.isBlank())throw new IllegalArgumentException("请先填写原料采购单位");
            if(item.getUnit()!=null && !item.getUnit().isBlank() && !unit.equals(item.getUnit()))
                throw new IllegalArgumentException("实收数量必须使用该原料的采购单位："+unit);
            if("ACCEPTED".equals(receipt.getStatus()) && item.getQualityStatus()!=null && !"QUALIFIED".equals(item.getQualityStatus()))
                throw new IllegalArgumentException("质检不合格的原料不能验收入库");
            item.setIngredientName(ingredient.getIngredientName());item.setUnit(unit);
            item.setAmount(item.getActualQuantity().multiply(item.getUnitPrice()).setScale(2,RoundingMode.HALF_UP));
            checkDecimal(item.getAmount(),"明细金额",2,false,new BigDecimal("9999999999.99"));
            quantity=quantity.add(item.getActualQuantity());amount=amount.add(item.getAmount());
        }
        checkDecimal(quantity,"入库总数量",2,true,new BigDecimal("99999999.99"));
        checkDecimal(amount,"入库总金额",2,false,new BigDecimal("9999999999.99"));
        receipt.setTotalQuantity(quantity);receipt.setTotalAmount(amount);
    }

    @Transactional
    public GoodsReceipt acceptGoodsReceipt(Long receiptId,String operator) {
        GoodsReceipt receipt=goodsReceiptRepository.findForAcceptance(receiptId)
            .orElseThrow(()->new IllegalArgumentException("入库单不存在"));
        UserContext.assertStoreAccess(receipt.getStoreId());
        if("ACCEPTED".equals(receipt.getStatus()))return receipt;
        if(!"PENDING".equals(receipt.getStatus()))throw new IllegalArgumentException("仅待验收单据可以入库");
        List<GoodsReceiptItem> items=goodsReceiptItemRepository.findByReceiptId(receiptId);
        receipt.setStatus("ACCEPTED");
        validateReceiptItems(receipt,items);
        receipt.setWarehouseKeeperName(operator!=null && !operator.isBlank()?operator.trim():UserContext.getUsername());
        goodsReceiptItemRepository.saveAll(items);
        updateInventoryOnReceipt(receipt,items);
        return goodsReceiptRepository.save(receipt);
    }

    private static void checkDecimal(BigDecimal value,String name,int scale,boolean positive,BigDecimal max) {
        if(value==null || value.signum()<0 || (positive && value.signum()==0) || value.stripTrailingZeros().scale()>scale || value.compareTo(max)>0)
            throw new IllegalArgumentException(name+"无效，请核对数值范围与小数位");
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
            inventoryService.stockInReceipt(dto,item.getUnitPrice(),receipt.getReceiptDate());
        }
        // Cost changes propagate in the same transaction; failed recalculation cannot leave stale successful receipts.
        Set<String> affectedDishes=new java.util.TreeSet<>();
        for(var item:items) for(var recipe:dishRecipeRepository.findByIngredientIdAndStoreId(item.getIngredientId(),receipt.getStoreId()))
            affectedDishes.add(recipe.getDishId());
        for(String dishId:affectedDishes)calculateAndSaveCostCard(dishId,receipt.getStoreId());
    }

    @Transactional
    public MaterialRequisition createRequisition(MaterialRequisition requisition) {
        throw new IllegalArgumentException("领料单必须包含原料明细，请填写明细后提交");
    }

    @Transactional
    public MaterialRequisition createRequisitionWithItems(MaterialRequisition requisition,List<MaterialRequisitionItem> items) {
        if(requisition==null || requisition.getRequisitionId()!=null || items==null || items.isEmpty())
            throw new IllegalArgumentException("请提交包含有效明细的新领料单");
        UserContext.assertStoreAccess(requisition.getStoreId());
        if(requisition.getRequestedBy()==null || requisition.getRequestedBy().isBlank())requisition.setRequestedBy(UserContext.getUsername());
        BigDecimal total=BigDecimal.ZERO;
        for(var item:items) {
            if(item==null || item.getIngredientId()==null)throw new IllegalArgumentException("请选择领料原料");
            checkDecimal(item.getQuantity(),"领料数量",3,true,new BigDecimal("9999999.999"));
            var ingredient=ingredientMasterRepository.findById(new IngredientMaster.IngredientMasterId(item.getIngredientId(),requisition.getStoreId()))
                .orElseThrow(()->new IllegalArgumentException("当前门店没有该领料原料"));
            fillRequisitionPrice(item,ingredient);
            total=total.add(item.getAmount());
        }
        requisition.setStatus("PENDING");requisition.setRequisitionNo("MR"+UUID.randomUUID().toString().replace("-",""));
        if(requisition.getRequisitionDate()==null)requisition.setRequisitionDate(LocalDate.now());
        if(requisition.getRequisitionDate().isAfter(LocalDate.now()))throw new IllegalArgumentException("领料日期不能晚于今天");
        checkDecimal(total,"领料总金额",2,false,new BigDecimal("9999999999.99"));
        requisition.setTotalAmount(total);
        var saved=materialRequisitionRepository.save(requisition);
        for(var item:items) {
            item.setItemId(null);item.setStoreId(saved.getStoreId());item.setRequisitionId(saved.getRequisitionId());requisitionItems.save(item);
        }
        return saved;
    }

    private void fillRequisitionPrice(MaterialRequisitionItem item,IngredientMaster ingredient) {
        if(Integer.valueOf(0).equals(ingredient.getIsActive()))throw new IllegalArgumentException("领料原料已停用");
        String unit=ingredient.getPurchaseUnit()!=null?ingredient.getPurchaseUnit():ingredient.getUnit();
        if(unit==null || unit.isBlank() || (item.getUnit()!=null && !item.getUnit().isBlank() && !unit.equals(item.getUnit())))
            throw new IllegalArgumentException("领料数量须使用原料采购单位，请核对换算");
        checkDecimal(ingredient.getUnitPrice(),"领料计价单价",8,false,new BigDecimal("9999999.99999999"));
        item.setIngredientName(ingredient.getIngredientName());item.setUnit(unit);item.setUnitPrice(ingredient.getUnitPrice());
        item.setAmount(item.getQuantity().multiply(item.getUnitPrice()).setScale(2,RoundingMode.HALF_UP));
        checkDecimal(item.getAmount(),"领料明细金额",2,false,new BigDecimal("9999999999.99"));
    }

    public List<MaterialRequisitionItem> getRequisitionItems(Long id) {
        var requisition=materialRequisitionRepository.findById(id).orElseThrow(()->new IllegalArgumentException("领料单不存在"));
        UserContext.assertStoreAccess(requisition.getStoreId());
        return requisitionItems.findByRequisitionIdAndStoreId(id,requisition.getStoreId());
    }

    @Transactional
    public MaterialRequisition approveRequisition(Long requisitionId, String approver) {
        if(approver==null || approver.isBlank())approver=UserContext.getUsername();
        MaterialRequisition requisition = materialRequisitionRepository.findForIssue(requisitionId)
                .orElseThrow(() -> new RuntimeException("领料单不存在"));
        UserContext.assertStoreAccess(requisition.getStoreId());
        if("APPROVED".equals(requisition.getStatus()))return requisition;
        if(!"PENDING".equals(requisition.getStatus()))throw new IllegalArgumentException("该领料单不处于待审批状态");
        var items=requisitionItems.findByRequisitionIdAndStoreId(requisitionId,requisition.getStoreId());
        if(items.isEmpty())throw new IllegalArgumentException("领料单没有原料明细，不能确认出库");
        BigDecimal amount=BigDecimal.ZERO;
        for(var item:items.stream().sorted(Comparator.comparing(MaterialRequisitionItem::getIngredientId)).toList()) {
            checkDecimal(item.getQuantity(),"领料数量",3,true,new BigDecimal("9999999.999"));
            var ingredient=ingredientMasterRepository.findForStockUpdate(item.getIngredientId(),requisition.getStoreId())
                .orElseThrow(()->new IllegalArgumentException("领料原料不存在"));
            fillRequisitionPrice(item,ingredient);requisitionItems.save(item);amount=amount.add(item.getAmount());
            var dto=new com.youjian.banquet.dto.InventoryDTO();dto.setStoreId(requisition.getStoreId().toString());
            dto.setIngredientId(item.getIngredientId());dto.setQuantity(item.getQuantity());dto.setOperator(approver);
            dto.setReferenceType("REQUISITION_ITEM");dto.setReferenceId(item.getItemId().toString());
            inventoryService.stockOut(dto);
        }
        requisition.setTotalAmount(amount);
        requisition.setStatus("APPROVED");
        requisition.setApprovedBy(approver);
        return materialRequisitionRepository.save(requisition);
    }

    @Transactional
    public PreprocessingRecord createPreprocessingRecord(PreprocessingRecord record) {
        if(record==null)throw new IllegalArgumentException("请填写加工记录");
        if(record.getOperator()==null || record.getOperator().isBlank())record.setOperator(UserContext.getUsername());
        UserContext.assertStoreAccess(record.getStoreId());
        checkDecimal(record.getRawQty(),"加工毛料数量",3,true,new BigDecimal("9999999.999"));
        checkDecimal(record.getProcessedQty(),"加工净料数量",3,false,new BigDecimal("9999999.999"));
        if(record.getRecordId()!=null || record.getRequisitionItemId()==null)throw new IllegalArgumentException("新加工记录必须关联已领料明细");
        var source=requisitionItems.findForProcessing(record.getRequisitionItemId())
            .orElseThrow(()->new IllegalArgumentException("加工来源领料明细不存在"));
        if(!record.getStoreId().equals(source.getStoreId()) || !Objects.equals(record.getIngredientId(),source.getIngredientId()))
            throw new IllegalArgumentException("加工原料和门店必须与领料明细一致");
        var requisition=materialRequisitionRepository.findById(source.getRequisitionId())
            .orElseThrow(()->new IllegalArgumentException("加工来源领料单不存在"));
        if(!"APPROVED".equals(requisition.getStatus()))throw new IllegalArgumentException("原料尚未领出，不能登记加工");
        if(record.getUnit()!=null && !source.getUnit().equals(record.getUnit()))throw new IllegalArgumentException("加工毛料和净料数量均须使用领料单位");
        record.setUnit(source.getUnit());record.setIngredientName(source.getIngredientName());
        BigDecimal processed=jdbc.queryForObject("SELECT COALESCE(SUM(raw_qty),0) FROM preprocessing_record WHERE requisition_item_id=? AND store_id=?",BigDecimal.class,source.getItemId(),record.getStoreId());
        if(processed.add(record.getRawQty()).compareTo(source.getQuantity())>0)throw new IllegalArgumentException("累计加工毛料不能超过已领数量");
        var ingredient=ingredientMasterRepository.findForStockUpdate(record.getIngredientId(),record.getStoreId())
            .orElseThrow(()->new IllegalArgumentException("当前门店没有该加工原料"));
        if (record.getRawQty() != null && record.getProcessedQty() != null) {
            BigDecimal yieldRate = record.getProcessedQty().multiply(new BigDecimal("100"))
                    .divide(record.getRawQty(), 2, RoundingMode.HALF_UP);
            record.setYieldRate(yieldRate);
        }
        checkDecimal(record.getYieldRate(),"出成率",2,false,new BigDecimal("999.99"));
        if(record.getRecordDate()==null)record.setRecordDate(LocalDate.now());
        if(record.getRecordDate().isAfter(LocalDate.now()) || record.getRecordDate().isBefore(requisition.getRequisitionDate()))
            throw new IllegalArgumentException("加工日期不得早于领料日期或晚于今天");
        var saved=preprocessingRecordRepository.save(record);
        boolean latestMeasurement=jdbc.queryForObject("SELECT COUNT(*) FROM preprocessing_record WHERE store_id=? AND ingredient_id=? AND record_date>?",Integer.class,record.getStoreId(),record.getIngredientId(),record.getRecordDate())==0;
        if(record.getYieldRate().signum()>0 && latestMeasurement) {
            ingredient.setYieldRate(record.getYieldRate());ingredientMasterRepository.save(ingredient);
            for(String dishId:dishRecipeRepository.findByIngredientIdAndStoreId(ingredient.getIngredientId(),ingredient.getStoreId()).stream().map(DishRecipe::getDishId).distinct().sorted().toList())
                calculateAndSaveCostCard(dishId,ingredient.getStoreId());
        }
        return saved;
    }

    @Transactional
    public CostCard calculateAndSaveCostCard(String dishId, Long storeId) {
        UserContext.assertStoreAccess(storeId);
        DishMaster dish = dishMasterRepository.findById(
                new DishMaster.DishMasterId(dishId, storeId))
                .orElseThrow(() -> new RuntimeException("菜品不存在"));

        List<DishRecipe> recipes = dishRecipeRepository.findByDishIdAndStoreId(dishId, storeId);
        if(recipes.isEmpty())throw new IllegalArgumentException("菜品尚未配置原料配方，不能生成零成本卡");

        BigDecimal materialCost = BigDecimal.ZERO;
        for (DishRecipe recipe : recipes) {
            IngredientMaster ingredient = ingredientMasterRepository.findById(
                    new IngredientMaster.IngredientMasterId(recipe.getIngredientId(), storeId))
                    .orElse(null);
            materialCost=materialCost.add(DishCostCalculator.calculateLine(recipe,ingredient));
            dishRecipeRepository.save(recipe);
        }
        materialCost=materialCost.setScale(2,RoundingMode.HALF_UP);
        BigDecimal totalCost = materialCost;

        BigDecimal sellingPrice = dish.getSalePrice();
        BigDecimal costRate=DishCostCalculator.costRate(totalCost,sellingPrice);
        BigDecimal grossMargin=costRate==null?null:new BigDecimal("100").subtract(costRate);
        dish.setCostPrice(totalCost);dish.setCostRate(costRate);dishMasterRepository.save(dish);

        CostCard costCard = costCardRepository.findByDishIdAndStoreId(dishId, storeId)
                .orElse(new CostCard());
        costCard.setStoreId(storeId);
        costCard.setDishId(dishId);
        costCard.setDishName(dish.getDishName());
        costCard.setStandardCost(materialCost);
        // Recipe-based standard material cost is not a measured actual dish-consumption cost.
        costCard.setSellingPrice(sellingPrice);
        costCard.setGrossMargin(grossMargin);
        costCard.setStatus("active");
        costCard.setEffectiveDate(LocalDateTime.now());

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
        UserContext.assertStoreAccess(storeId);
        if (status != null && !status.isEmpty()) {
            return goodsReceiptRepository.findByStoreIdAndStatus(storeId, status);
        }
        return goodsReceiptRepository.findByStoreId(storeId);
    }

    public List<MaterialRequisition> getRequisitions(Long storeId, String status) {
        UserContext.assertStoreAccess(storeId);
        if (status != null && !status.isEmpty()) {
            return materialRequisitionRepository.findByStoreIdAndStatus(storeId, status);
        }
        return materialRequisitionRepository.findByStoreId(storeId);
    }

    public List<PreprocessingRecord> getPreprocessingRecords(Long storeId, String ingredientId) {
        UserContext.assertStoreAccess(storeId);
        if (ingredientId != null && !ingredientId.isEmpty()) {
            return preprocessingRecordRepository.findByStoreId(storeId).stream().filter(r->ingredientId.equals(r.getIngredientId())).toList();
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
