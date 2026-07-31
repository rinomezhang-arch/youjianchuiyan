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
 * 费用报销实体。对应 finance_expense 表。
 * <p>说明：occurDate 映射到存量列 expense_date（避免冗余）；status 映射到存量列 approval_status。
 * dept_name / remark 由 finance_migration_v1.sql 补齐。
 */
@Entity
@Table(name = "finance_expense")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceExpense {

    @Id
    @Column(name = "expense_id")
    private Long expenseId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "expense_no")
    private String expenseNo;

    @Column(name = "applicant_name")
    private String applicantName;

    @Column(name = "dept_name")
    private String deptName;

    /** 差旅/办公/招待/其他 */
    @Column(name = "expense_type")
    private String expenseType;

    @Column(name = "amount", precision = 14, scale = 2)
    private BigDecimal amount;

    /** 发生日期，映射到存量列 expense_date */
    @Column(name = "expense_date")
    private LocalDate occurDate;

    /** 待审/已批/已驳/已付，映射到存量列 approval_status */
    @Column(name = "approval_status")
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
            this.status = "待审";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
