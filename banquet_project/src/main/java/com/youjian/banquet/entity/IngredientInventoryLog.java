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

    /** 表定义 NOT NULL 无默认值(入库/出库)，之前这个实体完全没映射这一列，
     *  导致所有真实入库/出库(stockIn/stockOut)从未真正成功写过库存台账——
     *  这是"全流程测试"第一批就撞上的真bug，不是理论风险。 */
    @Column(name = "change_direction")
    private String changeDirection;

    @Column(name = "before_quantity", precision = 10, scale = 3)
    private BigDecimal beforeStock;

    @Column(name = "after_quantity", precision = 10, scale = 3)
    private BigDecimal afterStock;

    @Column(name = "unit_price", precision = 12, scale = 6)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "source_id")
    private String referenceId;

    @Column(name = "source_type")
    private String referenceType;

    @Column(name = "operator_id")
    private Integer operatorId;

    /** 表定义同样 NOT NULL 无默认值，同上一并补上。 */
    @Column(name = "operate_time")
    private LocalDateTime operateTime;

    @Column(name = "remark")
    private String notes;

    @Column(name = "is_deleted")
    private Integer isDeleted;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.operateTime == null) {
            this.operateTime = LocalDateTime.now();
        }
        if (this.isDeleted == null) {
            this.isDeleted = 0;
        }
    }
}
