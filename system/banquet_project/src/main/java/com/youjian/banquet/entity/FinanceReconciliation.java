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
@Table(name = "finance_reconciliation")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recon_id")
    private Long reconId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "recon_no")
    private String reconNo;

    @Column(name = "recon_date")
    private LocalDate reconDate;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "book_balance", precision = 12, scale = 2)
    private BigDecimal bookBalance;

    @Column(name = "bank_balance", precision = 12, scale = 2)
    private BigDecimal bankBalance;

    @Column(name = "diff_amount", precision = 12, scale = 2)
    private BigDecimal diffAmount;

    @Column(name = "status")
    private String status;

    @Column(name = "operator_id")
    private Integer operatorId;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "remark")
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
