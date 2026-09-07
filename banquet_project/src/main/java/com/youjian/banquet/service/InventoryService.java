/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.InventoryDTO
 *  com.youjian.banquet.entity.IngredientInventoryLog
 *  com.youjian.banquet.entity.IngredientMaster
 *  com.youjian.banquet.repository.IngredientInventoryLogRepository
 *  com.youjian.banquet.repository.IngredientMasterRepository
 *  com.youjian.banquet.service.InventoryService
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.youjian.banquet.service;

import com.youjian.banquet.dto.InventoryDTO;
import com.youjian.banquet.entity.IngredientInventoryLog;
import com.youjian.banquet.entity.IngredientMaster;
import com.youjian.banquet.repository.IngredientInventoryLogRepository;
import com.youjian.banquet.repository.IngredientMasterRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {
    @Autowired
    private IngredientInventoryLogRepository inventoryLogRepository;
    @Autowired
    private IngredientMasterRepository ingredientMasterRepository;

    public List<InventoryDTO> getInventoryLogs(String storeId) {
        return this.inventoryLogRepository.findByStoreId(Long.valueOf(Long.parseLong(storeId))).stream().map(arg_0 -> this.toDTO(arg_0)).collect(Collectors.toList());
    }

    public List<InventoryDTO> getInventoryLogsByIngredient(String storeId, String ingredientId) {
        return this.inventoryLogRepository.findByStoreIdAndIngredientId(Long.valueOf(Long.parseLong(storeId)), ingredientId).stream().map(arg_0 -> this.toDTO(arg_0)).collect(Collectors.toList());
    }

    public List<InventoryDTO> getInventoryLogsByDateRange(String storeId, LocalDateTime start, LocalDateTime end) {
        return this.inventoryLogRepository.findByStoreIdAndCreatedAtBetween(Long.valueOf(Long.parseLong(storeId)), start, end).stream().map(arg_0 -> this.toDTO(arg_0)).collect(Collectors.toList());
    }

    @Transactional
    public InventoryDTO stockIn(InventoryDTO dto) {
        return stockInWithPrice(dto,null,null);
    }

    @Transactional
    public InventoryDTO stockInReceipt(InventoryDTO dto,BigDecimal purchasePrice,LocalDate entryDate) {
        if(purchasePrice==null || purchasePrice.signum()<0 || purchasePrice.stripTrailingZeros().scale()>8 || purchasePrice.compareTo(new BigDecimal("9999999.99999999"))>0 || entryDate==null || entryDate.isAfter(LocalDate.now()))
            throw new IllegalArgumentException("采购单价或入库日期无效");
        return stockInWithPrice(dto,purchasePrice,entryDate);
    }

    private InventoryDTO stockInWithPrice(InventoryDTO dto,BigDecimal purchasePrice,LocalDate entryDate) {
        validateQuantity(dto);
        IngredientMaster ingredient = this.ingredientMasterRepository.findForStockUpdate(dto.getIngredientId(), Long.parseLong(dto.getStoreId()))
                .orElseThrow(() -> new IllegalArgumentException("当前门店没有该食材，请核对食材和门店"));
        BigDecimal beforeStock = ingredient.getCurrentStock() == null ? BigDecimal.ZERO : ingredient.getCurrentStock();
        BigDecimal afterStock = beforeStock.add(dto.getQuantity());
        ingredient.setCurrentStock(afterStock);
        // Latest effective receipt price is the user's chosen costing basis. Preserve historical receipts.
        if(purchasePrice!=null && (ingredient.getLastEntryDate()==null || !entryDate.isBefore(ingredient.getLastEntryDate()))) {
            ingredient.setUnitPrice(purchasePrice);
            ingredient.setLastEntryDate(entryDate);
        }
        this.ingredientMasterRepository.save(ingredient);
        IngredientInventoryLog log = new IngredientInventoryLog();
        log.setStoreId(Long.valueOf(Long.parseLong(dto.getStoreId())));
        log.setIngredientId(dto.getIngredientId());
        log.setChangeType("IN");
        log.setChangeDirection("入库");
        log.setQuantity(dto.getQuantity());
        log.setBeforeStock(beforeStock);
        log.setAfterStock(afterStock);
        if(purchasePrice!=null) {
            log.setUnitPrice(purchasePrice);
            log.setTotalAmount(purchasePrice.multiply(dto.getQuantity()).setScale(2,RoundingMode.HALF_UP));
        }
        log.setReferenceId(dto.getReferenceId());
        log.setReferenceType(dto.getReferenceType());
        log.setNotes(appendOperatorToNotes(dto.getNotes(), dto.getOperator()));
        this.inventoryLogRepository.save(log);
        return this.toDTO(log);
    }

    @Transactional
    public InventoryDTO stockOut(InventoryDTO dto) {
        validateQuantity(dto);
        IngredientMaster ingredient = this.ingredientMasterRepository.findForStockUpdate(dto.getIngredientId(), Long.parseLong(dto.getStoreId()))
                .orElseThrow(() -> new IllegalArgumentException("当前门店没有该食材，请核对食材和门店"));
        BigDecimal beforeStock = ingredient.getCurrentStock() == null ? BigDecimal.ZERO : ingredient.getCurrentStock();
        if (beforeStock.compareTo(dto.getQuantity()) < 0) {
            throw new IllegalArgumentException("库存不足：当前可用 " + beforeStock.toPlainString() + "，请核对出库数量");
        }
        BigDecimal afterStock = beforeStock.subtract(dto.getQuantity());
        ingredient.setCurrentStock(afterStock);
        this.ingredientMasterRepository.save(ingredient);
        IngredientInventoryLog log = new IngredientInventoryLog();
        log.setStoreId(Long.valueOf(Long.parseLong(dto.getStoreId())));
        log.setIngredientId(dto.getIngredientId());
        log.setChangeType("OUT");
        log.setChangeDirection("出库");
        if(ingredient.getUnitPrice()!=null) {
            log.setUnitPrice(ingredient.getUnitPrice());
            log.setTotalAmount(ingredient.getUnitPrice().multiply(dto.getQuantity()).setScale(2,RoundingMode.HALF_UP));
        }
        log.setQuantity(dto.getQuantity());
        log.setBeforeStock(beforeStock);
        log.setAfterStock(afterStock);
        log.setReferenceId(dto.getReferenceId());
        log.setReferenceType(dto.getReferenceType());
        log.setNotes(appendOperatorToNotes(dto.getNotes(), dto.getOperator()));
        this.inventoryLogRepository.save(log);
        return this.toDTO(log);
    }

    public List<InventoryDTO> getLowStockAlerts(String storeId) {
        return this.ingredientMasterRepository.findLowStockIngredients(Long.valueOf(Long.parseLong(storeId))).stream().map(i -> {
            InventoryDTO dto = new InventoryDTO();
            dto.setIngredientId(i.getIngredientId());
            dto.setStoreId(String.valueOf(i.getStoreId()));
            dto.setIngredientName(i.getIngredientName());
            dto.setAfterStock(i.getCurrentStock());
            dto.setNotes("Low stock alert: current=" + String.valueOf(i.getCurrentStock()) + ", min=" + String.valueOf(i.getMinStock()));
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 跨门店原料调拨：从源门店库存扣减，调入目标门店库存增加，两笔日志同时记录。
     * <p>
     * 调用方必须确保已做权限校验（仅总经理可执行）。本方法在一个事务内完成
     * stockOut + stockIn，任一步失败整体回滚。
     *
     * @param ingredientId 原料ID
     * @param fromStoreId  源门店ID
     * @param toStoreId    目标门店ID
     * @param quantity     调拨数量（必须 > 0）
     * @param operator     操作人
     * @param notes        备注
     * @return 目标门店入库后的库存日志 DTO
     */
    @Transactional
    public InventoryDTO transfer(String ingredientId, Long fromStoreId, Long toStoreId,
                                 BigDecimal quantity, String operator, String notes) {
        if (fromStoreId == null || toStoreId == null) {
            throw new IllegalArgumentException("调拨门店ID不能为空");
        }
        if (fromStoreId.equals(toStoreId)) {
            throw new IllegalArgumentException("源门店与目标门店不能相同");
        }
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("调拨数量必须大于0");
        }
        String transferRef = "TRF-" + System.currentTimeMillis();

        // 相向调拨按同一门店顺序锁定两端，避免死锁和丢失更新。
        for (Long store : fromStoreId < toStoreId ? List.of(fromStoreId, toStoreId) : List.of(toStoreId, fromStoreId)) {
            ingredientMasterRepository.findForStockUpdate(ingredientId, store)
                .orElseThrow(() -> new IllegalArgumentException("调拨两端门店都必须先建立该食材档案"));
        }

        // 1. 源门店出库
        InventoryDTO outDto = new InventoryDTO();
        outDto.setIngredientId(ingredientId);
        outDto.setStoreId(String.valueOf(fromStoreId));
        outDto.setQuantity(quantity);
        outDto.setReferenceId(transferRef);
        outDto.setReferenceType("TRANSFER_OUT");
        outDto.setOperator(operator);
        outDto.setNotes(notes != null ? notes : "调拨出库至门店 " + toStoreId);
        this.stockOut(outDto);

        // 2. 目标门店入库
        InventoryDTO inDto = new InventoryDTO();
        inDto.setIngredientId(ingredientId);
        inDto.setStoreId(String.valueOf(toStoreId));
        inDto.setQuantity(quantity);
        inDto.setReferenceId(transferRef);
        inDto.setReferenceType("TRANSFER_IN");
        inDto.setOperator(operator);
        inDto.setNotes(notes != null ? notes : "门店 " + fromStoreId + " 调拨入库");
        return this.stockIn(inDto);
    }

    /**
     * ingredient_inventory_log.operator_id 是员工ID外键（int），但当前调用方只传操作人姓名字符串，
     * 没有姓名->员工ID的查找逻辑，暂不能直接写入 operator_id。为了不丢失这个信息，先并入 remark。
     */
    private String appendOperatorToNotes(String notes, String operator) {
        if (operator == null || operator.isBlank()) {
            return notes;
        }
        String opNote = "操作人:" + operator;
        return (notes == null || notes.isBlank()) ? opNote : notes + " | " + opNote;
    }

    private static void validateQuantity(InventoryDTO dto) {
        if (dto == null || dto.getIngredientId() == null || dto.getIngredientId().isBlank()
                || dto.getStoreId() == null || dto.getStoreId().isBlank()) {
            throw new IllegalArgumentException("请选择门店和食材后提交");
        }
        if (dto.getQuantity() == null || dto.getQuantity().signum() <= 0
                || dto.getQuantity().stripTrailingZeros().scale() > 3) {
            throw new IllegalArgumentException("入出库数量必须大于零，最多三位小数");
        }
    }

    private InventoryDTO toDTO(IngredientInventoryLog e) {
        InventoryDTO dto = new InventoryDTO();
        dto.setIngredientId(e.getIngredientId());
        dto.setStoreId(String.valueOf(e.getStoreId()));
        dto.setChangeType(e.getChangeType());
        dto.setQuantity(e.getQuantity());
        dto.setBeforeStock(e.getBeforeStock());
        dto.setAfterStock(e.getAfterStock());
        dto.setReferenceId(e.getReferenceId());
        dto.setReferenceType(e.getReferenceType());
        dto.setNotes(e.getNotes());
        this.ingredientMasterRepository.findByIngredientIdAndStoreId(e.getIngredientId(), e.getStoreId()).ifPresent(i -> dto.setIngredientName(i.getIngredientName()));
        return dto;
    }
}

