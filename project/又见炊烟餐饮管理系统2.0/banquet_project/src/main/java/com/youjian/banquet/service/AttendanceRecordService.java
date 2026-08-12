package com.youjian.banquet.service;

import com.youjian.banquet.dto.AttendanceRecordDTO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceRecordService {
    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 加载单员工某月考勤记录。
     * 门店数据隔离：storeId 非空时，通过 JOIN staff_master 确保 emp_id 归属当前用户门店。
     *
     * @param empId   员工ID（对应 staff_master.staff_id）
     * @param month   月份 YYYY-MM
     * @param storeId 当前用户门店ID；null 表示不限制（总经理全局）
     */
    public AttendanceRecordDTO.AttendanceLoadDTO loadAttendance(String empId, String month, Long storeId) {
        String sql;
        Object[] params;
        if (storeId != null) {
            // JOIN staff_master 确保 emp_id/staff_id 正确关联并按门店过滤
            sql = "SELECT ar.* FROM attendance_records ar " +
                    "INNER JOIN staff_master sm ON CAST(ar.emp_id AS UNSIGNED) = sm.staff_id " +
                    "WHERE ar.emp_id = ? AND ar.month = ? AND sm.store_id = ? " +
                    "ORDER BY ar.day_num";
            params = new Object[]{empId, month, storeId};
        } else {
            sql = "SELECT * FROM attendance_records WHERE emp_id = ? AND month = ? ORDER BY day_num";
            params = new Object[]{empId, month};
        }
        List<Map<String, Object>> rows = this.jdbc.queryForList(sql, params);
        if (rows.isEmpty()) {
            return null;
        }
        AttendanceRecordDTO.AttendanceLoadDTO dto = new AttendanceRecordDTO.AttendanceLoadDTO();
        Map first = (Map) rows.get(0);
        dto.setEmpId(this.asString(first.get("emp_id")));
        dto.setEmpName(this.asString(first.get("emp_name")));
        dto.setDepartment(this.asString(first.get("department")));
        dto.setMonth(month);
        dto.setEmployment(this.asString(first.get("employment")));
        dto.setSalaryStatus(this.asString(first.get("salary_status")));
        dto.setTotalPresent(this.asBd(first.get("total_present")));
        dto.setTotalStatutory(this.asBd(first.get("total_statutory")));
        dto.setTotalHoliday(this.asBd(first.get("total_holiday")));
        dto.setTotalComp(this.asBd(first.get("total_comp")));
        dto.setTotalTravel(this.asBd(first.get("total_travel")));
        dto.setTotalOvertime(this.asBd(first.get("total_overtime")));
        dto.setTotalLeave(this.asBd(first.get("total_leave")));
        dto.setTotalLate(this.asBd(first.get("total_late")));
        dto.setTotalEarly(this.asBd(first.get("total_early")));
        dto.setTotalAbsent(this.asBd(first.get("total_absent")));
        dto.setFinalBalance(this.asBd(first.get("final_balance")));
        ArrayList<AttendanceRecordDTO.DayRecord> days = new ArrayList<AttendanceRecordDTO.DayRecord>();
        for (Map r : rows) {
            AttendanceRecordDTO.DayRecord day = new AttendanceRecordDTO.DayRecord();
            day.setDayNum(this.asInt(r.get("day_num")));
            day.setAmType(this.asString(r.get("am_type")));
            day.setPmType(this.asString(r.get("pm_type")));
            day.setAmNote(this.asString(r.get("am_note")));
            day.setPmNote(this.asString(r.get("pm_note")));
            day.setDayNote(this.asString(r.get("day_note")));
            days.add(day);
        }
        dto.setDays(days);
        return dto;
    }

    /**
     * 保存/更新考勤记录。
     * 门店数据隔离：storeId 非空时，校验 emp_id 必须归属当前用户门店。
     */
    @Transactional
    public void saveAttendance(AttendanceRecordDTO.AttendanceSaveDTO dto, Long storeId) {
        // 门店校验：店长仅可操作本店员工考勤
        if (storeId != null) {
            List<Map<String, Object>> check = this.jdbc.queryForList(
                    "SELECT staff_id FROM staff_master WHERE staff_id = ? AND store_id = ? LIMIT 1",
                    Integer.parseInt(dto.getEmpId()), storeId);
            if (check.isEmpty()) {
                throw new IllegalArgumentException("无权操作非本店员工考勤");
            }
        }
        this.jdbc.update("DELETE FROM attendance_records WHERE emp_id = ? AND month = ?", new Object[]{dto.getEmpId(), dto.getMonth()});
        if (dto.getDays() == null) {
            return;
        }
        for (AttendanceRecordDTO.DayRecord day : dto.getDays()) {
            String recordId = dto.getEmpId() + "-" + dto.getMonth() + "-" + day.getDayNum();
            this.jdbc.update("INSERT INTO attendance_records (record_id, emp_id, emp_name, department, month, scope, day_num, am_type, pm_type, am_note, pm_note, day_note, employment, salary_status, public_holiday, carry_over, summary_notes, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", new Object[]{recordId, dto.getEmpId(), dto.getEmpName(), dto.getDepartment(), dto.getMonth(), dto.getScope() != null ? dto.getScope() : "full", day.getDayNum(), day.getAmType(), day.getPmType(), day.getAmNote(), day.getPmNote(), day.getDayNote(), dto.getEmployment() != null ? dto.getEmployment() : "全勤在职", dto.getSalaryStatus() != null ? dto.getSalaryStatus() : "未发放", dto.getPublicHoliday() != null ? dto.getPublicHoliday() : 6, dto.getCarryOver() != null ? dto.getCarryOver() : 0, dto.getSummaryNotes(), "Rino"});
        }
    }

    /**
     * 获取某月全员考勤汇总。
     * 门店数据隔离 + emp_id/staff_id 正确关联 staff_master：
     * storeId 非空时，JOIN staff_master 按 store_id 过滤，确保考勤记录归属正确门店。
     *
     * @param month   月份 YYYY-MM
     * @param storeId 当前用户门店ID；null 表示不限制（总经理全局）
     */
    public List<AttendanceRecordDTO.AttendanceSummaryDTO> getSummary(String month, Long storeId) {
        String sql;
        Object[] params;
        if (storeId != null) {
            // JOIN staff_master 确保 emp_id/staff_id 正确关联并按门店过滤
            sql = "SELECT ar.emp_id, ar.emp_name, ar.department, ar.month, ar.employment, ar.salary_status, " +
                    "MAX(ar.total_present) AS total_present, MAX(ar.total_statutory) AS total_statutory, " +
                    "MAX(ar.total_holiday) AS total_holiday, MAX(ar.total_comp) AS total_comp, " +
                    "MAX(ar.total_travel) AS total_travel, MAX(ar.total_overtime) AS total_overtime, " +
                    "MAX(ar.total_leave) AS total_leave, MAX(ar.total_late) AS total_late, " +
                    "MAX(ar.total_early) AS total_early, MAX(ar.total_absent) AS total_absent, " +
                    "MAX(ar.final_balance) AS final_balance " +
                    "FROM attendance_records ar " +
                    "INNER JOIN staff_master sm ON CAST(ar.emp_id AS UNSIGNED) = sm.staff_id " +
                    "WHERE ar.month = ? AND sm.store_id = ? " +
                    "GROUP BY ar.emp_id, ar.emp_name, ar.department, ar.month, ar.employment, ar.salary_status";
            params = new Object[]{month, storeId};
        } else {
            sql = "SELECT emp_id, emp_name, department, month, employment, salary_status, " +
                    "MAX(total_present) AS total_present, MAX(total_statutory) AS total_statutory, " +
                    "MAX(total_holiday) AS total_holiday, MAX(total_comp) AS total_comp, " +
                    "MAX(total_travel) AS total_travel, MAX(total_overtime) AS total_overtime, " +
                    "MAX(total_leave) AS total_leave, MAX(total_late) AS total_late, " +
                    "MAX(total_early) AS total_early, MAX(total_absent) AS total_absent, " +
                    "MAX(final_balance) AS final_balance " +
                    "FROM attendance_records WHERE month = ? " +
                    "GROUP BY emp_id, emp_name, department, month, employment, salary_status";
            params = new Object[]{month};
        }
        List<Map<String, Object>> rows = this.jdbc.queryForList(sql, params);
        ArrayList<AttendanceRecordDTO.AttendanceSummaryDTO> result = new ArrayList<AttendanceRecordDTO.AttendanceSummaryDTO>();
        for (Map r : rows) {
            AttendanceRecordDTO.AttendanceSummaryDTO s = new AttendanceRecordDTO.AttendanceSummaryDTO();
            s.setEmpId(this.asString(r.get("emp_id")));
            s.setEmpName(this.asString(r.get("emp_name")));
            s.setDepartment(this.asString(r.get("department")));
            s.setMonth(this.asString(r.get("month")));
            s.setEmployment(this.asString(r.get("employment")));
            s.setSalaryStatus(this.asString(r.get("salary_status")));
            s.setTotalPresent(this.asBd(r.get("total_present")));
            s.setTotalStatutory(this.asBd(r.get("total_statutory")));
            s.setTotalHoliday(this.asBd(r.get("total_holiday")));
            s.setTotalComp(this.asBd(r.get("total_comp")));
            s.setTotalTravel(this.asBd(r.get("total_travel")));
            s.setTotalOvertime(this.asBd(r.get("total_overtime")));
            s.setTotalLeave(this.asBd(r.get("total_leave")));
            s.setTotalLate(this.asBd(r.get("total_late")));
            s.setTotalEarly(this.asBd(r.get("total_early")));
            s.setTotalAbsent(this.asBd(r.get("total_absent")));
            s.setFinalBalance(this.asBd(r.get("final_balance")));
            result.add(s);
        }
        return result;
    }

    /**
     * 删除单条考勤记录。
     * 门店数据隔离：storeId 非空时，校验记录归属当前用户门店。
     */
    @Transactional
    public void deleteRecord(Integer id, Long storeId) {
        if (storeId != null) {
            int affected = this.jdbc.update(
                    "DELETE FROM attendance_records WHERE id = ? AND EXISTS (" +
                    "  SELECT 1 FROM staff_master sm WHERE CAST(attendance_records.emp_id AS UNSIGNED) = sm.staff_id AND sm.store_id = ?" +
                    ")",
                    id, storeId);
            if (affected == 0) {
                throw new IllegalArgumentException("无权删除非本店员工考勤记录或记录不存在");
            }
        } else {
            this.jdbc.update("DELETE FROM attendance_records WHERE id = ?", new Object[]{id});
        }
    }

    private String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private Integer asInt(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal asBd(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        if (o instanceof BigDecimal) {
            return (BigDecimal) o;
        }
        if (o instanceof Number) {
            return new BigDecimal(o.toString());
        }
        try {
            return new BigDecimal(o.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
