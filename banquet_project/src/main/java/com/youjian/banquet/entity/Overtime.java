/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.Overtime
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name="overtime")
public class Overtime {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="overtime_id")
    private Integer overtimeId;
    @Column(name="store_id")
    private Long storeId;
    @Column(name="staff_id")
    private Integer staffId;
    @Column(name="overtime_date")
    private LocalDate overtimeDate;
    @Column(name="start_time")
    private LocalDateTime startTime;
    @Column(name="end_time")
    private LocalDateTime endTime;
    @Column(name="hours")
    private Double hours;
    @Column(name="status")
    private String status;
    @Column(name="reason")
    private String reason;
    @Column(name="approver_id")
    private Integer approverId;
    @Column(name="approve_time")
    private LocalDateTime approveTime;
    @Column(name="approve_remark")
    private String approveRemark;
    @Column(name="created_at")
    private LocalDateTime createdAt;
    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    public Integer getOvertimeId() {
        return this.overtimeId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public Integer getStaffId() {
        return this.staffId;
    }

    public LocalDate getOvertimeDate() {
        return this.overtimeDate;
    }

    public LocalDateTime getStartTime() {
        return this.startTime;
    }

    public LocalDateTime getEndTime() {
        return this.endTime;
    }

    public Double getHours() {
        return this.hours;
    }

    public String getStatus() {
        return this.status;
    }

    public String getReason() {
        return this.reason;
    }

    public Integer getApproverId() {
        return this.approverId;
    }

    public LocalDateTime getApproveTime() {
        return this.approveTime;
    }

    public String getApproveRemark() {
        return this.approveRemark;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    public void setOvertimeId(Integer overtimeId) {
        this.overtimeId = overtimeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public void setOvertimeDate(LocalDate overtimeDate) {
        this.overtimeDate = overtimeDate;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setHours(Double hours) {
        this.hours = hours;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setApproverId(Integer approverId) {
        this.approverId = approverId;
    }

    public void setApproveTime(LocalDateTime approveTime) {
        this.approveTime = approveTime;
    }

    public void setApproveRemark(String approveRemark) {
        this.approveRemark = approveRemark;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Overtime)) {
            return false;
        }
        Overtime other = (Overtime)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$overtimeId = this.getOvertimeId();
        Integer other$overtimeId = other.getOvertimeId();
        if (this$overtimeId == null ? other$overtimeId != null : !((Object)this$overtimeId).equals(other$overtimeId)) {
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
        Double this$hours = this.getHours();
        Double other$hours = other.getHours();
        if (this$hours == null ? other$hours != null : !((Object)this$hours).equals(other$hours)) {
            return false;
        }
        Integer this$approverId = this.getApproverId();
        Integer other$approverId = other.getApproverId();
        if (this$approverId == null ? other$approverId != null : !((Object)this$approverId).equals(other$approverId)) {
            return false;
        }
        LocalDate this$overtimeDate = this.getOvertimeDate();
        LocalDate other$overtimeDate = other.getOvertimeDate();
        if (this$overtimeDate == null ? other$overtimeDate != null : !((Object)this$overtimeDate).equals(other$overtimeDate)) {
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
        String this$reason = this.getReason();
        String other$reason = other.getReason();
        if (this$reason == null ? other$reason != null : !this$reason.equals(other$reason)) {
            return false;
        }
        LocalDateTime this$approveTime = this.getApproveTime();
        LocalDateTime other$approveTime = other.getApproveTime();
        if (this$approveTime == null ? other$approveTime != null : !((Object)this$approveTime).equals(other$approveTime)) {
            return false;
        }
        String this$approveRemark = this.getApproveRemark();
        String other$approveRemark = other.getApproveRemark();
        if (this$approveRemark == null ? other$approveRemark != null : !this$approveRemark.equals(other$approveRemark)) {
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
        return other instanceof Overtime;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $overtimeId = this.getOvertimeId();
        result = result * 59 + ($overtimeId == null ? 43 : ((Object)$overtimeId).hashCode());
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        Integer $staffId = this.getStaffId();
        result = result * 59 + ($staffId == null ? 43 : ((Object)$staffId).hashCode());
        Double $hours = this.getHours();
        result = result * 59 + ($hours == null ? 43 : ((Object)$hours).hashCode());
        Integer $approverId = this.getApproverId();
        result = result * 59 + ($approverId == null ? 43 : ((Object)$approverId).hashCode());
        LocalDate $overtimeDate = this.getOvertimeDate();
        result = result * 59 + ($overtimeDate == null ? 43 : ((Object)$overtimeDate).hashCode());
        LocalDateTime $startTime = this.getStartTime();
        result = result * 59 + ($startTime == null ? 43 : ((Object)$startTime).hashCode());
        LocalDateTime $endTime = this.getEndTime();
        result = result * 59 + ($endTime == null ? 43 : ((Object)$endTime).hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $reason = this.getReason();
        result = result * 59 + ($reason == null ? 43 : $reason.hashCode());
        LocalDateTime $approveTime = this.getApproveTime();
        result = result * 59 + ($approveTime == null ? 43 : ((Object)$approveTime).hashCode());
        String $approveRemark = this.getApproveRemark();
        result = result * 59 + ($approveRemark == null ? 43 : $approveRemark.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        LocalDateTime $updatedAt = this.getUpdatedAt();
        result = result * 59 + ($updatedAt == null ? 43 : ((Object)$updatedAt).hashCode());
        return result;
    }

    public String toString() {
        return "Overtime(overtimeId=" + this.getOvertimeId() + ", storeId=" + this.getStoreId() + ", staffId=" + this.getStaffId() + ", overtimeDate=" + String.valueOf(this.getOvertimeDate()) + ", startTime=" + String.valueOf(this.getStartTime()) + ", endTime=" + String.valueOf(this.getEndTime()) + ", hours=" + this.getHours() + ", status=" + this.getStatus() + ", reason=" + this.getReason() + ", approverId=" + this.getApproverId() + ", approveTime=" + String.valueOf(this.getApproveTime()) + ", approveRemark=" + this.getApproveRemark() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", updatedAt=" + String.valueOf(this.getUpdatedAt()) + ")";
    }

    public Overtime() {
    }

    public Overtime(Integer overtimeId, Long storeId, Integer staffId, LocalDate overtimeDate, LocalDateTime startTime, LocalDateTime endTime, Double hours, String status, String reason, Integer approverId, LocalDateTime approveTime, String approveRemark, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.overtimeId = overtimeId;
        this.storeId = storeId;
        this.staffId = staffId;
        this.overtimeDate = overtimeDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.hours = hours;
        this.status = status;
        this.reason = reason;
        this.approverId = approverId;
        this.approveTime = approveTime;
        this.approveRemark = approveRemark;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

