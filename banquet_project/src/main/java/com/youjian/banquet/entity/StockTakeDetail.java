package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_take_detail")
public class StockTakeDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @Column(name = "take_id", nullable = false)
    private Long takeId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "ingredient_id", length = 50)
    private String ingredientId;

    @Column(name = "ingredient_name", nullable = false, length = 100)
    private String ingredientName;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "system_quantity", precision = 10, scale = 2, nullable = false)
    private BigDecimal systemQuantity;

    @Column(name = "system_amount", precision = 12, scale = 2)
    private BigDecimal systemAmount;

    @Column(name = "actual_quantity", precision = 10, scale = 2, nullable = false)
    private BigDecimal actualQuantity;

    @Column(name = "actual_amount", precision = 12, scale = 2)
    private BigDecimal actualAmount;

    @Column(name = "diff_quantity", precision = 10, scale = 2)
    private BigDecimal diffQuantity;

    @Column(name = "diff_amount", precision = 12, scale = 2)
    private BigDecimal diffAmount;

    @Column(name = "diff_type", length = 20)
    private String diffType;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
