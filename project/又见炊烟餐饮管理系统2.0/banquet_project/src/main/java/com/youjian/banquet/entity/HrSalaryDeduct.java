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

import java.time.LocalDateTime;

/**
 * 工资扣除规则表
 * 对应参考系统 sal_salary_deduct
 * 按部门配置迟到/早退/旷工/休假的每次扣款金额
 * 扣款类型：0迟到，1早退，2旷工，3休假
 */
@Entity
@Table(name = "hr_salary_deduct")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrSalaryDeduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "dept_id")
    private Integer deptId;

    /** 扣款类型：0迟到，1早退，2旷工，3休假 */
    @Column(name = "type_num")
    private Integer typeNum;

    /** 每次扣款金额 */
    @Column(name = "deduct")
    private Integer deduct;

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