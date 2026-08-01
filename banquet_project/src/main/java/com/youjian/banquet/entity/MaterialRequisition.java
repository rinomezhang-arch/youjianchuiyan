package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "requisition_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaterialRequisition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "requisition_id")
    private Long requisitionId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "requisition_no", nullable = false, length = 50)
    private String requisitionNo;

    @Column(name = "department_id")
    private Integer departmentId;

    @Column(name = "department_name", length = 50)
    private String departmentName;

    @Column(name = "requester_id")
    private Integer requesterId;

    @Column(name = "requester_name", length = 50)
    private String requesterName;

    @Column(name = "requisition_date", nullable = false)
    private LocalDate requisitionDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "approver_id")
    private Integer approverId;

    @Column(name = "approver_name", length = 50)
    private String approverName;

    @Column(name = "approve_time")
    private LocalDateTime approveTime;

    @Column(name = "warehouse_keeper_id")
    private Integer warehouseKeeperId;

    @Column(name = "warehouse_keeper_name", length = 50)
    private String warehouseKeeperName;

    @Column(name = "issue_time")
    private LocalDateTime issueTime;

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
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
