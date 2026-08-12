package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * HR部门表（复刻自HR系统 sys_dept 表）
 * 使用 JPA 替代 MyBatis-Plus，增加 store_id 多租户支持
 *
 * @author cow
 * @since 2022-03-07
 */
@Entity
@Table(name = "hr_dept")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrDept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** 门店ID（多租户） */
    @Column(name = "store_id")
    private Long storeId;

    /** 部门编码 */
    @Column(name = "code")
    private String code;

    /** 部门名称 */
    @Column(name = "name")
    private String name;

    /** 上午上班时间 */
    @Column(name = "mor_start_time")
    private LocalTime morStartTime;

    /** 上午下班时间 */
    @Column(name = "mor_end_time")
    private LocalTime morEndTime;

    /** 下午上班时间 */
    @Column(name = "aft_start_time")
    private LocalTime aftStartTime;

    /** 下午下班时间 */
    @Column(name = "aft_end_time")
    private LocalTime aftEndTime;

    /** 员工的总工作时长 */
    @Column(name = "total_work_time")
    private BigDecimal totalWorkTime;

    /** 部门备注 */
    @Column(name = "remark")
    private String remark;

    /** 父级部门id，0根部门 */
    @Column(name = "parent_id")
    private Integer parentId;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除，0未删除，1删除 */
    @Column(name = "is_deleted")
    private Integer isDeleted;

    /** 子部门（非数据库字段） */
    @Transient
    private List<HrDept> children;

    @PrePersist
    protected void onCreate() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        if (this.isDeleted == null) {
            this.isDeleted = 0;
        }
        if (this.parentId == null) {
            this.parentId = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}