/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.Schedule
 *  jakarta.persistence.Column
 *  jakarta.persistence.Entity
 *  jakarta.persistence.GeneratedValue
 *  jakarta.persistence.GenerationType
 *  jakarta.persistence.Id
 *  jakarta.persistence.Table
 */
package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="schedule")
public class Schedule {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="schedule_id")
    private Integer scheduleId;
    @Column(name="store_id")
    private Long storeId;
    @Column(name="staff_id")
    private Integer staffId;
    @Column(name="schedule_date")
    private LocalDate scheduleDate;
    @Column(name="shift_type")
    private String shiftType;
    @Column(name="start_time")
    private LocalDateTime startTime;
    @Column(name="end_time")
    private LocalDateTime endTime;
    @Column(name="status")
    private String status;
    @Column(name="remark")
    private String remark;
    @Column(name="create_time")
    private LocalDateTime createdAt;
    @Column(name="update_time")
    private LocalDateTime updatedAt;

    public Integer getScheduleId() {
        return this.scheduleId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public Integer getStaffId() {
        return this.staffId;
    }

    public LocalDate getScheduleDate() {
        return this.scheduleDate;
    }

    public String getShiftType() {
        return this.shiftType;
    }

    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    public LocalDateTime getEndTime() {
        return this.endTime;
    }

    public String getStatus() {
        return this.status;
    }

    public String getRemark() {
        return this.remark;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setScheduleId(Integer scheduleId) {
        this.scheduleId = scheduleId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public void setShiftType(String shiftType) {
        this.shiftType = shiftType;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Schedule)) {
            return false;
        }
        Schedule other = (Schedule)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$scheduleId = this.getScheduleId();
        Integer other$scheduleId = other.getScheduleId();
        if (this$scheduleId == null ? other$scheduleId != null : !((Object)this$scheduleId).equals(other$scheduleId)) {
            return false;
        }
        Long this$storeId = this.getStoreId();
        Long other$storeId = other.getStoreId();
        if (this$storeId == null ? other$storeId != null : !((Object)this$storeId).equals(other$storeId)) {
            return false;
        }
        Integer this$staffId = this.getStaffId();
        Integer other$staffId = other.getStaffId();
        if (this$staffId == null ? other$staffId != null : !((Object)this$staffId).equals(other$staffId)) {
            return false;
        }
        LocalDate this$scheduleDate = this.getScheduleDate();
        LocalDate other$scheduleDate = other.getScheduleDate();
        if (this$scheduleDate == null ? other$scheduleDate != null : !((Object)this$scheduleDate).equals(other$scheduleDate)) {
            return false;
        }
        String this$shiftType = this.getShiftType();
        String other$shiftType = other.getShiftType();
        if (this$shiftType == null ? other$shiftType != null : !this$shiftType.equals(other$shiftType)) {
            return false;
        }
        LocalDateTime this$startTime = this.getStartTime();
        LocalDateTime other$startTime = other.getStartTime();
        if (this$startTime == null ? other$startTime != null : !((Object)this$startTime).equals(other$startTime)) {
            return false;
        }
        LocalDateTime this$endTime = this.getEndTime();
        LocalDateTime other$endTime = other.getEndTime();
        if (this$endTime == null ? other$endTime != null : !((Object)this$endTime).equals(other$endTime)) {
            return false;
        }
        String this$status = this.getStatus();
        String other$status = other.getStatus();
        if (this$status == null ? other$status != null : !this$status.equals(other$status)) {
            return false;
        }
        String this$remark = this.getRemark();
        String other$remark = other.getRemark();
        if (this$remark == null ? other$remark != null : !this$remark.equals(other$remark)) {
            return false;
        }
        LocalDateTime this$createdAt = this.getCreatedAt();
        LocalDateTime other$createdAt = other.getCreatedAt();
        if (this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt)) {
            return false;
        }
        LocalDateTime this$updatedAt = this.getUpdatedAt();
        LocalDateTime other$updatedAt = other.getUpdatedAt();
        return !(this$updatedAt == null ? other$updatedAt != null : !((Object)this$updatedAt).equals(other$updatedAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof Schedule;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $scheduleId = this.getScheduleId();
        result = result * 59 + ($scheduleId == null ? 43 : ((Object)$scheduleId).hashCode());
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        Integer $staffId = this.getStaffId();
        result = result * 59 + ($staffId == null ? 43 : ((Object)$staffId).hashCode());
        LocalDate $scheduleDate = this.getScheduleDate();
        result = result * 59 + ($scheduleDate == null ? 43 : ((Object)$scheduleDate).hashCode());
        String $shiftType = this.getShiftType();
        result = result * 59 + ($shiftType == null ? 43 : $shiftType.hashCode());
        LocalDateTime $startTime = this.getStartTime();
        result = result * 59 + ($startTime == null ? 43 : ((Object)$startTime).hashCode());
        LocalDateTime $endTime = this.getEndTime();
        result = result * 59 + ($endTime == null ? 43 : ((Object)$endTime).hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $remark = this.getRemark();
        result = result * 59 + ($remark == null ? 43 : $remark.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "Schedule(scheduleId=" + this.getScheduleId() + ", storeId=" + this.getStoreId() + ", staffId=" + this.getStaffId() + ", scheduleDate=" + String.valueOf(this.getScheduleDate()) + ", shiftType=" + this.getShiftType() + ", startTime=" + String.valueOf(this.getStartTime()) + ", endTime=" + String.valueOf(this.getEndTime()) + ", status=" + this.getStatus() + ", remark=" + this.getRemark() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", updatedAt=" + String.valueOf(this.getUpdatedAt()) + ")";
    }

    public Schedule() {
    }

    public Schedule(Integer scheduleId, Long storeId, Integer staffId, LocalDate scheduleDate, String shiftType, LocalDateTime startTime, LocalDateTime endTime, String status, String remark, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.scheduleId = scheduleId;
        this.storeId = storeId;
        this.staffId = staffId;
        this.scheduleDate = scheduleDate;
        this.shiftType = shiftType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.remark = remark;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

