package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_loss_detail")
public class StockLossDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @Column(name = "loss_id", nullable = false, length = 50)
    private String lossId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "food_material_id", nullable = false, length = 50)
    private String ingredientId;

    @Column(name = "batch_no", length = 50)
    private String batchNo;

    @Column(name = "purchase_id", length = 50)
    private String purchaseId;

    @Column(name = "loss_quantity", precision = 12, scale = 3, nullable = false)
    private BigDecimal lossQuantity;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "unit_price", precision = 12, scale = 6)
    private BigDecimal unitPrice;

    @Column(name = "loss_amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "before_quantity", precision = 12, scale = 3)
    private BigDecimal beforeQuantity;

    @Column(name = "after_quantity", precision = 12, scale = 3)
    private BigDecimal afterQuantity;

    @Column(name = "loss_description", length = 200)
    private String lossReason;

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "is_deleted")
    private Boolean isDeleted;

    /** 生产库该列是 bigint（毫秒时间戳） */
    @Column(name = "created_at")
    private Long createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = System.currentTimeMillis();
        if (this.isDeleted == null) this.isDeleted = false;
    }
}
