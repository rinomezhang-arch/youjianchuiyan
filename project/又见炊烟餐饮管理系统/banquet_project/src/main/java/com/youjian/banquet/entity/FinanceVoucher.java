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
 * 凭证总账实体。对应 finance_voucher 表。
 * <p>说明：debitAmount 映射到存量列 total_debit；creditAmount 映射到 total_credit；
 * makerName 映射到 prepared_name。无需新增列。
 */
@Entity
@Table(name = "finance_voucher")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceVoucher {

    @Id
    @Column(name = "voucher_id")
    private Long voucherId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "voucher_no")
    private String voucherNo;

    @Column(name = "voucher_date")
    private LocalDate voucherDate;

    /** 收款/付款/转账/结转 */
    @Column(name = "voucher_type")
    private String voucherType;

    @Column(name = "summary")
    private String summary;

    /** 借方金额，映射到存量列 total_debit */
    @Column(name = "total_debit", precision = 14, scale = 2)
    private BigDecimal debitAmount;

    /** 贷方金额，映射到存量列 total_credit */
    @Column(name = "total_credit", precision = 14, scale = 2)
    private BigDecimal creditAmount;

    /** 制单人，映射到存量列 prepared_name */
    @Column(name = "prepared_name")
    private String makerName;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
