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
@Table(name = "tool_inventory")
public class ToolInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long inventoryId;

    @Column(name = "inventory_no", nullable = false, unique = true, length = 32)
    private String inventoryNo;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "inventory_date", nullable = false)
    private LocalDate inventoryDate;

    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "total_qty", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalQty;

    @Column(name = "actual_qty", precision = 12, scale = 2, nullable = false)
    private BigDecimal actualQty;

    @Column(name = "diff_qty", precision = 12, scale = 2, nullable = false)
    private BigDecimal diffQty;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

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
        if (status == null) status = "待审核";
        if (totalQty == null) totalQty = BigDecimal.ZERO;
        if (actualQty == null) actualQty = BigDecimal.ZERO;
        if (diffQty == null) diffQty = BigDecimal.ZERO;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
