package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.Attendance;
import com.youjian.banquet.repository.AttendanceRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简单每日考勤记录（Attendance.vue 用）。
 * <p>注意：与 AttendanceRecordController（/api/hr/attendance/record，月度批量打卡表格用）
 * 是两套完全不同的数据模型——那套服务 AttendanceCalendar.vue 的整月网格录入，这套服务
 * 本页面"一人一天一行"的简单列表增删改。此前这里对应的 AttendanceRepository 一直存在，
 * 但从没有 Controller 挂上去，前端调用了这么久其实一直是 404。
 */
@RestController
@RequestMapping("/api/hr/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    @Autowired
    private AttendanceRepository attendanceRepo;

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        Long storeId = resolveStoreId();
        try {
            String sql = "SELECT a.attendance_id AS id, s.staff_name AS staffName, s.department AS department, " +
                    "a.attendance_date AS date, a.clock_in AS checkIn, a.clock_out AS checkOut, a.status AS status, " +
                    "a.late_minutes AS lateMinutes, a.early_leave_minutes AS earlyMinutes, a.remark AS remark " +
                    "FROM attendance a LEFT JOIN staff_master s ON s.staff_id = a.staff_id " +
                    (storeId != null ? "WHERE a.store_id = ? " : "") +
                    "ORDER BY a.attendance_date DESC, a.attendance_id DESC";
            List<Map<String, Object>> rows = storeId != null
                    ? jdbc.queryForList(sql, storeId)
                    : jdbc.queryForList(sql);
            return Result.success(rows);
        } catch (Exception e) {
            return Result.error(500, "获取考勤记录失败：" + e.getMessage());
        }
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Long storeId = resolveStoreId();
        if (storeId == null) storeId = 1L;
        try {
            Integer staffId = resolveStaffId((String) body.get("staffName"), storeId);
            if (staffId == null) return Result.error(400, "找不到员工：" + body.get("staffName"));

            Attendance a = new Attendance();
            a.setStoreId(storeId);
            a.setStaffId(staffId);
            fillFromBody(a, body);
            Attendance saved = attendanceRepo.save(a);
            Map<String, Object> data = new HashMap<>();
            data.put("id", saved.getAttendanceId());
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "创建考勤记录失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Long storeId = resolveStoreId();
        try {
            Attendance a = attendanceRepo.findById(id).orElse(null);
            if (a == null) return Result.error(404, "考勤记录不存在");
            if (storeId != null && a.getStoreId() != null) {
                UserContext.assertStoreAccess(a.getStoreId());
            }
            if (body.get("staffName") != null) {
                Integer staffId = resolveStaffId((String) body.get("staffName"), a.getStoreId());
                if (staffId != null) a.setStaffId(staffId);
            }
            fillFromBody(a, body);
            attendanceRepo.save(a);
            return Result.success(null);
        } catch (IllegalArgumentException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新考勤记录失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        try {
            Attendance a = attendanceRepo.findById(id).orElse(null);
            if (a == null) return Result.error(404, "考勤记录不存在");
            if (a.getStoreId() != null) {
                UserContext.assertStoreAccess(a.getStoreId());
            }
            attendanceRepo.delete(a);
            return Result.success(null);
        } catch (IllegalArgumentException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除考勤记录失败：" + e.getMessage());
        }
    }

    private void fillFromBody(Attendance a, Map<String, Object> body) {
        if (body.get("date") != null) a.setAttendanceDate(LocalDate.parse(body.get("date").toString()));
        if (body.get("checkIn") != null && !body.get("checkIn").toString().isEmpty()) {
            a.setClockIn(LocalDateTime.of(a.getAttendanceDate(), LocalTime.parse(body.get("checkIn").toString())));
        }
        if (body.get("checkOut") != null && !body.get("checkOut").toString().isEmpty()) {
            a.setClockOut(LocalDateTime.of(a.getAttendanceDate(), LocalTime.parse(body.get("checkOut").toString())));
        }
        if (body.get("status") != null) a.setStatus(body.get("status").toString());
        if (body.get("lateMinutes") != null) a.setLateMinutes(Integer.valueOf(body.get("lateMinutes").toString()));
        if (body.get("earlyMinutes") != null) a.setEarlyLeaveMinutes(Integer.valueOf(body.get("earlyMinutes").toString()));
        if (body.get("remark") != null) a.setRemark(body.get("remark").toString());
    }

    private Integer resolveStaffId(String staffName, Long storeId) {
        if (staffName == null || staffName.isBlank()) return null;
        try {
            List<Map<String, Object>> rows = storeId != null
                    ? jdbc.queryForList("SELECT staff_id FROM staff_master WHERE staff_name = ? AND store_id = ? LIMIT 1", staffName, storeId)
                    : jdbc.queryForList("SELECT staff_id FROM staff_master WHERE staff_name = ? LIMIT 1", staffName);
            if (rows.isEmpty()) return null;
            return ((Number) rows.get(0).get("staff_id")).intValue();
        } catch (Exception e) {
            return null;
        }
    }

    private Long resolveStoreId() {
        if (UserContext.isDataScopeAll()) return null;
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? null : sid;
    }
}
