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

import java.time.LocalDateTime;

/**
 * 月度排班主表
 * 对应规划手册 5.txt 阶段2.1
 */
@Entity
@Table(name = "schedule_month",
       uniqueConstraints = @UniqueConstraint(columnNames = {"schedule_month", "dept_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleMonth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "dept_id", nullable = false)
    private Long deptId;

    @Column(name = "schedule_month", nullable = false, length = 7)
    private String scheduleMonth;

    @Column(name = "status")
    private Integer status = 0;

    @Column(name = "published_by")
    private Long publishedBy;

    @Column(name = "published_time")
    private LocalDateTime publishedTime;

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
