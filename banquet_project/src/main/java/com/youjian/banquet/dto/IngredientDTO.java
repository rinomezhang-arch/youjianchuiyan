package com.youjian.banquet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientDTO {
    private String ingredientId;
    private String storeId;
    private String ingredientName;
    private String category;
    private String unit;
    private BigDecimal currentStock;
    private BigDecimal minStock;
    private BigDecimal unitPrice;
    private String supplierId;
    private String supplierName;
    private String status;

    // 规格（如"500g*20包"）、采购单位、使用(成本计算)单位、
    // 采购单位->使用单位换算率、出成率(初加工损耗后的可用比例)。
    // 这几个字段在 ingredient_master 表里早就有并且早就填了真实数据(1209/1210条)，
    // 之前这个 DTO 一直没映射，导致前端(采购申请页的原料下拉、成本卡页等)
    // 永远拿不到这些字段——不是没有数据，是接口没传。
    private String spec;
    private String purchaseUnit;
    private String usageUnit;
    private BigDecimal conversionRate;
    private BigDecimal yieldRate;
}
