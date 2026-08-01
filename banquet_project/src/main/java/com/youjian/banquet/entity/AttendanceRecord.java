package com.youjian.banquet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "record_id")
    private String recordId;

    @Column(name = "emp_id")
    private String empId;

    @Column(name = "emp_name")
    private String empName;

    @Column(name = "department")
    private String department;

    @Column(name = "month")
    private String month;

    @Column(name = "scope")
    private String scope;

    @Column(name = "day_num")
    private Integer dayNum;

    @Column(name = "am_type")
    private String amType;

    @Column(name = "pm_type")
    private String pmType;

    @Column(name = "am_note", columnDefinition = "text")
    private String amNote;

    @Column(name = "pm_note", columnDefinition = "text")
    private String pmNote;

    @Column(name = "day_note", columnDefinition = "text")
    private String dayNote;

    @Column(name = "employment")
    private String employment;

    @Column(name = "salary_status")
    private String salaryStatus;

    @Column(name = "public_holiday")
    private Integer publicHoliday;

    @Column(name = "carry_over")
    private Integer carryOver;

    @Column(name = "summary_notes", columnDefinition = "text")
    private String summaryNotes;

    @Column(name = "total_present", precision = 6, scale = 1)
    private BigDecimal totalPresent;

    @Column(name = "total_statutory", precision = 6, scale = 1)
    private BigDecimal totalStatutory;

    @Column(name = "total_holiday", precision = 6, scale = 1)
    private BigDecimal totalHoliday;

    @Column(name = "total_comp", precision = 6, scale = 1)
    private BigDecimal totalComp;

    @Column(name = "total_travel", precision = 6, scale = 1)
    private BigDecimal totalTravel;

    @Column(name = "total_overtime", precision = 6, scale = 1)
    private BigDecimal totalOvertime;

    @Column(name = "total_leave", precision = 6, scale = 1)
    private BigDecimal totalLeave;

    @Column(name = "total_late", precision = 6, scale = 1)
    private BigDecimal totalLate;

    @Column(name = "total_early", precision = 6, scale = 1)
    private BigDecimal totalEarly;

    @Column(name = "total_absent", precision = 6, scale = 1)
    private BigDecimal totalAbsent;

    @Column(name = "final_balance", precision = 6, scale = 1)
    private BigDecimal finalBalance;

    @Column(name = "recorded_days")
    private Integer recordedDays;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "staff_name")
    private String staffName;

    @Column(name = "store_id")
    private Long storeId;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
