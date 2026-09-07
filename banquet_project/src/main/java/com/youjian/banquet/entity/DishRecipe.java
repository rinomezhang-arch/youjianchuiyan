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
@Table(name = "dish_recipe")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DishRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "dish_id")
    private String dishId;

    @Column(name = "ingredient_id")
    private String ingredientId;

    @Column(name = "ingredient_name")
    private String ingredientName;

    @Column(name = "unit")
    private String unit;

    @Column(name = "unit_price", precision = 15, scale = 8)
    private BigDecimal unitPrice;

    @Column(name = "quantity", precision = 10, scale = 3)
    private BigDecimal quantity;

    @Column(name = "total_cost", precision = 15, scale = 4)
    private BigDecimal totalCost;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "wastage_rate", precision = 5, scale = 2)
    private BigDecimal wastageRate;

    @Column(name = "yield_rate", precision = 5, scale = 2)
    private BigDecimal yieldRate;

    @Column(name = "last_entry_date")
    private LocalDate lastEntryDate;

    @Column(name = "net_unit_price", precision = 15, scale = 8)
    private BigDecimal netUnitPrice;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 所属配方版本；NULL 表示版本化之前的导入基线。
     * 历史行不删除，靠 {@link #isActive} 区分当前生效版本与历史版本。
     */
    @Column(name = "revision_id")
    private Long revisionId;

    /** 1=当前生效 0=历史版本。默认 1，保证迁移后既有数据的查询结果完全不变。 */
    @Column(name = "is_active", columnDefinition = "TINYINT")
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.TINYINT)
    private Integer isActive = 1;

    public Long getRevisionId() { return revisionId; }
    public void setRevisionId(Long revisionId) { this.revisionId = revisionId; }

    public Integer getIsActive() { return isActive; }
    public void setIsActive(Integer isActive) { this.isActive = isActive; }

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
