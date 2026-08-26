package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ingredient_inventory_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IngredientInventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "food_material_id")
    private String ingredientId;

    @Column(name = "change_type")
    private String changeType;

    @Column(name = "change_quantity", precision = 10, scale = 3)
    private BigDecimal quantity;

    @Column(name = "before_quantity", precision = 10, scale = 3)
    private BigDecimal beforeStock;

    @Column(name = "after_quantity", precision = 10, scale = 3)
    private BigDecimal afterStock;

    @Column(name = "source_id")
    private String referenceId;

    @Column(name = "source_type")
    private String referenceType;

    @Column(name = "operator_id")
    private Integer operatorId;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
