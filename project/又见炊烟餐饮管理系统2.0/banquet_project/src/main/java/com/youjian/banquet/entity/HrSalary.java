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
 * 员工工资表
 * 对应参考系统 sal_salary
 * 薪资计算规则：基础工资 + 加班费 + 补贴 + 奖金 - 迟到扣款 - 早退扣款 - 旷工扣款 - 休假扣款 - 社保 - 公积金 = 总工资
 */
@Entity
@Table(name = "hr_salary")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrSalary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "base_salary", precision = 10, scale = 3)
    private BigDecimal baseSalary;

    @Column(name = "overtime_salary", precision = 10, scale = 3)
    private BigDecimal overtimeSalary;

    @Column(name = "subsidy", precision = 10, scale = 3)
    private BigDecimal subsidy;

    @Column(name = "bonus", precision = 10, scale = 3)
    private BigDecimal bonus;

    @Column(name = "total_salary", precision = 10, scale = 3)
    private BigDecimal totalSalary;

    @Column(name = "late_deduct", precision = 10, scale = 3)
    private BigDecimal lateDeduct;

    @Column(name = "leave_deduct", precision = 10, scale = 3)
    private BigDecimal leaveDeduct;

    @Column(name = "leave_early_deduct", precision = 10, scale = 3)
    private BigDecimal leaveEarlyDeduct;

    @Column(name = "absenteeism_deduct", precision = 10, scale = 3)
    private BigDecimal absenteeismDeduct;

    @Column(name = "month", length = 6)
    private String month;

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "is_deleted")
    private Integer isDeleted;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.isDeleted == null) {
            this.isDeleted = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}