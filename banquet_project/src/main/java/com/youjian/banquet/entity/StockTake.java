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
@Table(name = "stock_take")
public class StockTake {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "take_id")
    private Long takeId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "take_no", nullable = false, unique = true, length = 50)
    private String takeNo;

    @Column(name = "take_date", nullable = false)
    private LocalDate takeDate;

    @Column(name = "take_type", nullable = false, length = 20)
    private String takeType;

    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "warehouse_id")
    private Integer warehouseId;

    @Column(name = "total_items")
    private Integer totalItems;

    @Column(name = "total_diff_items")
    private Integer totalDiffItems;

    @Column(name = "total_diff_amount", precision = 12, scale = 2)
    private BigDecimal totalDiffAmount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "operator_id")
    private Integer operatorId;

    @Column(name = "operator_name", length = 50)
    private String operatorName;

    @Column(name = "supervisor_id")
    private Integer supervisorId;

    @Column(name = "supervisor_name", length = 50)
    private String supervisorName;

    @Column(name = "finish_time")
    private LocalDateTime finishTime;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (status == null) status = "draft";
        if (takeType == null) takeType = "full";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
