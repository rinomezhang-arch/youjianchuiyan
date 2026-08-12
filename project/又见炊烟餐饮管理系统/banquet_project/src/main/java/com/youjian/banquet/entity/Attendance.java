/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.entity.Attendance
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
@Table(name="attendance")
public class Attendance {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="attendance_id")
    private Integer attendanceId;
    @Column(name="store_id")
    private Long storeId;
    @Column(name="staff_id")
    private Integer staffId;
    @Column(name="attendance_date")
    private LocalDate attendanceDate;
    @Column(name="clock_in")
    private LocalDateTime clockIn;
    @Column(name="clock_out")
    private LocalDateTime clockOut;
    @Column(name="status")
    private String status;
    @Column(name="late_minutes")
    private Integer lateMinutes;
    @Column(name="early_leave_minutes")
    private Integer earlyLeaveMinutes;
    @Column(name="absent")
    private Integer absent;
    @Column(name="work_hours")
    private Double workHours;
    @Column(name="remark")
    private String remark;
    @Column(name="create_time")
    private LocalDateTime createdAt;

    public Integer getAttendanceId() {
        return this.attendanceId;
    }

    public Long getStoreId() {
        return this.storeId;
    }

    public Integer getStaffId() {
        return this.staffId;
    }

    public LocalDate getAttendanceDate() {
        return this.attendanceDate;
    }

    public LocalDateTime getClockIn() {
        return this.clockIn;
    }

    public LocalDateTime getClockOut() {
        return this.clockOut;
    }

    public String getStatus() {
        return this.status;
    }

    public Integer getLateMinutes() {
        return this.lateMinutes;
    }

    public Integer getEarlyLeaveMinutes() {
        return this.earlyLeaveMinutes;
    }

    public Integer getAbsent() {
        return this.absent;
    }

    public Double getWorkHours() {
        return this.workHours;
    }

    public String getRemark() {
        return this.remark;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setAttendanceId(Integer attendanceId) {
        this.attendanceId = attendanceId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public void setStaffId(Integer staffId) {
        this.staffId = staffId;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public void setClockIn(LocalDateTime clockIn) {
        this.clockIn = clockIn;
    }

    public void setClockOut(LocalDateTime clockOut) {
        this.clockOut = clockOut;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setLateMinutes(Integer lateMinutes) {
        this.lateMinutes = lateMinutes;
    }

    public void setEarlyLeaveMinutes(Integer earlyLeaveMinutes) {
        this.earlyLeaveMinutes = earlyLeaveMinutes;
    }

    public void setAbsent(Integer absent) {
        this.absent = absent;
    }

    public void setWorkHours(Double workHours) {
        this.workHours = workHours;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Attendance)) {
            return false;
        }
        Attendance other = (Attendance)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        Integer this$attendanceId = this.getAttendanceId();
        Integer other$attendanceId = other.getAttendanceId();
        if (this$attendanceId == null ? other$attendanceId != null : !((Object)this$attendanceId).equals(other$attendanceId)) {
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
        Integer this$lateMinutes = this.getLateMinutes();
        Integer other$lateMinutes = other.getLateMinutes();
        if (this$lateMinutes == null ? other$lateMinutes != null : !((Object)this$lateMinutes).equals(other$lateMinutes)) {
            return false;
        }
        Integer this$earlyLeaveMinutes = this.getEarlyLeaveMinutes();
        Integer other$earlyLeaveMinutes = other.getEarlyLeaveMinutes();
        if (this$earlyLeaveMinutes == null ? other$earlyLeaveMinutes != null : !((Object)this$earlyLeaveMinutes).equals(other$earlyLeaveMinutes)) {
            return false;
        }
        Integer this$absent = this.getAbsent();
        Integer other$absent = other.getAbsent();
        if (this$absent == null ? other$absent != null : !((Object)this$absent).equals(other$absent)) {
            return false;
        }
        Double this$workHours = this.getWorkHours();
        Double other$workHours = other.getWorkHours();
        if (this$workHours == null ? other$workHours != null : !((Object)this$workHours).equals(other$workHours)) {
            return false;
        }
        LocalDate this$attendanceDate = this.getAttendanceDate();
        LocalDate other$attendanceDate = other.getAttendanceDate();
        if (this$attendanceDate == null ? other$attendanceDate != null : !((Object)this$attendanceDate).equals(other$attendanceDate)) {
            return false;
        }
        LocalDateTime this$clockIn = this.getClockIn();
        LocalDateTime other$clockIn = other.getClockIn();
        if (this$clockIn == null ? other$clockIn != null : !((Object)this$clockIn).equals(other$clockIn)) {
            return false;
        }
        LocalDateTime this$clockOut = this.getClockOut();
        LocalDateTime other$clockOut = other.getClockOut();
        if (this$clockOut == null ? other$clockOut != null : !((Object)this$clockOut).equals(other$clockOut)) {
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
        return !(this$createdAt == null ? other$createdAt != null : !((Object)this$createdAt).equals(other$createdAt));
    }

    protected boolean canEqual(Object other) {
        return other instanceof Attendance;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $attendanceId = this.getAttendanceId();
        result = result * 59 + ($attendanceId == null ? 43 : ((Object)$attendanceId).hashCode());
        Long $storeId = this.getStoreId();
        result = result * 59 + ($storeId == null ? 43 : ((Object)$storeId).hashCode());
        Integer $staffId = this.getStaffId();
        result = result * 59 + ($staffId == null ? 43 : ((Object)$staffId).hashCode());
        Integer $lateMinutes = this.getLateMinutes();
        result = result * 59 + ($lateMinutes == null ? 43 : ((Object)$lateMinutes).hashCode());
        Integer $earlyLeaveMinutes = this.getEarlyLeaveMinutes();
        result = result * 59 + ($earlyLeaveMinutes == null ? 43 : ((Object)$earlyLeaveMinutes).hashCode());
        Integer $absent = this.getAbsent();
        result = result * 59 + ($absent == null ? 43 : ((Object)$absent).hashCode());
        Double $workHours = this.getWorkHours();
        result = result * 59 + ($workHours == null ? 43 : ((Object)$workHours).hashCode());
        LocalDate $attendanceDate = this.getAttendanceDate();
        result = result * 59 + ($attendanceDate == null ? 43 : ((Object)$attendanceDate).hashCode());
        LocalDateTime $clockIn = this.getClockIn();
        result = result * 59 + ($clockIn == null ? 43 : ((Object)$clockIn).hashCode());
        LocalDateTime $clockOut = this.getClockOut();
        result = result * 59 + ($clockOut == null ? 43 : ((Object)$clockOut).hashCode());
        String $status = this.getStatus();
        result = result * 59 + ($status == null ? 43 : $status.hashCode());
        String $remark = this.getRemark();
        result = result * 59 + ($remark == null ? 43 : $remark.hashCode());
        LocalDateTime $createdAt = this.getCreatedAt();
        result = result * 59 + ($createdAt == null ? 43 : ((Object)$createdAt).hashCode());
        return result;
    }

    public String toString() {
        return "Attendance(attendanceId=" + this.getAttendanceId() + ", storeId=" + this.getStoreId() + ", staffId=" + this.getStaffId() + ", attendanceDate=" + String.valueOf(this.getAttendanceDate()) + ", clockIn=" + String.valueOf(this.getClockIn()) + ", clockOut=" + String.valueOf(this.getClockOut()) + ", status=" + this.getStatus() + ", lateMinutes=" + this.getLateMinutes() + ", earlyLeaveMinutes=" + this.getEarlyLeaveMinutes() + ", absent=" + this.getAbsent() + ", workHours=" + this.getWorkHours() + ", remark=" + this.getRemark() + ", createdAt=" + String.valueOf(this.getCreatedAt()) + ")";
    }

    public Attendance() {
    }

    public Attendance(Integer attendanceId, Long storeId, Integer staffId, LocalDate attendanceDate, LocalDateTime clockIn, LocalDateTime clockOut, String status, Integer lateMinutes, Integer earlyLeaveMinutes, Integer absent, Double workHours, String remark, LocalDateTime createdAt) {
        this.attendanceId = attendanceId;
        this.storeId = storeId;
        this.staffId = staffId;
        this.attendanceDate = attendanceDate;
        this.clockIn = clockIn;
        this.clockOut = clockOut;
        this.status = status;
        this.lateMinutes = lateMinutes;
        this.earlyLeaveMinutes = earlyLeaveMinutes;
        this.absent = absent;
        this.workHours = workHours;
        this.remark = remark;
        this.createdAt = createdAt;
    }
}

