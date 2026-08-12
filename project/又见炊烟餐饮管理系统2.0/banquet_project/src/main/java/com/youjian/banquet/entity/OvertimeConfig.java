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
 * 加班配置表
 * 对应数据库表: att_overtime
 */
@Entity
@Table(name = "att_overtime")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "salary_multiple", precision = 10, scale = 3)
    private BigDecimal salaryMultiple;

    @Column(name = "bonus", precision = 10, scale = 3)
    private BigDecimal bonus;

    @Column(name = "type_num")
    private Integer typeNum;

    @Column(name = "dept_id")
    private Integer deptId;

    @Column(name = "count_type")
    private Integer countType;

    @Column(name = "remark", length = 200)
    private String remark;

    @Column(name = "is_time_off")
    private Integer isTimeOff;

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
        if (this.countType == null) {
            this.countType = 0;
        }
        if (this.isTimeOff == null) {
            this.isTimeOff = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}