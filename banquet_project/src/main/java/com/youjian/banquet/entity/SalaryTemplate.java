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
import java.time.LocalDateTime;

/**
 * 薪资标准模板
 * 对应规划手册 5.txt 阶段2.2
 */
@Entity
@Table(name = "salary_template")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "post_name", nullable = false, length = 50)
    private String postName;

    @Column(name = "base_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "overtime_rate", precision = 3, scale = 1)
    private BigDecimal overtimeRate = new BigDecimal("1.5");

    @Column(name = "meal_subsidy", precision = 10, scale = 2)
    private BigDecimal mealSubsidy = BigDecimal.ZERO;

    @Column(name = "transport_subsidy", precision = 10, scale = 2)
    private BigDecimal transportSubsidy = BigDecimal.ZERO;

    @Column(name = "housing_subsidy", precision = 10, scale = 2)
    private BigDecimal housingSubsidy = BigDecimal.ZERO;

    @Column(name = "attendance_bonus", precision = 10, scale = 2)
    private BigDecimal attendanceBonus = BigDecimal.ZERO;

    @Column(name = "social_security_employee", precision = 10, scale = 2)
    private BigDecimal socialSecurityEmployee = BigDecimal.ZERO;

    @Column(name = "housing_fund_employee", precision = 10, scale = 2)
    private BigDecimal housingFundEmployee = BigDecimal.ZERO;

    @Column(name = "performance_ratio", precision = 5, scale = 2)
    private BigDecimal performanceRatio = new BigDecimal("30");

    @Column(name = "is_active")
    private Integer isActive = 1;

    @Column(name = "remark", length = 255)
    private String remark;

    @Column(name = "created_at")
    private LocalDateTime createTime;

    @Column(name = "updated_at")
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
