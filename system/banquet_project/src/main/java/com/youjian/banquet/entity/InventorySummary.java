package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_summary",
       uniqueConstraints = @UniqueConstraint(columnNames = {"store_id", "ingredient_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventorySummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "summary_id")
    private Long summaryId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "ingredient_id", nullable = false, length = 50)
    private String ingredientId;

    @Column(name = "total_quantity", nullable = false)
    private BigDecimal totalQuantity = BigDecimal.ZERO;

    @Column(name = "total_cost", nullable = false)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(name = "avg_unit_price", nullable = false)
    private BigDecimal avgUnitPrice = BigDecimal.ZERO;

    @Column(name = "last_in_time")
    private LocalDateTime lastInTime;

    @Column(name = "last_out_time")
    private LocalDateTime lastOutTime;

    @Column(name = "updated_at")
    private LocalDateTime updateTime;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
