package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * supplier_id / unpaid_amount / last_settle_date / remark 由 finance_migration_v1.sql 补齐。
 */
@Entity
@Table(name = "finance_payable")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancePayable {

    @Id
    @Column(name = "payable_id")
    private Long payableId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", precision = 14, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "unpaid_amount", precision = 14, scale = 2)
    private BigDecimal unpaidAmount;

    @Column(name = "last_settle_date")
    private LocalDate lastSettleDate;

    /** unpaid/partial/paid */
    @Column(name = "status")
    private String status;

    @Column(name = "remark")
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
