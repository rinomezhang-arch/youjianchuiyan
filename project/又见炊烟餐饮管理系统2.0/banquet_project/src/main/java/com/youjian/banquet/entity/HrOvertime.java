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
 * HR加班配置表
 * 来源：HR系统 att_overtime
 * 对应数据库表：hr_overtime
 */
@Entity
@Table(name = "hr_overtime")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrOvertime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "salary_multiple", precision = 5, scale = 2)
    private BigDecimal salaryMultiple;

    @Column(name = "multiple_salary", precision = 10, scale = 3)
    private BigDecimal multipleSalary;

    @Column(name = "bonus", precision = 10, scale = 3)
    private BigDecimal bonus;

    @Column(name = "type_num")
    private Integer typeNum;

    @Column(name = "dept_id")
    private Integer deptId;

    @Column(name = "count_type")
    private Integer countType;

    @Column(name = "make_up")
    private Integer makeUp;

    @Column(name = "status")
    private Integer status;

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
        if (this.isDeleted == null) {
            this.isDeleted = 0;
        }
        if (this.status == null) {
            this.status = 1;
        }
        if (this.countType == null) {
            this.countType = 0;
        }
        if (this.makeUp == null) {
            this.makeUp = 0;
        }
        if (this.storeId == null) {
            this.storeId = 1L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}