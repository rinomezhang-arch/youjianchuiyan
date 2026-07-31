/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.IngredientDTO
 */
package com.youjian.banquet.dto;

import java.math.BigDecimal;

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

    public String getIngredientId() {
        return this.ingredientId;
    }

    public String getStoreId() {
        return this.storeId;
    }

    public String getIngredientName() {
        return this.ingredientName;
    }

    public String getCategory() {
        return this.category;
    }

    public String getUnit() {
        return this.unit;
    }

    public BigDecimal getCurrentStock() {
        return this.currentStock;
    }

    public BigDecimal getMinStock() {
        return this.minStock;
    }

    public BigDecimal getUnitPrice() {
        return this.unitPrice;
    }

    public String getSupplierId() {
        return this.supplierId;
    }

    public String getSupplierName() {
        return this.supplierName;
    }

    public String getStatus() {
        return this.status;
    }

    public void setIngredientId(String ingredientId) {
        this.ingredientId = ingredientId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock;
    }

    public void setMinStock(BigDecimal minStock) {
        this.minStock = minStock;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof IngredientDTO)) {
            return false;
        }
        IngredientDTO other = (IngredientDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        String this$ingredientId = this.getIngredientId();
        String other$ingredientId = other.getIngredientId();
        if (this$ingredientId == null ? other$ingredientId != null : !this$ingredientId.equals(other$ingredientId)) {
            return false;
        }
        String this$storeId = this.getStoreId();
        String other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !this$storeId.equals(other$storeId)) {
            return false;
        }
        String this$ingredientName = this.getIngredientName();
        String other$ingredientName = other.getIngredientName();
        if (this$ingredientName == null ? other$ingredientName != null : !this$ingredientName.equals(other$ingredientName)) {
            return false;
        }
        String this$category = this.getCategory();
        String other$category = other.getCategory();
        if (this$category == null ? other$category != null : !this$category.equals(other$category)) {
            return false;
        }
        String this$unit = this.getUnit();
        String other$unit = other.getUnit();
        if (this$unit == null ? other$unit != null : !this$unit.equals(other$unit)) {
            return false;
        }
        BigDecimal this$currentStock = this.getCurrentStock();
        BigDecimal other$currentStock = other.getCurrentStock();
        if (this$currentStock == null ? other$currentStock != null : !((Object)this$currentStock).equals(other$currentStock)) {
            return false;
        }
        BigDecimal this$minStock = this.getMinStock();
        BigDecimal other$minStock = other.getMinStock();
        if (this$minStock == null ? other$minStock != null : !((Object)this$minStock).equals(other$minStock)) {
            return false;
        }
        BigDecimal this$unitPrice = this.getUnitPrice();
        BigDecimal other$unitPrice = other.getUnitPrice();
        if (this$unitPrice == null ? other$unitPrice != null : !((Object)this$unitPrice).equals(other$unitPrice)) {
            return false;
        }
        String this$supplierId = this.getSupplierId();
        String other$supplierId = other.getSupplierId();
        if (this$supplierId == null ? other$supplierId != null : !this$supplierId.equals(other$supplierId)) {
            return false;
        }
        String this$supplierName = this.getSupplierName();
        String other$supplierName = other.getSupplierName();
        if (this$supplierName == null ? other$supplierName != null : !this$supplierName.equals(other$supplierName)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        return !(this$status == null ? other$status != null : !this$status.equals(other$status));
    }

    protected boolean canEqual(Object other) {
        return other instanceof IngredientDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        String $ingredientId = this.getIngredientId();
        result = result * 59 + ($ingredientId == null ? 43 : $ingredientId.hashCode());
        String $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : $storeId.hashCode());
        String $ingredientName = this.getIngredientName();
        result = result * 59 + ($ingredientName == null ? 43 : $ingredientName.hashCode());
        String $category = this.getCategory();
        result = result * 59 + ($category == null ? 43 : $category.hashCode());
        String $unit = this.getUnit();
        result = result * 59 + ($unit == null ? 43 : $unit.hashCode());
        BigDecimal $currentStock = this.getCurrentStock();
        result = result * 59 + ($currentStock == null ? 43 : ((Object)$currentStock).hashCode());
        BigDecimal $minStock = this.getMinStock();
        result = result * 59 + ($minStock == null ? 43 : ((Object)$minStock).hashCode());
        BigDecimal $unitPrice = this.getUnitPrice();
        result = result * 59 + ($unitPrice == null ? 43 : ((Object)$unitPrice).hashCode());
        String $supplierId = this.getSupplierId();
        result = result * 59 + ($supplierId == null ? 43 : $supplierId.hashCode());
        String $supplierName = this.getSupplierName();
        result = result * 59 + ($supplierName == null ? 43 : $supplierName.hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        return result;
    }

    public String toString() {
        return "IngredientDTO(ingredientId=" + this.getIngredientId() + ", storeId=" + this.getStoreId() + ", ingredientName=" + this.getIngredientName() + ", category=" + this.getCategory() + ", unit=" + this.getUnit() + ", currentStock=" + String.valueOf(this.getCurrentStock()) + ", minStock=" + String.valueOf(this.getMinStock()) + ", unitPrice=" + String.valueOf(this.getUnitPrice()) + ", supplierId=" + this.getSupplierId() + ", supplierName=" + this.getSupplierName() + ", status=" + this.getStatus() + ")";
    }

    public IngredientDTO() {
    }

    public IngredientDTO(String ingredientId, String storeId, String ingredientName, String category, String unit, BigDecimal currentStock, BigDecimal minStock, BigDecimal unitPrice, String supplierId, String supplierName, String status) {
        this.ingredientId = ingredientId;
        this.storeId = storeId;
        this.ingredientName = ingredientName;
        this.category = category;
        this.unit = unit;
        this.currentStock = currentStock;
        this.minStock = minStock;
        this.unitPrice = unitPrice;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.status = status;
    }
}

