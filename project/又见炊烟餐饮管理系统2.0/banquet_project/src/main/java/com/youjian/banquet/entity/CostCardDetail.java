package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dish_cost_card_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CostCardDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "cost_card_id", nullable = false)
    private Long costCardId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "ingredient_id", nullable = false, length = 50)
    private String ingredientId;

    @Column(name = "ingredient_name", nullable = false, length = 100)
    private String ingredientName;

    @Column(name = "spec", length = 100)
    private String spec;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "standard_quantity", precision = 10, scale = 3)
    private BigDecimal standardQuantity;

    @Column(name = "actual_quantity", precision = 10, scale = 3)
    private BigDecimal actualQuantity;

    @Column(name = "converted_quantity", precision = 10, scale = 3)
    private BigDecimal convertedQuantity;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "cost_amount", precision = 12, scale = 2)
    private BigDecimal costAmount;

    @Column(name = "yield_rate", precision = 5, scale = 2)
    private BigDecimal yieldRate;

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
