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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingredient_master")
@IdClass(IngredientMaster.IngredientMasterId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientMaster {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngredientMasterId implements Serializable {
        private String ingredientId;
        private Long storeId;
    }

    @Id
    @Column(name = "ingredient_id", length = 50)
    private String ingredientId;

    @Id
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "ingredient_name", length = 100)
    private String ingredientName;

    @Column(name = "ingredient_category", length = 50)
    private String ingredientCategory;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "spec", length = 100)
    private String spec;

    @Column(name = "purchase_unit", length = 20)
    private String purchaseUnit;

    @Column(name = "usage_unit", length = 20)
    private String usageUnit;

    @Column(name = "conversion_rate", precision = 10, scale = 3)
    private BigDecimal conversionRate;

    @Column(name = "primary_supplier_id")
    private Integer primarySupplierId;

    @Column(name = "current_stock", precision = 12, scale = 3)
    private BigDecimal currentStock;

    @Column(name = "warning_threshold", precision = 10, scale = 3)
    private BigDecimal warningThreshold;

    @Column(name = "avg_price", precision = 10, scale = 4)
    private BigDecimal avgPrice;

    @Column(name = "yield_rate", precision = 5, scale = 2)
    private BigDecimal yieldRate;

    @Column(name = "last_entry_date")
    private LocalDate lastEntryDate;

    @Column(name = "is_active")
    private Integer isActive;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "category", length = 255)
    private String category;

    @Column(name = "unit", length = 255)
    private String unit;

    @Column(name = "min_stock", precision = 10, scale = 3)
    private BigDecimal minStock;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "supplier_id")
    private Integer supplierId;

    @Column(name = "status", length = 255)
    private String status;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
