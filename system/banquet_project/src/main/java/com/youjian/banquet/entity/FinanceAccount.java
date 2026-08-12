package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.youjian.banquet.config.BankAccountConverter;
import com.youjian.banquet.config.SensitiveDataSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金账户实体。
 * <p>对应 finance_account 表；ID 由应用层生成（System.currentTimeMillis），与 FinanceController 保持一致，
 * 故不使用 @GeneratedValue。
 * <p>P1-14 深度审计修复：列名对齐 DB 实际结构（initial_balance/is_active/created_at/updated_at）
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

    @Column(name = "account_code")
    private String accountCode;

    @Column(name = "account_name")
    private String accountName;

    /** 现金/银行/支付宝/微信 */
    @Column(name = "account_type")
    private String accountType;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account")
    @Convert(converter = BankAccountConverter.class)
    @JsonSerialize(using = SensitiveDataSerializer.class)
    private String bankAccount;

    @Column(name = "account_holder")
    private String accountHolder;

    @Column(name = "initial_balance", precision = 14, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "current_balance", precision = 14, scale = 2)
    private BigDecimal currentBalance;

    /** DB列 is_active (tinyint): 1=启用 0=禁用 */
    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "remark")
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createTime;

    @Column(name = "updated_at")
    private LocalDateTime updateTime;

    /**
     * 兼容旧API：status 字符串 ↔ isActive 布尔
     */
    public String getStatus() {
        return Boolean.TRUE.equals(isActive) ? "active" : "inactive";
    }

    public void setStatus(String status) {
        this.isActive = "active".equalsIgnoreCase(status);
    }

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
