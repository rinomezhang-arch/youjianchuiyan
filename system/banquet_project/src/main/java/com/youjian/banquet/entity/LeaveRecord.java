/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.LeaveRecord
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
@Table(name="leave_record")
public class LeaveRecord {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="leave_id")
    private Integer leaveId;
    @Column(name="store_id")
    private Long storeId;
    @Column(name="staff_id")
    private Integer staffId;
    @Column(name="leave_type")
    private String leaveType;
    @Column(name="start_date")
    private LocalDate startDate;
    @Column(name="end_date")
    private LocalDate endDate;
    @Column(name="days")
    private Double days;
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
    @Column(name="create_time")
    private LocalDateTime createdAt;
    @Column(name="update_time")
    private LocalDateTime updatedAt;

    public Integer getLeaveId() {
        return this.leaveId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public Integer getStaffId() {
        return this.staffId;
    }

    public String getLeaveType() {
        return this.leaveType;
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public LocalDate getEndDate() {
        return this.endDate;
    }

    public Double getDays() {
        return this.days;
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

    public void setLeaveId(Integer leaveId) {
        this.leaveId = leaveId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public void setLeaveType(String leaveType) {
        this.leaveType = leaveType;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void setDays(Double days) {
        this.days = days;
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
        if (!(o instanceof LeaveRecord)) {
            return false;
        }
        LeaveRecord other = (LeaveRecord)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$leaveId = this.getLeaveId();
        Integer other$leaveId = other.getLeaveId();
        if (this$leaveId == null ? other$leaveId != null : !((Object)this$leaveId).equals(other$leaveId)) {
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
        Double this$days = this.getDays();
        Double other$days = other.getDays();
        if (this$days == null ? other$days != null : !((Object)this$days).equals(other$days)) {
            return false;
        }
        Integer this$approverId = this.getApproverId();
        Integer other$approverId = other.getApproverId();
        if (this$approverId == null ? other$approverId != null : !((Object)this$approverId).equals(other$approverId)) {
            return false;
        }
        String this$leaveType = this.getLeaveType();
        String other$leaveType = other.getLeaveType();
        if (this$leaveType == null ? other$leaveType != null : !this$leaveType.equals(other$leaveType)) {
            return false;
        }
        LocalDate this$startDate = this.getStartDate();
        LocalDate other$startDate = other.getStartDate();
        if (this$startDate == null ? other$startDate != null : !((Object)this$startDate).equals(other$startDate)) {
            return false;
        }
        LocalDate this$endDate = this.getEndDate();
        LocalDate other$endDate = other.getEndDate();
        if (this$endDate == null ? other$endDate != null : !((Object)this$endDate).equals(other$endDate)) {
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
        return other instanceof LeaveRecord;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $leaveId = this.getLeaveId();
        result = result * 59 + ($leaveId == null ? 43 : ((Object)$leaveId).hashCode());
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        Integer $staffId = this.getStaffId();
        result = result * 59 + ($staffId == null ? 43 : ((Object)$staffId).hashCode());
        Double $days = this.getDays();
        result = result * 59 + ($days == null ? 43 : ((Object)$days).hashCode());
        Integer $approverId = this.getApproverId();
        result = result * 59 + ($approverId == null ? 43 : ((Object)$approverId).hashCode());
        String $leaveType = this.getLeaveType();
        result = result * 59 + ($leaveType == null ? 43 : $leaveType.hashCode());
        LocalDate $startDate = this.getStartDate();
        result = result * 59 + ($startDate == null ? 43 : ((Object)$startDate).hashCode());
        LocalDate $endDate = this.getEndDate();
        result = result * 59 + ($endDate == null ? 43 : ((Object)$endDate).hashCode());
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
        return "LeaveRecord(leaveId=" + this.getLeaveId() + ", storeId=" + this.getStoreId() + ", staffId=" + this.getStaffId() + ", leaveType=" + this.getLeaveType() + ", startDate=" + String.valueOf(this.getStartDate()) + ", endDate=" + String.valueOf(this.getEndDate()) + ", days=" + this.getDays() + ", status=" + this.getStatus() + ", reason=" + this.getReason() + ", approverId=" + this.getApproverId() + ", approveTime=" + String.valueOf(this.getApproveTime()) + ", approveRemark=" + this.getApproveRemark() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ", updatedAt=" + String.valueOf(this.getUpdatedAt()) + ")";
    }

    public LeaveRecord() {
    }

    public LeaveRecord(Integer leaveId, Long storeId, Integer staffId, String leaveType, LocalDate startDate, LocalDate endDate, Double days, String status, String reason, Integer approverId, LocalDateTime approveTime, String approveRemark, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.leaveId = leaveId;
        this.storeId = storeId;
        this.staffId = staffId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.days = days;
        this.status = status;
        this.reason = reason;
        this.approverId = approverId;
        this.approveTime = approveTime;
        this.approveRemark = approveRemark;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}

