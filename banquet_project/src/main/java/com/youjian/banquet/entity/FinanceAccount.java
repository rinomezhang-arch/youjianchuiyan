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
import java.time.LocalDateTime;

/**
 * 资金账户实体。
 * <p>对应 finance_account 表；ID 由应用层生成（System.currentTimeMillis），与 FinanceController 保持一致，
 * 故不使用 @GeneratedValue。
 */
@Entity
@Table(name = "finance_account")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceAccount {

    @Id
    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "account_name")
    private String accountName;

    /** 现金/银行/支付宝/微信 */
    @Column(name = "account_type")
    private String accountType;

    @Column(name = "opening_balance", precision = 14, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "current_balance", precision = 14, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "card_no")
    private String cardNo;

    /** active/inactive */
    @Column(name = "status")
    private String status;

    @Column(name = "remark")
    private String remark;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.status == null) {
            this.status = "active";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
