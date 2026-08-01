/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.IngredientMaster
 *  com.youjian.banquet.entity.IngredientMaster$IngredientMasterId
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.Id
 *  jakarta.persistence.IdClass
 *  jakarta.persistence.PrePersist
 *  jakarta.persistence.PreUpdate
 *  jakarta.persistence.Table
 */
package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="ingredient_master")
@IdClass(IngredientMaster.IngredientMasterId.class)
public class IngredientMaster {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngredientMasterId implements Serializable {
        private String ingredientId;
        private Long storeId;
    }

    @Id
    @Column(name="ingredient_id")
    private String ingredientId;
    @Id
    @Column(name="store_id")
    private Long storeId;
    @Column(name="ingredient_name")
    private String ingredientName;
    @Column(name="category")
    private String category;
    @Column(name="unit")
    private String unit;
    @Column(name="current_stock", precision=10, scale=3)
    private BigDecimal currentStock;
    @Column(name="min_stock", precision=10, scale=3)
    private BigDecimal minStock;
    @Column(name="unit_price", precision=10, scale=2)
    private BigDecimal unitPrice;
    @Column(name="supplier_id")
    private Long supplierId;
    @Column(name="status")
    private String status;
    @Column(name="created_at")
    private LocalDateTime createdAt;
    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public String getIngredientId() {
        return this.ingredientId;
    }

    public Long getStoreId() {
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

    public Long getSupplierId() {
        return this.supplierId;
    }

    public String getStatus() {
        return this.status;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setIngredientId(String ingredientId) {
        this.ingredientId = ingredientId;
    }

    public void setStoreId(Long storeId) {
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

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof IngredientMaster)) {
            return false;
        }
        IngredientMaster other = (IngredientMaster)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !((Object)this$storeId).equals(other$storeId)) {
            return false;
        }
        Long this$supplierId = this.getSupplierId();
        Long other$supplierId = other.getSupplierId();
        if (this$supplierId == null ? other$supplierId != null : !((Object)this$supplierId).equals(other$supplierId)) {
            return false;
        }
        String this$ingredientId = this.getIngredientId();
        String other$ingredientId = other.getIngredientId();
        if (this$ingredientId == null ? other$ingredientId != null : !this$ingredientId.equals(other$ingredientId)) {
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
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt)) {
            return false;
        }
        LocalDateTime this$updatedAt = this.getUpdatedAt();
        LocalDateTime other$updatedAt = other.getUpdatedAt();
        return !(this$updatedAt == null ? other$updatedAt != null : !((Object)this$updatedAt).equals(other$updatedAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof IngredientMaster;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        Long $supplierId = this.getSupplierId();
        result = result * 59 + ($supplierId == null ? 43 : ((Object)$supplierId).hashCode());
        String $ingredientId = this.getIngredientId();
        result = result * 59 + ($ingredientId == null ? 43 : $ingredientId.hashCode());
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
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "IngredientMaster(ingredientId=" + this.getIngredientId() + ", storeId=" + this.getStoreId() + ", ingredientName=" + this.getIngredientName() + ", category=" + this.getCategory() + ", unit=" + this.getUnit() + ", currentStock=" + String.valueOf(this.getCurrentStock()) + ", minStock=" + String.valueOf(this.getMinStock()) + ", unitPrice=" + String.valueOf(this.getUnitPrice()) + ", supplierId=" + this.getSupplierId() + ", status=" + this.getStatus() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", updatedAt=" + String.valueOf(this.getUpdatedAt()) + ")";
    }

    public IngredientMaster() {
    }

    public IngredientMaster(String ingredientId, Long storeId, String ingredientName, String category, String unit, BigDecimal currentStock, BigDecimal minStock, BigDecimal unitPrice, Long supplierId, String status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.ingredientId = ingredientId;
        this.storeId = storeId;
        this.ingredientName = ingredientName;
        this.category = category;
        this.unit = unit;
        this.currentStock = currentStock;
        this.minStock = minStock;
        this.unitPrice = unitPrice;
        this.supplierId = supplierId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

