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
 * 费用报销实体。对应 finance_expense 表。
 * <p>说明：occurDate 映射到存量列 expense_date；status 映射到存量列 approval_status。
 */
@Entity
@Table(name = "finance_expense")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    private Long expenseId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "expense_no", length = 50)
    private String expenseNo;

    @Column(name = "expense_type", length = 50)
    private String expenseType;

    /** 发生日期，映射到存量列 expense_date */
    @Column(name = "expense_date")
    private LocalDate occurDate;

    @Column(name = "applicant_id")
    private Integer applicantId;

    @Column(name = "applicant_name", length = 50)
    private String applicantName;

    @Column(name = "department_id")
    private Integer departmentId;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "invoice_amount", precision = 12, scale = 2)
    private BigDecimal invoiceAmount;

    /** 待审/已批/已驳/已付，映射到存量列 approval_status */
    @Column(name = "approval_status", length = 20)
    private String status;

    @Column(name = "approver_id")
    private Integer approverId;

    @Column(name = "approver_name", length = 50)
    private String approverName;

    @Column(name = "approve_time")
    private LocalDateTime approveTime;

    @Column(name = "approve_remark", length = 500)
    private String approveRemark;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus;

    @Column(name = "payment_time")
    private LocalDateTime paymentTime;

    @Column(name = "account_id")
    private Long accountId;

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
            this.status = "待审";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
