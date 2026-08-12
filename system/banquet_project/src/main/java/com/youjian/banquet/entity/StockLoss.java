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
@Table(name = "stock_loss")
public class StockLoss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loss_id")
    private Long lossId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "loss_no", nullable = false, unique = true, length = 50)
    private String lossNo;

    @Column(name = "loss_date", nullable = false)
    private LocalDate lossDate;

    @Column(name = "loss_type", length = 50)
    private String lossType;

    @Column(name = "total_quantity", precision = 10, scale = 2)
    private BigDecimal totalQuantity;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "applicant_id")
    private Integer applicantId;

    @Column(name = "applicant_name", length = 50)
    private String applicantName;

    @Column(name = "approver_id")
    private Integer approverId;

    @Column(name = "approver_name", length = 50)
    private String approverName;

    @Column(name = "approve_time")
    private LocalDateTime approveTime;

    @Column(name = "approve_remark", length = 500)
    private String approveRemark;

    @Column(name = "warehouse_keeper_id")
    private Integer warehouseKeeperId;

    @Column(name = "warehouse_keeper_name", length = 50)
    private String warehouseKeeperName;

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
        if (status == null) status = "pending";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
