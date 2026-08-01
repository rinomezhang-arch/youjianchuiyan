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
@Table(name = "stock_transfer")
public class StockTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private Long transferId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "transfer_no", nullable = false, length = 50)
    private String transferNo;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Column(name = "from_warehouse_id")
    private Integer fromWarehouseId;

    @Column(name = "from_warehouse_name", length = 50)
    private String fromWarehouseName;

    @Column(name = "to_warehouse_id")
    private Integer toWarehouseId;

    @Column(name = "to_warehouse_name", length = 50)
    private String toWarehouseName;

    @Column(name = "total_quantity", precision = 10, scale = 2)
    private BigDecimal totalQuantity;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "out_time")
    private LocalDateTime outTime;

    @Column(name = "in_time")
    private LocalDateTime inTime;

    @Column(name = "operator_out_id")
    private Integer operatorOutId;

    @Column(name = "operator_out_name", length = 50)
    private String operatorOutName;

    @Column(name = "operator_in_id")
    private Integer operatorInId;

    @Column(name = "operator_in_name", length = 50)
    private String operatorInName;

    @Column(name = "transfer_reason", length = 200)
    private String transferReason;

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
        if (status == null) {
            status = "pending";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
