package com.youjian.banquet.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tool_master")
public class ToolMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tool_id")
    private Long toolId;

    @Column(name = "tool_no", nullable = false, length = 32)
    private String toolNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "tool_name", nullable = false, length = 128)
    private String toolName;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "spec", length = 128)
    private String spec;

    @Column(name = "brand", length = 64)
    private String brand;

    @Column(name = "unit", nullable = false, length = 16)
    private String unit;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_qty", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalQty;

    @Column(name = "available_qty", precision = 12, scale = 2, nullable = false)
    private BigDecimal availableQty;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "location", length = 64)
    private String location;

    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;

    @Column(name = "creator", length = 64)
    private String creator;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (status == null) status = "在用";
        if (unit == null) unit = "个";
        if (totalQty == null) totalQty = BigDecimal.ZERO;
        if (availableQty == null) availableQty = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
