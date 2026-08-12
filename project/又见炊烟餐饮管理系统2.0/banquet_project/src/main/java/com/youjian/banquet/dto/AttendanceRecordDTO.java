package com.youjian.banquet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class AttendanceRecordDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceLoadDTO {
        private String empId;
        private String empName;
        private String department;
        private String month;
        private String employment;
        private String salaryStatus;
        private BigDecimal totalPresent;
        private BigDecimal totalStatutory;
        private BigDecimal totalHoliday;
        private BigDecimal totalComp;
        private BigDecimal totalTravel;
        private BigDecimal totalOvertime;
        private BigDecimal totalLeave;
        private BigDecimal totalLate;
        private BigDecimal totalEarly;
        private BigDecimal totalAbsent;
        private BigDecimal finalBalance;
        private List<DayRecord> days;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceSaveDTO {
        private String empId;
        private String empName;
        private String department;
        private String month;
        private String scope;
        private String employment;
        private String salaryStatus;
        private Integer publicHoliday;
        private Integer carryOver;
        private String summaryNotes;
        private List<DayRecord> days;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceSummaryDTO {
        private String empId;
        private String empName;
        private String department;
        private String month;
        private String employment;
        private String salaryStatus;
        private BigDecimal totalPresent;
        private BigDecimal totalStatutory;
        private BigDecimal totalHoliday;
        private BigDecimal totalComp;
        private BigDecimal totalTravel;
        private BigDecimal totalOvertime;
        private BigDecimal totalLeave;
        private BigDecimal totalLate;
        private BigDecimal totalEarly;
        private BigDecimal totalAbsent;
        private BigDecimal finalBalance;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayRecord {
        private Integer dayNum;
        private String amType;
        private String pmType;
        private String amNote;
        private String pmNote;
        private String dayNote;
    }
}
