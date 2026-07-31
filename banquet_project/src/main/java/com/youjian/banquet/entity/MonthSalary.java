package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 月度薪资主表
 * 对应规划手册 5.txt 阶段2.2
 */
@Entity
@Table(name = "month_salary",
       uniqueConstraints = @UniqueConstraint(columnNames = {"staff_id", "salary_month"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthSalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "salary_id")
    private Long salaryId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "salary_month", nullable = false, length = 7)
    private String salaryMonth;

    @Column(name = "base_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "overtime_pay", precision = 10, scale = 2)
    private BigDecimal overtimePay = BigDecimal.ZERO;

    @Column(name = "performance_salary", precision = 10, scale = 2)
    private BigDecimal performanceSalary = BigDecimal.ZERO;

    @Column(name = "reward_amount", precision = 10, scale = 2)
    private BigDecimal rewardAmount = BigDecimal.ZERO;

    @Column(name = "punish_deduction", precision = 10, scale = 2)
    private BigDecimal punishDeduction = BigDecimal.ZERO;

    @Column(name = "leave_deduction", precision = 10, scale = 2)
    private BigDecimal leaveDeduction = BigDecimal.ZERO;

    @Column(name = "social_security_deduction", precision = 10, scale = 2)
    private BigDecimal socialSecurityDeduction = BigDecimal.ZERO;

    @Column(name = "housing_fund_deduction", precision = 10, scale = 2)
    private BigDecimal housingFundDeduction = BigDecimal.ZERO;

    @Column(name = "other_allowance", precision = 10, scale = 2)
    private BigDecimal otherAllowance = BigDecimal.ZERO;

    @Column(name = "other_deduction", precision = 10, scale = 2)
    private BigDecimal otherDeduction = BigDecimal.ZERO;

    @Column(name = "gross_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossSalary;

    @Column(name = "net_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal netSalary;

    @Column(name = "tax_amount", precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "status")
    private Integer status = 0;

    @Column(name = "remark", length = 255)
    private String remark;

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
