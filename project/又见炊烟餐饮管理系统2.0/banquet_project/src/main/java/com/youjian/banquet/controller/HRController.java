package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import com.youjian.banquet.service.ApprovalService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

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
    @Autowired private AttendanceRecordRepository attendanceRecordRepo;
    @Autowired private StaffMasterRepository staffRepo;
    @Autowired private SalaryRepository salaryRepo;
    @Autowired private SalaryDeductRepository salaryDeductRepo;
    @Autowired private SocInsuranceRepository socInsuranceRepo;
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

    @PostMapping("/departments")
    public Result<Department> createDepartment(@RequestBody Department dept) {
        try {
            Long userStore = resolveWriteStoreId();
            if (userStore != null) {
                dept.setStoreId(userStore);
            } else if (dept.getStoreId() == null) {
                dept.setStoreId(1L);
            }
            return Result.success(deptRepo.save(dept));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建部门失败: " + e.getMessage());
        }
    }

    @PutMapping("/departments/{id}")
    public Result<Department> updateDepartment(@PathVariable Integer id, @RequestBody Department dept) {
        try {
            Department existing = deptRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "部门不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权编辑非本店部门");
            }
            if (dept.getDeptName() != null) existing.setDeptName(dept.getDeptName());
            if (dept.getDeptCode() != null) existing.setDeptCode(dept.getDeptCode());
            if (dept.getParentId() != null) existing.setParentId(dept.getParentId());
            if (dept.getSortOrder() != null) existing.setSortOrder(dept.getSortOrder());
            if (dept.getStatus() != null) existing.setStatus(dept.getStatus());
            if (dept.getDescription() != null) existing.setDescription(dept.getDescription());
            if (dept.getMorStartTime() != null) existing.setMorStartTime(dept.getMorStartTime());
            if (dept.getMorEndTime() != null) existing.setMorEndTime(dept.getMorEndTime());
            if (dept.getAftStartTime() != null) existing.setAftStartTime(dept.getAftStartTime());
            if (dept.getAftEndTime() != null) existing.setAftEndTime(dept.getAftEndTime());
            if (dept.getTotalWorkHours() != null) existing.setTotalWorkHours(dept.getTotalWorkHours());
            existing.setUpdatedAt(null);
            return Result.success(deptRepo.save(existing));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新部门失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/departments/{id}")
    public Result<?> deleteDepartment(@PathVariable Integer id) {
        try {
            Department existing = deptRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "部门不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权删除非本店部门");
            }
            deptRepo.delete(existing);
            return Result.success("部门已删除");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除部门失败: " + e.getMessage());
        }
    }

    // ===== 请假 =====
    @GetMapping("/leave")
    public Result<List<LeaveRecord>> getLeaveList(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            List<LeaveRecord> list;
            if (effective == null) {
                list = leaveRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            } else {
                list = leaveRepo.findByStoreIdOrderByCreatedAtDesc(effective);
            }
            list.forEach(l -> {
                if (l.getStaffId() != null) {
                    staffRepo.findById(l.getStaffId()).ifPresent(s -> {
                        try {
                            var field = LeaveRecord.class.getDeclaredField("staffName");
                            field.setAccessible(true);
                            field.set(l, s.getStaffName());
                        } catch (Exception ignored) {}
                        try {
                            var field = LeaveRecord.class.getDeclaredField("department");
                            field.setAccessible(true);
                            field.set(l, s.getDepartment());
                        } catch (Exception ignored) {}
                    });
                }
            });
            return Result.success(list);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取请假列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/leave")
    public Result<LeaveRecord> createLeave(@RequestBody LeaveRecord leave) {
        try {
            Long userStore = resolveWriteStoreId();
            if (userStore != null) {
                leave.setStoreId(userStore);
            }
            if (leave.getStatus() == null) {
                leave.setStatus("pending");
            }
            LeaveRecord saved = leaveRepo.save(leave);
            try {
                approvalService.submit("leave", saved.getLeaveId().longValue(),
                        "LV-" + saved.getLeaveId(), saved.getStoreId(),
                        saved.getStaffId(), null);
            } catch (Exception af) {
                af.printStackTrace();
            }
            return Result.success(saved);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建请假失败: " + e.getMessage());
        }
    }

    @PutMapping("/leave/{id}")
    public Result<LeaveRecord> updateLeave(@PathVariable Integer id, @RequestBody LeaveRecord leave) {
        try {
            LeaveRecord existing = leaveRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "请假记录不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权编辑非本店请假记录");
            }
            if (leave.getStaffId() != null) existing.setStaffId(leave.getStaffId());
            if (leave.getLeaveType() != null) existing.setLeaveType(leave.getLeaveType());
            if (leave.getStartDate() != null) existing.setStartDate(leave.getStartDate());
            if (leave.getEndDate() != null) existing.setEndDate(leave.getEndDate());
            if (leave.getDays() != null) existing.setDays(leave.getDays());
            if (leave.getStatus() != null) existing.setStatus(leave.getStatus());
            if (leave.getReason() != null) existing.setReason(leave.getReason());
            existing.setUpdatedAt(null);
            return Result.success(leaveRepo.save(existing));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新请假失败: " + e.getMessage());
        }
    }

    @PutMapping("/leave/{id}/approve")
    public Result<LeaveRecord> approveLeave(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            LeaveRecord existing = leaveRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "请假记录不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权审批非本店请假记录");
            }
            String status = body.get("status") != null ? body.get("status").toString() : "approved";
            String remark = body.get("remark") != null ? body.get("remark").toString() : "";
            Long currentStaffId = getCurrentStaffId();
            existing.setStatus(status);
            existing.setApproverId(currentStaffId != null ? currentStaffId.intValue() : null);
            existing.setApproveTime(LocalDateTime.now());
            existing.setApproveRemark(remark);
            existing.setUpdatedAt(null);
            return Result.success(leaveRepo.save(existing));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "审批请假失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/leave/{id}")
    public Result<?> deleteLeave(@PathVariable Integer id) {
        try {
            LeaveRecord existing = leaveRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "请假记录不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权删除非本店请假记录");
            }
            leaveRepo.delete(existing);
            return Result.success("请假记录已删除");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除请假失败: " + e.getMessage());
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

    @PutMapping("/schedule/{id}")
    public Result<Schedule> updateSchedule(@PathVariable Integer id, @RequestBody Schedule schedule) {
        try {
            Schedule existing = scheduleRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "排班不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权编辑非本店排班");
            }
            if (schedule.getStaffId() != null) existing.setStaffId(schedule.getStaffId());
            if (schedule.getScheduleDate() != null) existing.setScheduleDate(schedule.getScheduleDate());
            if (schedule.getShiftType() != null) existing.setShiftType(schedule.getShiftType());
            if (schedule.getStartTime() != null) existing.setStartTime(schedule.getStartTime());
            if (schedule.getEndTime() != null) existing.setEndTime(schedule.getEndTime());
            if (schedule.getRemark() != null) existing.setRemark(schedule.getRemark());
            return Result.success(scheduleRepo.save(existing));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新排班失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/schedule/{id}")
    public Result<?> deleteSchedule(@PathVariable Integer id) {
        try {
            Schedule existing = scheduleRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "排班不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权删除非本店排班");
            }
            scheduleRepo.delete(existing);
            return Result.success("排班已删除");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除排班失败: " + e.getMessage());
        }
    }

    // ===== 加班 =====
    @GetMapping("/overtime")
    public Result<List<Overtime>> getOvertimeList(@RequestParam(defaultValue = "1") Long storeId,
                                                  @RequestParam(required = false) Integer staffId,
                                                  @RequestParam(required = false) String status) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            List<Overtime> list;
            if (effective == null) {
                list = overtimeRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            } else {
                list = overtimeRepo.findByStoreIdOrderByCreatedAtDesc(effective);
            }
            if (staffId != null) {
                list = list.stream().filter(o -> staffId.equals(o.getStaffId())).toList();
            }
            if (status != null && !status.isEmpty()) {
                list = list.stream().filter(o -> status.equals(o.getStatus())).toList();
            }
            List<Overtime> finalList = list;
            list.forEach(o -> {
                if (o.getStaffId() != null) {
                    staffRepo.findById(o.getStaffId()).ifPresent(s -> {
                        try {
                            var nameField = Overtime.class.getDeclaredField("staffName");
                            nameField.setAccessible(true);
                            nameField.set(o, s.getStaffName());
                            var deptField = Overtime.class.getDeclaredField("department");
                            deptField.setAccessible(true);
                            deptField.set(o, s.getDepartment());
                        } catch (Exception ignored) {}
                    });
                }
            });
            return Result.success(finalList);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取加班列表失败: " + e.getMessage());
        }
    }

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
            List<Object> params = new ArrayList<>();
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
                params.add(LocalDate.parse(startDate));
            }
            if (endDate != null && !endDate.isEmpty()) {
                sql.append(" AND overtime_date <= ?");
                params.add(LocalDate.parse(endDate));
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

    @PostMapping("/overtime")
    public Result<Overtime> createOvertime(@RequestBody Overtime overtime) {
        try {
            Long userStore = resolveWriteStoreId();
            if (userStore != null) {
                overtime.setStoreId(userStore);
            }
            if (overtime.getStatus() == null) {
                overtime.setStatus("pending");
            }
            Overtime saved = overtimeRepo.save(overtime);
            try {
                approvalService.submit("overtime", saved.getOvertimeId().longValue(),
                        "OT-" + saved.getOvertimeId(), saved.getStoreId(),
                        saved.getStaffId(), null);
            } catch (Exception af) {
                af.printStackTrace();
            }
            return Result.success(saved);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建加班失败: " + e.getMessage());
        }
    }

    @PutMapping("/overtime/{id}")
    public Result<Overtime> updateOvertime(@PathVariable Integer id, @RequestBody Overtime overtime) {
        try {
            Overtime existing = overtimeRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "加班记录不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权编辑非本店加班记录");
            }
            if (overtime.getStaffId() != null) existing.setStaffId(overtime.getStaffId());
            if (overtime.getOvertimeDate() != null) existing.setOvertimeDate(overtime.getOvertimeDate());
            if (overtime.getStartTime() != null) existing.setStartTime(overtime.getStartTime());
            if (overtime.getEndTime() != null) existing.setEndTime(overtime.getEndTime());
            if (overtime.getHours() != null) existing.setHours(overtime.getHours());
            if (overtime.getSalaryMultiple() != null) existing.setSalaryMultiple(overtime.getSalaryMultiple());
            if (overtime.getOvertimeBonus() != null) existing.setOvertimeBonus(overtime.getOvertimeBonus());
            if (overtime.getMakeUp() != null) existing.setMakeUp(overtime.getMakeUp());
            if (overtime.getStatus() != null) existing.setStatus(overtime.getStatus());
            if (overtime.getReason() != null) existing.setReason(overtime.getReason());
            existing.setUpdatedAt(null);
            return Result.success(overtimeRepo.save(existing));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新加班失败: " + e.getMessage());
        }
    }

    @PutMapping("/overtime/{id}/approve")
    public Result<Overtime> approveOvertime(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            Overtime existing = overtimeRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "加班记录不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权审批非本店加班记录");
            }
            String status = body.get("status") != null ? body.get("status").toString() : "approved";
            String remark = body.get("remark") != null ? body.get("remark").toString() : "";
            Long currentStaffId = getCurrentStaffId();
            existing.setStatus(status);
            existing.setApproverId(currentStaffId != null ? currentStaffId.intValue() : null);
            existing.setApproveTime(LocalDateTime.now());
            existing.setApproveRemark(remark);
            existing.setUpdatedAt(null);
            return Result.success(overtimeRepo.save(existing));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "审批加班失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/overtime/{id}")
    public Result<?> deleteOvertime(@PathVariable Integer id) {
        try {
            Overtime existing = overtimeRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "加班记录不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权删除非本店加班记录");
            }
            overtimeRepo.delete(existing);
            return Result.success("加班记录已删除");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除加班失败: " + e.getMessage());
        }
    }

    // ===== 考勤（Attendance.vue 使用的简单 CRUD 接口） =====
    @GetMapping("/attendance")
    public Result<List<Map<String, Object>>> getAttendanceList(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            StringBuilder sql = new StringBuilder("SELECT a.id, a.staff_id as staffId, a.staff_name as staffName, " +
                    "a.emp_name as empName, a.department, a.attendance_date as date, " +
                    "a.status, a.clock_in as checkIn, a.clock_out as checkOut, " +
                    "a.late_minutes as lateMinutes, a.early_minutes as earlyMinutes, " +
                    "a.remark, a.store_id as storeId FROM attendance a WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (effective != null) {
                sql.append(" AND a.store_id = ?");
                params.add(effective);
            }
            sql.append(" ORDER BY a.attendance_date DESC, a.id DESC LIMIT 500");
            return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取考勤列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/attendance")
    public Result<Map<String, Object>> createAttendance(@RequestBody Map<String, Object> body) {
        try {
            Long userStore = resolveWriteStoreId();
            Long storeId = userStore != null ? userStore : (body.get("storeId") != null ? Long.valueOf(body.get("storeId").toString()) : 1L);
            String staffName = body.get("staffName") != null ? body.get("staffName").toString() : "";
            String department = body.get("department") != null ? body.get("department").toString() : "";
            String date = body.get("date") != null ? body.get("date").toString() : LocalDate.now().toString();
            String checkIn = body.get("checkIn") != null ? body.get("checkIn").toString() : null;
            String checkOut = body.get("checkOut") != null ? body.get("checkOut").toString() : null;
            String status = body.get("status") != null ? body.get("status").toString() : "normal";
            Integer lateMinutes = body.get("lateMinutes") != null ? Integer.valueOf(body.get("lateMinutes").toString()) : 0;
            Integer earlyMinutes = body.get("earlyMinutes") != null ? Integer.valueOf(body.get("earlyMinutes").toString()) : 0;
            String remark = body.get("remark") != null ? body.get("remark").toString() : "";
            Integer staffId = null;
            if (!staffName.isEmpty()) {
                List<Map<String, Object>> rows = jdbc.queryForList(
                        "SELECT staff_id FROM staff_master WHERE staff_name = ? AND (store_id = ? OR store_id IS NULL) LIMIT 1",
                        staffName, storeId);
                if (!rows.isEmpty()) {
                    staffId = ((Number) rows.get(0).get("staff_id")).intValue();
                }
            }
            jdbc.update("INSERT INTO attendance (store_id, staff_id, staff_name, department, attendance_date, " +
                            "clock_in, clock_out, status, late_minutes, early_minutes, remark, create_time) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                    storeId, staffId, staffName, department, date,
                    checkIn, checkOut, status, lateMinutes, earlyMinutes, remark);
            return Result.success(Map.of("message", "考勤记录已创建"));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建考勤失败: " + e.getMessage());
        }
    }

    @PutMapping("/attendance/{id}")
    public Result<?> updateAttendance(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            Map<String, Object> existing = jdbc.queryForList("SELECT * FROM attendance WHERE id = ?", id)
                    .stream().findFirst().orElse(null);
            if (existing == null) return Result.error(404, "考勤记录不存在");
            Long userStore = resolveWriteStoreId();
            Long rowStoreId = existing.get("store_id") != null ? ((Number) existing.get("store_id")).longValue() : null;
            if (userStore != null && (rowStoreId == null || !userStore.equals(rowStoreId))) {
                return Result.error(403, "无权编辑非本店考勤记录");
            }
            StringBuilder sql = new StringBuilder("UPDATE attendance SET ");
            List<Object> params = new ArrayList<>();
            String[] fields = {"staffName", "department", "date", "checkIn", "checkOut",
                    "status", "lateMinutes", "earlyMinutes", "remark"};
            String[] dbCols = {"staff_name", "department", "attendance_date", "clock_in", "clock_out",
                    "status", "late_minutes", "early_minutes", "remark"};
            for (int i = 0; i < fields.length; i++) {
                if (body.containsKey(fields[i])) {
                    if (!params.isEmpty()) sql.append(", ");
                    sql.append(dbCols[i]).append(" = ?");
                    params.add(body.get(fields[i]));
                }
            }
            sql.append(" WHERE id = ?");
            params.add(id);
            jdbc.update(sql.toString(), params.toArray());
            return Result.success("考勤记录已更新");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新考勤失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/attendance/{id}")
    public Result<?> deleteAttendance(@PathVariable Integer id) {
        try {
            Map<String, Object> existing = jdbc.queryForList("SELECT * FROM attendance WHERE id = ?", id)
                    .stream().findFirst().orElse(null);
            if (existing == null) return Result.error(404, "考勤记录不存在");
            Long userStore = resolveWriteStoreId();
            Long rowStoreId = existing.get("store_id") != null ? ((Number) existing.get("store_id")).longValue() : null;
            if (userStore != null && (rowStoreId == null || !userStore.equals(rowStoreId))) {
                return Result.error(403, "无权删除非本店考勤记录");
            }
            jdbc.update("DELETE FROM attendance WHERE id = ?", id);
            return Result.success("考勤记录已删除");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除考勤失败: " + e.getMessage());
        }
    }

    // ===== 考勤日历（AttendanceCalendar.vue 使用） =====
    @GetMapping("/attendance/record")
    public Result<Map<String, Object>> getAttendanceCalendarRecord(
            @RequestParam String empId,
            @RequestParam String month) {
        try {
            Long effective = resolveQueryStoreId(1L);
            String storeFilter = effective != null ? " AND store_id = " + effective : "";
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT * FROM attendance_records WHERE emp_id = ? AND month = ? " + storeFilter + " LIMIT 1",
                    empId, month);
            if (rows.isEmpty()) {
                return Result.error(404, "未找到考勤记录");
            }
            Map<String, Object> r = rows.get(0);
            List<Map<String, Object>> days = new ArrayList<>();
            String scope = (String) r.get("scope");
            if (scope != null && !scope.isEmpty()) {
                String[] perDay = scope.split("\\|");
                int dayNum = 1;
                for (String dayScope : perDay) {
                    if (dayScope.length() >= 2) {
                        days.add(Map.of(
                                "dayNum", dayNum,
                                "amType", mapLegacyType(String.valueOf(dayScope.charAt(0))),
                                "pmType", mapLegacyType(String.valueOf(dayScope.charAt(1)))
                        ));
                    }
                    dayNum++;
                }
            }
            Map<String, Object> result = new HashMap<>();
            result.put("days", days);
            result.put("publicHoliday", r.get("public_holiday") != null ? ((Number) r.get("public_holiday")).intValue() : 0);
            result.put("carryOver", r.get("carry_over") != null ? ((Number) r.get("carry_over")).intValue() : 0);
            result.put("employment", r.get("employment") != null ? r.get("employment").toString() : "全勤在职");
            result.put("salaryStatus", r.get("salary_status") != null ? r.get("salary_status").toString() : "未发放");
            result.put("finalBalance", r.get("final_balance") != null ? ((Number) r.get("final_balance")).doubleValue() : 0);
            result.put("recordedDays", r.get("recorded_days") != null ? ((Number) r.get("recorded_days")).intValue() : 0);
            return Result.success(result);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取考勤记录失败: " + e.getMessage());
        }
    }

    private String mapLegacyType(String c) {
        return switch (c) {
            case "1", "√" -> "present";
            case "2" -> "leave";
            case "3" -> "late";
            case "4" -> "absent";
            case "5" -> "holiday";
            case "6" -> "overtime";
            case "7" -> "comp";
            case "8" -> "travel";
            case "9" -> "early";
            case "0" -> "statutory";
            default -> null;
        };
    }

    private String mapToLegacyType(String type) {
        return switch (type) {
            case "present" -> "1";
            case "leave" -> "2";
            case "late" -> "3";
            case "absent" -> "4";
            case "holiday" -> "5";
            case "overtime" -> "6";
            case "comp" -> "7";
            case "travel" -> "8";
            case "early" -> "9";
            case "statutory" -> "0";
            default -> "-";
        };
    }

    @PostMapping("/attendance/record")
    @Transactional
    public Result<?> saveAttendanceCalendarRecord(@RequestBody Map<String, Object> body) {
        try {
            Long userStore = resolveWriteStoreId();
            Long storeId = userStore != null ? userStore : 1L;
            String empId = body.get("empId") != null ? body.get("empId").toString() : "";
            String empName = body.get("empName") != null ? body.get("empName").toString() : "";
            String department = body.get("department") != null ? body.get("department").toString() : "";
            String month = body.get("month") != null ? body.get("month").toString() : "";
            String employment = body.get("employment") != null ? body.get("employment").toString() : "全勤在职";
            String salaryStatus = body.get("salaryStatus") != null ? body.get("salaryStatus").toString() : "未发放";
            Integer publicHoliday = body.get("publicHoliday") != null ? Integer.valueOf(body.get("publicHoliday").toString()) : 0;
            Integer carryOver = body.get("carryOver") != null ? Integer.valueOf(body.get("carryOver").toString()) : 0;
            Double finalBalance = body.get("finalBalance") != null ? Double.valueOf(body.get("finalBalance").toString()) : 0;
            Integer recordedDays = body.get("recordedDays") != null ? Integer.valueOf(body.get("recordedDays").toString()) : 0;
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> days = (List<Map<String, Object>>) body.get("days");
            Integer staffId = null;
            if (!empId.isEmpty()) {
                try {
                    List<Map<String, Object>> rows = jdbc.queryForList(
                            "SELECT staff_id FROM staff_master WHERE staff_id = ? OR staff_account = ? LIMIT 1",
                            Integer.parseInt(empId), empId);
                    if (!rows.isEmpty()) staffId = ((Number) rows.get(0).get("staff_id")).intValue();
                } catch (NumberFormatException ignored) {}
            }
            StringBuilder scopeSb = new StringBuilder();
            BigDecimal totalPresent = BigDecimal.ZERO, totalStatutory = BigDecimal.ZERO, totalHoliday = BigDecimal.ZERO;
            BigDecimal totalComp = BigDecimal.ZERO, totalTravel = BigDecimal.ZERO, totalOvertime = BigDecimal.ZERO;
            BigDecimal totalLeave = BigDecimal.ZERO, totalLate = BigDecimal.ZERO, totalEarly = BigDecimal.ZERO, totalAbsent = BigDecimal.ZERO;
            int maxDay = 0;
            if (days != null) {
                Map<Integer, Map<String, Object>> dayMap = new HashMap<>();
                days.forEach(d -> {
                    if (d.get("dayNum") != null) dayMap.put(Integer.valueOf(d.get("dayNum").toString()), d);
                });
                maxDay = dayMap.keySet().stream().max(Integer::compareTo).orElse(0);
                for (int d = 1; d <= maxDay; d++) {
                    Map<String, Object> rec = dayMap.get(d);
                    if (rec == null) {
                        scopeSb.append("--");
                        continue;
                    }
                    String amType = rec.get("amType") != null ? rec.get("amType").toString() : null;
                    String pmType = rec.get("pmType") != null ? rec.get("pmType").toString() : null;
                    scopeSb.append(mapToLegacyType(amType != null ? amType : "-"))
                            .append(mapToLegacyType(pmType != null ? pmType : "-"));
                    if ("present".equals(amType)) totalPresent = totalPresent.add(BigDecimal.valueOf(0.5));
                    if ("present".equals(pmType)) totalPresent = totalPresent.add(BigDecimal.valueOf(0.5));
                    if ("statutory".equals(amType)) totalStatutory = totalStatutory.add(BigDecimal.valueOf(0.5));
                    if ("statutory".equals(pmType)) totalStatutory = totalStatutory.add(BigDecimal.valueOf(0.5));
                    if ("holiday".equals(amType)) totalHoliday = totalHoliday.add(BigDecimal.valueOf(0.5));
                    if ("holiday".equals(pmType)) totalHoliday = totalHoliday.add(BigDecimal.valueOf(0.5));
                    if ("comp".equals(amType)) totalComp = totalComp.add(BigDecimal.valueOf(0.5));
                    if ("comp".equals(pmType)) totalComp = totalComp.add(BigDecimal.valueOf(0.5));
                    if ("travel".equals(amType)) totalTravel = totalTravel.add(BigDecimal.valueOf(0.5));
                    if ("travel".equals(pmType)) totalTravel = totalTravel.add(BigDecimal.valueOf(0.5));
                    if ("overtime".equals(amType)) totalOvertime = totalOvertime.add(BigDecimal.valueOf(0.5));
                    if ("overtime".equals(pmType)) totalOvertime = totalOvertime.add(BigDecimal.valueOf(0.5));
                    if ("leave".equals(amType)) totalLeave = totalLeave.add(BigDecimal.valueOf(0.5));
                    if ("leave".equals(pmType)) totalLeave = totalLeave.add(BigDecimal.valueOf(0.5));
                    if ("late".equals(amType)) totalLate = totalLate.add(BigDecimal.valueOf(0.5));
                    if ("late".equals(pmType)) totalLate = totalLate.add(BigDecimal.valueOf(0.5));
                    if ("early".equals(amType)) totalEarly = totalEarly.add(BigDecimal.valueOf(0.5));
                    if ("early".equals(pmType)) totalEarly = totalEarly.add(BigDecimal.valueOf(0.5));
                    if ("absent".equals(amType)) totalAbsent = totalAbsent.add(BigDecimal.valueOf(0.5));
                    if ("absent".equals(pmType)) totalAbsent = totalAbsent.add(BigDecimal.valueOf(0.5));
                }
            }
            jdbc.update("DELETE FROM attendance_records WHERE emp_id = ? AND month = ? AND (store_id = ? OR store_id IS NULL)",
                    empId, month, storeId);
            String recordId = "AR-" + empId + "-" + month;
            jdbc.update("INSERT INTO attendance_records (record_id, emp_id, emp_name, staff_id, staff_name, department, " +
                            "month, scope, day_num, public_holiday, carry_over, employment, salary_status, " +
                            "total_present, total_statutory, total_holiday, total_comp, total_travel, total_overtime, " +
                            "total_leave, total_late, total_early, total_absent, final_balance, recorded_days, store_id, created_at, staff_name) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?)",
                    recordId, empId, empName, staffId, empName, department,
                    month, scopeSb.toString(), maxDay, publicHoliday, carryOver, employment, salaryStatus,
                    totalPresent, totalStatutory, totalHoliday, totalComp, totalTravel, totalOvertime,
                    totalLeave, totalLate, totalEarly, totalAbsent, finalBalance, recordedDays, storeId, empName);
            return Result.success(Map.of("recordId", recordId, "message", "保存成功"));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "保存考勤记录失败: " + e.getMessage());
        }
    }

    // ===== 工资管理 =====
    @GetMapping("/salary")
    public Result<List<Map<String, Object>>> getSalaryList(@RequestParam(defaultValue = "1") Long storeId,
                                                           @RequestParam(required = false) String month,
                                                           @RequestParam(required = false) Integer deptId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            StringBuilder sql = new StringBuilder("SELECT s.id, s.staff_id as staffId, s.month, " +
                    "s.base_salary as baseSalary, s.overtime_salary as overtimeSalary, s.subsidy, s.bonus, " +
                    "s.late_deduct as lateDeduct, s.leave_deduct as leaveDeduct, " +
                    "s.leave_early_deduct as leaveEarlyDeduct, s.absenteeism_deduct as absenteeismDeduct, " +
                    "s.total_salary as totalSalary, s.remark, s.store_id as storeId, " +
                    "sm.staff_name as staffName, sm.department " +
                    "FROM sal_salary s LEFT JOIN staff_master sm ON s.staff_id = sm.staff_id " +
                    "WHERE s.is_deleted = 0");
            List<Object> params = new ArrayList<>();
            if (effective != null) {
                sql.append(" AND s.store_id = ?");
                params.add(effective);
            }
            if (month != null && !month.isEmpty()) {
                sql.append(" AND s.month = ?");
                params.add(month);
            }
            if (deptId != null) {
                sql.append(" AND sm.dept_id = ?");
                params.add(deptId);
            }
            sql.append(" ORDER BY s.month DESC, s.id DESC LIMIT 500");
            return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取工资列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/salary")
    public Result<Salary> createSalary(@RequestBody Salary salary) {
        try {
            Long userStore = resolveWriteStoreId();
            if (userStore != null) {
                salary.setStoreId(userStore);
            } else if (salary.getStoreId() == null) {
                salary.setStoreId(1L);
            }
            if (salary.getIsDeleted() == null) salary.setIsDeleted(0);
            if (salary.getTotalSalary() == null) {
                BigDecimal total = BigDecimal.ZERO;
                if (salary.getBaseSalary() != null) total = total.add(salary.getBaseSalary());
                if (salary.getOvertimeSalary() != null) total = total.add(salary.getOvertimeSalary());
                if (salary.getSubsidy() != null) total = total.add(salary.getSubsidy());
                if (salary.getBonus() != null) total = total.add(salary.getBonus());
                if (salary.getLateDeduct() != null) total = total.subtract(salary.getLateDeduct());
                if (salary.getLeaveDeduct() != null) total = total.subtract(salary.getLeaveDeduct());
                if (salary.getLeaveEarlyDeduct() != null) total = total.subtract(salary.getLeaveEarlyDeduct());
                if (salary.getAbsenteeismDeduct() != null) total = total.subtract(salary.getAbsenteeismDeduct());
                salary.setTotalSalary(total);
            }
            return Result.success(salaryRepo.save(salary));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建工资记录失败: " + e.getMessage());
        }
    }

    @PutMapping("/salary/{id}")
    public Result<Salary> updateSalary(@PathVariable Integer id, @RequestBody Salary salary) {
        try {
            Salary existing = salaryRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "工资记录不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权编辑非本店工资记录");
            }
            if (salary.getStaffId() != null) existing.setStaffId(salary.getStaffId());
            if (salary.getMonth() != null) existing.setMonth(salary.getMonth());
            if (salary.getBaseSalary() != null) existing.setBaseSalary(salary.getBaseSalary());
            if (salary.getOvertimeSalary() != null) existing.setOvertimeSalary(salary.getOvertimeSalary());
            if (salary.getSubsidy() != null) existing.setSubsidy(salary.getSubsidy());
            if (salary.getBonus() != null) existing.setBonus(salary.getBonus());
            if (salary.getLateDeduct() != null) existing.setLateDeduct(salary.getLateDeduct());
            if (salary.getLeaveDeduct() != null) existing.setLeaveDeduct(salary.getLeaveDeduct());
            if (salary.getLeaveEarlyDeduct() != null) existing.setLeaveEarlyDeduct(salary.getLeaveEarlyDeduct());
            if (salary.getAbsenteeismDeduct() != null) existing.setAbsenteeismDeduct(salary.getAbsenteeismDeduct());
            if (salary.getRemark() != null) existing.setRemark(salary.getRemark());
            existing.setUpdateTime(null);
            Salary saved = salaryRepo.save(existing);
            BigDecimal total = BigDecimal.ZERO;
            if (saved.getBaseSalary() != null) total = total.add(saved.getBaseSalary());
            if (saved.getOvertimeSalary() != null) total = total.add(saved.getOvertimeSalary());
            if (saved.getSubsidy() != null) total = total.add(saved.getSubsidy());
            if (saved.getBonus() != null) total = total.add(saved.getBonus());
            if (saved.getLateDeduct() != null) total = total.subtract(saved.getLateDeduct());
            if (saved.getLeaveDeduct() != null) total = total.subtract(saved.getLeaveDeduct());
            if (saved.getLeaveEarlyDeduct() != null) total = total.subtract(saved.getLeaveEarlyDeduct());
            if (saved.getAbsenteeismDeduct() != null) total = total.subtract(saved.getAbsenteeismDeduct());
            if (saved.getTotalSalary() == null || !saved.getTotalSalary().equals(total)) {
                saved.setTotalSalary(total);
                saved = salaryRepo.save(saved);
            }
            return Result.success(saved);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新工资失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/salary/{id}")
    public Result<?> deleteSalary(@PathVariable Integer id) {
        try {
            Salary existing = salaryRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "工资记录不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权删除非本店工资记录");
            }
            existing.setIsDeleted(1);
            salaryRepo.save(existing);
            return Result.success("工资记录已删除");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除工资失败: " + e.getMessage());
        }
    }

    // ===== Payroll 工资加密接口 =====
    @GetMapping("/payroll")
    public Result<List<Map<String, Object>>> getPayroll(@RequestParam(defaultValue = "1") Long storeId,
                                                        @RequestParam(required = false) String month) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            String targetMonth = month != null && !month.isEmpty() ? month :
                    LocalDate.now().getYear() + "-" + String.format("%02d", LocalDate.now().getMonthValue());
            StringBuilder sql = new StringBuilder("SELECT " +
                    "CAST(sm.staff_id AS CHAR) as emp_id, sm.staff_name as emp_name, sm.department, " +
                    "COALESCE(s.base_salary, sm.basic_salary, 0) as base_salary, " +
                    "COALESCE(sm.performance_salary, 0) as post_salary, " +
                    "COALESCE(sm.subsidy, 0) as allowance, " +
                    "COALESCE(s.overtime_salary, 0) as overtime_pay, " +
                    "COALESCE(s.bonus, sm.bonus, 0) as bonus, " +
                    "0 as attendance_pay, " +
                    "COALESCE(sm.social_insurance, 0) as deduction_social, " +
                    "COALESCE(sm.housing_fund, 0) as deduction_tax, " +
                    "(COALESCE(s.late_deduct, 0) + COALESCE(s.leave_deduct, 0) + COALESCE(s.absenteeism_deduct, 0)) as deduction_other, " +
                    "COALESCE(s.total_salary, 0) as gross_pay, " +
                    "COALESCE(s.total_salary, 0) - COALESCE(sm.social_insurance, 0) - COALESCE(sm.housing_fund, 0) as net_pay " +
                    "FROM staff_master sm LEFT JOIN sal_salary s ON s.staff_id = sm.staff_id AND s.month = ? AND s.is_deleted = 0 " +
                    "WHERE sm.employment_status IN ('active', '在职')");
            List<Object> params = new ArrayList<>(List.of(targetMonth));
            if (effective != null) {
                sql.append(" AND sm.store_id = ?");
                params.add(effective);
            }
            sql.append(" ORDER BY sm.staff_id");
            return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取工资表失败: " + e.getMessage());
        }
    }

    @PostMapping("/payroll/unlock")
    public Result<Map<String, String>> unlockPayroll(@RequestBody Map<String, Object> body) {
        try {
            String code = body.get("code") != null ? body.get("code").toString() : "";
            if ("payroll2026".equals(code) || "888888".equals(code)) {
                String token = "UNLOCK-" + UUID.randomUUID().toString().substring(0, 8);
                return Result.success(Map.of("token", token));
            }
            return Result.error(401, "验证码错误");
        } catch (Exception e) {
            return Result.error(500, "解锁失败: " + e.getMessage());
        }
    }

    @PostMapping("/payroll/lock")
    public Result<?> lockPayroll(@RequestBody Map<String, Object> body) {
        return Result.success("已锁定");
    }

    // ===== 工资扣款配置 =====
    @GetMapping("/salary-deduct")
    public Result<List<SalaryDeduct>> getSalaryDeductList(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            if (effective == null) {
                return Result.success(salaryDeductRepo.findAll().stream()
                        .filter(sd -> sd.getIsDeleted() == null || sd.getIsDeleted() == 0).toList());
            }
            return Result.success(salaryDeductRepo.findByStoreIdAndIsDeletedOrderByCreateTimeDesc(effective, 0));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取扣款配置失败: " + e.getMessage());
        }
    }

    @PostMapping("/salary-deduct")
    public Result<SalaryDeduct> createSalaryDeduct(@RequestBody SalaryDeduct sd) {
        try {
            Long userStore = resolveWriteStoreId();
            if (userStore != null) sd.setStoreId(userStore);
            else if (sd.getStoreId() == null) sd.setStoreId(1L);
            if (sd.getIsDeleted() == null) sd.setIsDeleted(0);
            return Result.success(salaryDeductRepo.save(sd));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建扣款配置失败: " + e.getMessage());
        }
    }

    @PutMapping("/salary-deduct/{id}")
    public Result<SalaryDeduct> updateSalaryDeduct(@PathVariable Integer id, @RequestBody SalaryDeduct sd) {
        try {
            SalaryDeduct existing = salaryDeductRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "扣款配置不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权编辑非本店扣款配置");
            }
            if (sd.getDeptId() != null) existing.setDeptId(sd.getDeptId());
            if (sd.getTypeNum() != null) existing.setTypeNum(sd.getTypeNum());
            if (sd.getDeduct() != null) existing.setDeduct(sd.getDeduct());
            if (sd.getRemark() != null) existing.setRemark(sd.getRemark());
            existing.setUpdateTime(null);
            return Result.success(salaryDeductRepo.save(existing));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新扣款配置失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/salary-deduct/{id}")
    public Result<?> deleteSalaryDeduct(@PathVariable Integer id) {
        try {
            SalaryDeduct existing = salaryDeductRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "扣款配置不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权删除非本店扣款配置");
            }
            existing.setIsDeleted(1);
            salaryDeductRepo.save(existing);
            return Result.success("扣款配置已删除");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除扣款配置失败: " + e.getMessage());
        }
    }

    // ===== 社保 =====
    @GetMapping("/insurance")
    public Result<List<SocInsurance>> getInsuranceList(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            if (effective == null) {
                return Result.success(socInsuranceRepo.findAll().stream()
                        .filter(s -> s.getIsDeleted() == null || s.getIsDeleted() == 0).toList());
            }
            return Result.success(socInsuranceRepo.findByStoreIdAndIsDeletedOrderByCreateTimeDesc(effective, 0));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取社保列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/insurance/staff/{staffId}")
    public Result<SocInsurance> getInsuranceByStaffId(@PathVariable Integer staffId) {
        try {
            Long effective = resolveQueryStoreId(1L);
            List<SocInsurance> list;
            if (effective == null) {
                list = socInsuranceRepo.findAll();
            } else {
                list = socInsuranceRepo.findAllByStoreIdAndStaffIdAndIsDeleted(effective, staffId, 0);
            }
            return Result.success(list.stream().findFirst().orElse(null));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取员工社保失败: " + e.getMessage());
        }
    }

    @PostMapping("/insurance")
    public Result<SocInsurance> createInsurance(@RequestBody SocInsurance ins) {
        try {
            Long userStore = resolveWriteStoreId();
            if (userStore != null) ins.setStoreId(userStore);
            else if (ins.getStoreId() == null) ins.setStoreId(1L);
            if (ins.getIsDeleted() == null) ins.setIsDeleted(0);
            if (ins.getStatus() == null) ins.setStatus(0);
            return Result.success(socInsuranceRepo.save(ins));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建社保失败: " + e.getMessage());
        }
    }

    @PutMapping("/insurance/{id}")
    public Result<SocInsurance> updateInsurance(@PathVariable Integer id, @RequestBody SocInsurance ins) {
        try {
            SocInsurance existing = socInsuranceRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "社保记录不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权编辑非本店社保记录");
            }
            if (ins.getStaffId() != null) existing.setStaffId(ins.getStaffId());
            if (ins.getCityId() != null) existing.setCityId(ins.getCityId());
            if (ins.getHouseBase() != null) existing.setHouseBase(ins.getHouseBase());
            if (ins.getPerHouseRate() != null) existing.setPerHouseRate(ins.getPerHouseRate());
            if (ins.getPerHousePay() != null) existing.setPerHousePay(ins.getPerHousePay());
            if (ins.getComHouseRate() != null) existing.setComHouseRate(ins.getComHouseRate());
            if (ins.getComHousePay() != null) existing.setComHousePay(ins.getComHousePay());
            if (ins.getSocialBase() != null) existing.setSocialBase(ins.getSocialBase());
            if (ins.getComSocialPay() != null) existing.setComSocialPay(ins.getComSocialPay());
            if (ins.getPerSocialPay() != null) existing.setPerSocialPay(ins.getPerSocialPay());
            if (ins.getComInjuryRate() != null) existing.setComInjuryRate(ins.getComInjuryRate());
            if (ins.getSocialRemark() != null) existing.setSocialRemark(ins.getSocialRemark());
            if (ins.getHouseRemark() != null) existing.setHouseRemark(ins.getHouseRemark());
            if (ins.getStatus() != null) existing.setStatus(ins.getStatus());
            existing.setUpdateTime(null);
            return Result.success(socInsuranceRepo.save(existing));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新社保失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/insurance/{id}")
    public Result<?> deleteInsurance(@PathVariable Integer id) {
        try {
            SocInsurance existing = socInsuranceRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "社保记录不存在");
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权删除非本店社保记录");
            }
            existing.setIsDeleted(1);
            socInsuranceRepo.save(existing);
            return Result.success("社保记录已删除");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除社保失败: " + e.getMessage());
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
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT * FROM employee_lifecycle WHERE store_id = ? ORDER BY event_date DESC", effective);
            return Result.success(rows.stream().map(this::mapLifecycle).toList());
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取生命周期失败: " + e.getMessage());
        }
    }

    // ===== 总览统计（HRAdmin.vue 使用） =====
    @GetMapping("/stats/overview")
    public Result<Map<String, Object>> getHrOverviewStats(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            String storeFilter = effective != null ? " store_id = " + effective : " 1=1 ";
            Map<String, Object> stats = new HashMap<>();
            List<Map<String, Object>> staffRows = jdbc.queryForList(
                    "SELECT employment_status, department, COUNT(*) as cnt FROM staff_master WHERE " + storeFilter + " GROUP BY employment_status, department");
            int total = 0, active = 0;
            Map<String, Integer> deptMap = new LinkedHashMap<>();
            for (Map<String, Object> r : staffRows) {
                int cnt = ((Number) r.get("cnt")).intValue();
                total += cnt;
                String st = r.get("employment_status") != null ? r.get("employment_status").toString() : "";
                if ("active".equals(st) || "在职".equals(st)) active += cnt;
                String dept = r.get("department") != null ? r.get("department").toString() : "未分配";
                deptMap.merge(dept, cnt, Integer::sum);
            }
            stats.put("totalStaff", total);
            stats.put("activeStaff", active);
            stats.put("inactiveStaff", total - active);
            stats.put("departmentDistribution", deptMap);
            String today = LocalDate.now().toString();
            List<Map<String, Object>> todayLeaveRows = jdbc.queryForList(
                    "SELECT COUNT(*) as cnt FROM leave_record WHERE " + storeFilter +
                            " AND start_date <= ? AND end_date >= ?", today, today);
            stats.put("todayOnLeave", todayLeaveRows.isEmpty() ? 0 :
                    ((Number) todayLeaveRows.get(0).get("cnt")).intValue());
            String thisMonth = LocalDate.now().getYear() + "-" + String.format("%02d", LocalDate.now().getMonthValue());
            List<Map<String, Object>> newHireRows = jdbc.queryForList(
                    "SELECT COUNT(*) as cnt FROM staff_master WHERE " + storeFilter +
                            " AND DATE_FORMAT(created_at, '%Y-%m') = ?", thisMonth);
            stats.put("newThisMonth", newHireRows.isEmpty() ? 0 :
                    ((Number) newHireRows.get(0).get("cnt")).intValue());
            List<Map<String, Object>> pendingRows = jdbc.queryForList(
                    "SELECT 'leave' as type, COUNT(*) as cnt FROM leave_record WHERE " + storeFilter + " AND status = 'pending' " +
                            "UNION ALL SELECT 'overtime', COUNT(*) FROM overtime WHERE " + storeFilter + " AND status = 'pending'");
            Map<String, Integer> pendingMap = new HashMap<>();
            pendingMap.put("leave", 0);
            pendingMap.put("overtime", 0);
            for (Map<String, Object> r : pendingRows) {
                pendingMap.put(r.get("type").toString(), ((Number) r.get("cnt")).intValue());
            }
            stats.put("pendingApprovals", pendingMap);
            return Result.success(stats);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取统计失败: " + e.getMessage());
        }
    }

    // ======== 门店数据隔离辅助方法 ========

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
            return null;
        }
        return userStoreId != null ? userStoreId : requestStoreId;
    }

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
            return null;
        }
        return userStoreId;
    }

    private EmployeeLifecycle mapLifecycle(Map<String, Object> r) {
        EmployeeLifecycle e = new EmployeeLifecycle();
        e.setId(r.get("id") == null ? null : ((Number) r.get("id")).longValue());
        e.setEventType(r.get("event_type") == null ? null : r.get("event_type").toString());
        e.setEventDate(r.get("event_date") == null ? null : java.sql.Date.valueOf(r.get("event_date").toString()).toLocalDate());
        e.setCreatedAt(r.get("created_at") == null ? null : java.sql.Timestamp.valueOf(r.get("created_at").toString()).toLocalDateTime());
        return e;
    }
}
