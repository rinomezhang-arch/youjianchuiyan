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

/**
 * 供应商应付实体。对应 finance_payable 表。
 */
@Entity
@Table(name = "finance_payable")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancePayable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payable_id")
    private Long payableId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "payable_no", length = 50)
    private String payableNo;

    @Column(name = "supplier_id")
    private Integer supplierId;

    @Column(name = "supplier_name", length = 100)
    private String supplierName;

    @Column(name = "purchase_id")
    private Integer purchaseId;

    @Column(name = "purchase_no", length = 50)
    private String purchaseNo;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "pending_amount", precision = 12, scale = 2)
    private BigDecimal pendingAmount;

    @Column(name = "payable_date")
    private LocalDate payableDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /** unpaid/partial/paid */
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "credit_days")
    private Integer creditDays;

    @Column(name = "operator_id")
    private Integer operatorId;

    @Column(name = "operator_name", length = 50)
    private String operatorName;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createTime;

    @Column(name = "updated_at")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.status == null) {
            this.status = "unpaid";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
