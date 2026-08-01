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
@Table(name = "cost_card")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cost_card_id")
    private Long costCardId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "dish_id", length = 20)
    private String dishId;

    @Column(name = "dish_name")
    private String dishName;

    @Column(name = "total_cost", precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "material_cost", precision = 12, scale = 2)
    private BigDecimal materialCost;

    @Column(name = "labor_cost", precision = 12, scale = 2)
    private BigDecimal laborCost;

    @Column(name = "overhead_cost", precision = 12, scale = 2)
    private BigDecimal overheadCost;

    @Column(name = "cost_rate", precision = 5, scale = 2)
    private BigDecimal costRate;

    @Column(name = "sell_price", precision = 12, scale = 2)
    private BigDecimal sellPrice;

    @Column(name = "calculated_price", precision = 12, scale = 2)
    private BigDecimal calculatedPrice;

    @Column(name = "status")
    private String status;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

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