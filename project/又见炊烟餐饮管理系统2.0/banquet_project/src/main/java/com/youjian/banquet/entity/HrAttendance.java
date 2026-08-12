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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 员工考勤表（复刻自HR系统 att_attendance 表）
 * 记录员工每日上午/下午的打卡时间及考勤状态
 *
 * @author cow
 * @since 2022-03-29
 */
@Entity
@Table(name = "att_attendance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** 门店ID（多租户） */
    @Column(name = "store_id")
    private Long storeId;

    /** 员工id */
    @Column(name = "staff_id")
    private Integer staffId;

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

    /** 考勤日期 */
    @Column(name = "attendance_date")
    private LocalDate attendanceDate;

    /** 0正常，1迟到，2早退，3旷工，4休假 */
    @Column(name = "status")
    private Integer status;

    /** 备注 */
    @Column(name = "remark")
    private String remark;

    /** 创建时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /** 逻辑删除，0未删除，1删除 */
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