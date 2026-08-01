package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import com.youjian.banquet.service.ApprovalService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hr")
@CrossOrigin(origins = "*")
public class HRController {

    @Autowired private DepartmentRepository deptRepo;
    @Autowired private LeaveRecordRepository leaveRepo;
    @Autowired private ScheduleRepository scheduleRepo;
    @Autowired private OvertimeRepository overtimeRepo;
    @Autowired private EmployeeLifecycleRepository lifecycleRepo;
    @Autowired private AttendanceRepository attendanceRepo;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ApprovalService approvalService;

    // ===== 部门 =====
    @GetMapping("/departments")
    public Result<List<Department>> getDepartments(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            if (effective == null) {
                return Result.success(deptRepo.findAll());
            }
            return Result.success(deptRepo.findByStoreIdOrderBySortOrderAsc(effective));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取部门列表失败: " + e.getMessage());
        }
    }

    // ===== 请假 =====
    @GetMapping("/leave")
    public Result<List<LeaveRecord>> getLeaveList(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            if (effective == null) {
                return Result.success(leaveRepo.findAll());
            }
            return Result.success(leaveRepo.findByStoreIdOrderByCreatedAtDesc(effective));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取请假列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/leave")
    public Result<LeaveRecord> createLeave(@RequestBody LeaveRecord leave) {
        try {
            // 店长仅可操作本店：强制覆盖 storeId
            Long userStore = resolveWriteStoreId();
            if (userStore != null) {
                leave.setStoreId(userStore);
            }
            if (leave.getStatus() == null) {
                leave.setStatus("pending");
            }
            LeaveRecord saved = leaveRepo.save(leave);
            // 自动提交请假审批流（分店单据 -> 店长审批）
            try {
                approvalService.submit("leave", saved.getLeaveId().longValue(),
                        "LV-" + saved.getLeaveId(), saved.getStoreId(),
                        saved.getStaffId(), null);
            } catch (Exception af) {
                // 审批流创建失败不阻断主业务，可后续通过 /api/approval/submit 补提
                af.printStackTrace();
            }
            return Result.success(saved);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建请假失败: " + e.getMessage());
        }
    }

    // ===== 排班 =====
    @GetMapping("/schedule")
    public Result<List<Schedule>> getScheduleList(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            if (effective == null) {
                return Result.success(scheduleRepo.findAll());
            }
            return Result.success(scheduleRepo.findByStoreIdOrderByScheduleDateDesc(effective));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取排班列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/schedule")
    public Result<Schedule> createSchedule(@RequestBody Schedule schedule) {
        try {
            Long userStore = resolveWriteStoreId();
            if (userStore != null) {
                schedule.setStoreId(userStore);
            }
            return Result.success(scheduleRepo.save(schedule));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建排班失败: " + e.getMessage());
        }
    }

    // ===== 加班 =====
    @GetMapping("/overtime")
    public Result<List<Overtime>> getOvertimeList(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            if (effective == null) {
                return Result.success(overtimeRepo.findAll());
            }
            return Result.success(overtimeRepo.findByStoreIdOrderByCreatedAtDesc(effective));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取加班列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/overtime")
    public Result<Overtime> createOvertime(@RequestBody Map<String, Object> body) {
        try {
            Overtime overtime = new Overtime();

            // staffId
            Object staffIdObj = body.get("staffId");
            if (staffIdObj == null) staffIdObj = body.get("staff_id");
            if (staffIdObj != null) overtime.setStaffId(Integer.valueOf(staffIdObj.toString()));

            // storeId
            Object storeIdObj = body.get("storeId");
            if (storeIdObj == null) storeIdObj = body.get("store_id");
            if (storeIdObj != null) overtime.setStoreId(Long.valueOf(storeIdObj.toString()));

            // overtimeDate: 支持 "2026-07-29" 或完整日期时间
            Object dateObj = body.get("overtimeDate");
            if (dateObj == null) dateObj = body.get("overtime_date");
            if (dateObj != null) {
                String dateStr = dateObj.toString();
                if (dateStr.length() > 10) dateStr = dateStr.substring(0, 10);
                overtime.setOvertimeDate(java.time.LocalDate.parse(dateStr));
            }

            // startTime / endTime: 兼容 "18:00"、"18:00:00"、"2026-07-29 18:00" 等格式
            // 实体字段是 LocalDateTime，需要将日期+时间组合
            java.time.LocalDate baseDate = overtime.getOvertimeDate() != null
                ? overtime.getOvertimeDate() : java.time.LocalDate.now();

            overtime.setStartTime(parseDateTime(body.get("startTime"), baseDate));
            overtime.setEndTime(parseDateTime(body.get("endTime"), baseDate));

            // hours
            Object hoursObj = body.get("hours");
            if (hoursObj != null) overtime.setHours(Double.valueOf(hoursObj.toString()));

            // reason
            Object reasonObj = body.get("reason");
            if (reasonObj != null) overtime.setReason(reasonObj.toString());

            // status
            Object statusObj = body.get("status");
            overtime.setStatus(statusObj != null ? statusObj.toString() : "pending");

            Long userStore = resolveWriteStoreId();
            if (userStore != null) {
                overtime.setStoreId(userStore);
            }
            if (overtime.getStatus() == null) {
                overtime.setStatus("pending");
            }
            Overtime saved = overtimeRepo.save(overtime);
            // 自动提交加班审批流（分店单据 -> 店长审批）
            try {
                approvalService.submit("overtime", saved.getOvertimeId().longValue(),
                        "OT-" + saved.getOvertimeId(), saved.getStoreId(),
                        saved.getStaffId(), null);
            } catch (Exception af) {
                // 审批流创建失败不阻断主业务，可后续通过 /api/approval/submit 补提
                af.printStackTrace();
            }
            return Result.success(saved);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建加班失败: " + e.getMessage());
        }
    }

    /**
     * 将时间值解析为 LocalDateTime。
     * 支持格式：
     * - "18:00" / "18:00:00" (仅时间，与 baseDate 组合)
     * - "2026-07-29T18:00" / "2026-07-29 18:00:00" (完整日期时间)
     */
    private java.time.LocalDateTime parseDateTime(Object value, java.time.LocalDate baseDate) {
        if (value == null) return null;
        String s = value.toString().trim();
        if (s.isEmpty()) return null;
        // 完整日期时间格式
        if (s.length() > 8 && (s.contains("-") || s.contains("/"))) {
            try {
                return java.time.LocalDateTime.parse(s.replace(" ", "T"));
            } catch (Exception e) {
                // fall through to time-only parsing
            }
        }
        // 仅时间格式: "18:00" 或 "18:00:00"
        try {
            java.time.LocalTime t;
            if (s.length() == 5) {
                t = java.time.LocalTime.parse(s);
            } else {
                t = java.time.LocalTime.parse(s);
            }
            return java.time.LocalDateTime.of(baseDate, t);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 加班多条件查询：按单号/员工/状态/日期范围检索。
     * GET /api/hr/overtime/search
     * 参数（均可选）：overtimeId(单号), staffId(员工), status(状态),
     *               startDate/endDate(日期范围, yyyy-MM-dd), storeId
     */
    @GetMapping("/overtime/search")
    public Result<List<Map<String, Object>>> searchOvertime(
            @RequestParam(required = false) Integer overtimeId,
            @RequestParam(required = false) Integer staffId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Long storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            StringBuilder sql = new StringBuilder("SELECT * FROM overtime WHERE 1=1");
            List<Object> params = new java.util.ArrayList<>();
            if (effective != null) {
                sql.append(" AND store_id = ?");
                params.add(effective);
            }
            if (overtimeId != null) {
                sql.append(" AND overtime_id = ?");
                params.add(overtimeId);
            }
            if (staffId != null) {
                sql.append(" AND staff_id = ?");
                params.add(staffId);
            }
            if (status != null && !status.isEmpty()) {
                sql.append(" AND status = ?");
                params.add(status);
            }
            if (startDate != null && !startDate.isEmpty()) {
                sql.append(" AND overtime_date >= ?");
                params.add(java.time.LocalDate.parse(startDate));
            }
            if (endDate != null && !endDate.isEmpty()) {
                sql.append(" AND overtime_date <= ?");
                params.add(java.time.LocalDate.parse(endDate));
            }
            sql.append(" ORDER BY create_time DESC, overtime_id DESC");
            List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());
            return Result.success(rows);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "查询加班列表失败: " + e.getMessage());
        }
    }

    // ===== 员工生命周期 =====
    @GetMapping("/lifecycle")
    public Result<List<EmployeeLifecycle>> getLifecycle(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            if (effective == null) {
                return Result.success(lifecycleRepo.findAll());
            }
            // EmployeeLifecycle 实体未映射 store_id，使用 JdbcTemplate 按门店过滤
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT * FROM employee_lifecycle WHERE store_id = ? ORDER BY event_date DESC", effective);
            return Result.success(rows.stream().map(this::mapLifecycle).toList());
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取生命周期失败: " + e.getMessage());
        }
    }

    // ===== 考勤 =====
    @GetMapping("/attendance")
    public Result<List<Attendance>> getAttendance(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            if (effective == null) {
                return Result.success(attendanceRepo.findAll());
            }
            return Result.success(attendanceRepo.findByStoreIdOrderByAttendanceDateDesc(effective));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取考勤失败: " + e.getMessage());
        }
    }

    @PostMapping("/attendance")
    public Result<Attendance> createAttendance(@RequestBody Attendance attendance) {
        try {
            Long userStore = resolveWriteStoreId();
            if (userStore != null) {
                attendance.setStoreId(userStore);
            } else if (attendance.getStoreId() == null) {
                attendance.setStoreId(1L);
            }
            return Result.success(attendanceRepo.save(attendance));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建考勤失败: " + e.getMessage());
        }
    }

    // ======== 门店数据隔离辅助方法 ========

    /**
     * 解析当前请求用户的 staffId。
     * GET 请求由 StoreDataScopeAspect 写入 UserContext；POST/PUT/DELETE 回退到 JwtAuthInterceptor 设置的请求属性。
     */
    private Long getCurrentStaffId() {
        Long staffId = UserContext.getStaffId();
        if (staffId != null) return staffId;
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                Object sid = sra.getRequest().getAttribute("jwt_staff_id");
                if (sid instanceof Long) return (Long) sid;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 查询接口：解析有效门店ID。
     * @param requestStoreId 前端传入的 storeId
     * @return null=全局（总经理，可查所有门店）；非null=限制到指定门店（店长仅本店）
     * @throws SecurityException 未登录时抛出
     */
    private Long resolveQueryStoreId(Long requestStoreId) {
        Long currentStaffId = getCurrentStaffId();
        if (currentStaffId == null) {
            throw new SecurityException("未登录，无权访问HR数据");
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT store_id, can_view_all_stores FROM staff_master WHERE staff_id = ? LIMIT 1",
                currentStaffId.intValue());
        if (rows.isEmpty()) {
            throw new SecurityException("无权访问HR数据");
        }
        Map<String, Object> row = rows.get(0);
        int canViewAllStores = row.get("can_view_all_stores") == null ? 0 : ((Number) row.get("can_view_all_stores")).intValue();
        Long userStoreId = row.get("store_id") == null ? null : ((Number) row.get("store_id")).longValue();
        boolean isAllStores = UserContext.isDataScopeAll() || canViewAllStores == 1;
        if (isAllStores) {
            return null; // 总经理：可查所有门店
        }
        // 店长：仅本店，忽略前端传入的 storeId
        return userStoreId != null ? userStoreId : requestStoreId;
    }

    /**
     * 写操作：解析当前用户门店ID（店长仅本店）。
     * @return 店长门店ID；null=总经理（允许跨门店操作，如调动员工）
     * @throws SecurityException 未登录时抛出
     */
    private Long resolveWriteStoreId() {
        Long currentStaffId = getCurrentStaffId();
        if (currentStaffId == null) {
            throw new SecurityException("未登录，无权操作HR数据");
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT store_id, can_view_all_stores FROM staff_master WHERE staff_id = ? LIMIT 1",
                currentStaffId.intValue());
        if (rows.isEmpty()) {
            throw new SecurityException("无权操作HR数据");
        }
        Map<String, Object> row = rows.get(0);
        int canViewAllStores = row.get("can_view_all_stores") == null ? 0 : ((Number) row.get("can_view_all_stores")).intValue();
        Long userStoreId = row.get("store_id") == null ? null : ((Number) row.get("store_id")).longValue();
        boolean isAllStores = UserContext.isDataScopeAll() || canViewAllStores == 1;
        if (isAllStores) {
            return null; // 总经理：允许跨门店操作
        }
        return userStoreId; // 店长：强制本店
    }

    /** 将 JDBC 行映射为 EmployeeLifecycle 实体 */
    private EmployeeLifecycle mapLifecycle(Map<String, Object> r) {
        EmployeeLifecycle e = new EmployeeLifecycle();
        e.setId(r.get("id") == null ? null : ((Number) r.get("id")).intValue());
        e.setEmpId(r.get("emp_id") == null ? null : r.get("emp_id").toString());
        e.setEmpName(r.get("emp_name") == null ? null : r.get("emp_name").toString());
        e.setEventType(r.get("event_type") == null ? null : r.get("event_type").toString());
        e.setEventDate(r.get("event_date") == null ? null : java.sql.Date.valueOf(r.get("event_date").toString()).toLocalDate());
        e.setCreatedAt(r.get("created_at") == null ? null : java.sql.Timestamp.valueOf(r.get("created_at").toString()).toLocalDateTime());
        return e;
    }
}
