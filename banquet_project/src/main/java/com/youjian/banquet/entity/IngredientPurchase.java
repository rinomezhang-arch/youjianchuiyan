package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingredient_purchase")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "purchase_id")
    private Long purchaseId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "ingredient_id")
    private String ingredientId;

    @Column(name = "supplier_id")
    private Integer supplierId;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    // 新字段（标准字段）
    @Column(name = "purchase_quantity", precision = 12, scale = 3)
    private BigDecimal purchaseQuantity;

    @Column(name = "purchase_price", precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "purchase_total", precision = 12, scale = 2)
    private BigDecimal purchaseTotal;

    @Column(name = "usage_quantity", precision = 12, scale = 3)
    private BigDecimal usageQuantity;

    @Column(name = "usage_price", precision = 10, scale = 4)
    private BigDecimal usagePrice;

    @Column(name = "processing_note", columnDefinition = "TEXT")
    private String processingNote;

    @Column(name = "operator_id")
    private Integer operatorId;

    @Column(name = "status")
    private String status;

    // 旧字段（灰度保留，兼容期使用）
    @Column(name = "quantity", precision = 10, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "create_time")
    private LocalDateTime createdAt;

    @Column(name = "update_time")
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
}
