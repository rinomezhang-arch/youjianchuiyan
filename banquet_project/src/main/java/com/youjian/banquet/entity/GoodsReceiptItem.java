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
@Table(name = "purchase_receipt_detail")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoodsReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "detail_id")
    private Long detailId;

    @Column(name = "receipt_id", nullable = false)
    private Long receiptId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "order_detail_id")
    private Long orderDetailId;

    @Column(name = "ingredient_id")
    private String ingredientId;

    @Column(name = "ingredient_name", nullable = false, length = 100)
    private String ingredientName;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "spec", length = 100)
    private String spec;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "order_quantity", precision = 10, scale = 2)
    private BigDecimal orderQuantity;

    @Column(name = "actual_quantity", nullable = false, precision = 10, scale = 2)
    private BigDecimal actualQuantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "quality_status", length = 20)
    private String qualityStatus;

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
