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
import java.time.LocalDateTime;

@Entity
@Table(name = "dish_cost_card")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cost_card_id")
    private Long costCardId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "dish_id", nullable = false, length = 50)
    private String dishId;

    @Column(name = "dish_name", nullable = false, length = 100)
    private String dishName;

    @Column(name = "dish_category", length = 50)
    private String dishCategory;

    @Column(name = "standard_yield", precision = 10, scale = 3)
    private BigDecimal standardYield;

    @Column(name = "actual_yield", precision = 10, scale = 3)
    private BigDecimal actualYield;

    @Column(name = "yield_rate", precision = 5, scale = 2)
    private BigDecimal yieldRate;

    @Column(name = "standard_cost", precision = 12, scale = 2)
    private BigDecimal standardCost;

    @Column(name = "actual_cost", precision = 12, scale = 2)
    private BigDecimal actualCost;

    @Column(name = "selling_price", precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "gross_margin", precision = 5, scale = 2)
    private BigDecimal grossMargin;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate;

    @Column(name = "created_by", length = 50)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
