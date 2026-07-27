package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // ===== 部门 =====
    @GetMapping("/departments")
    public Result<List<Department>> getDepartments(@RequestParam(defaultValue = "1") Long storeId) {
        try { return Result.success(deptRepo.findByStoreIdOrderBySortOrderAsc(storeId)); }
        catch (Exception e) { return Result.error(500, "获取部门列表失败: " + e.getMessage()); }
    }

    // ===== 请假 =====
    @GetMapping("/leave")
    public Result<List<LeaveRecord>> getLeaveList(@RequestParam(defaultValue = "1") Long storeId) {
        try { return Result.success(leaveRepo.findByStoreIdOrderByCreatedAtDesc(storeId)); }
        catch (Exception e) { return Result.error(500, "获取请假列表失败: " + e.getMessage()); }
    }

    @PostMapping("/leave")
    public Result<LeaveRecord> createLeave(@RequestBody LeaveRecord leave) {
        try { return Result.success(leaveRepo.save(leave)); }
        catch (Exception e) { return Result.error(500, "创建请假失败: " + e.getMessage()); }
    }

    // ===== 排班 =====
    @GetMapping("/schedule")
    public Result<List<Schedule>> getScheduleList(@RequestParam(defaultValue = "1") Long storeId) {
        try { return Result.success(scheduleRepo.findByStoreIdOrderByScheduleDateDesc(storeId)); }
        catch (Exception e) { return Result.error(500, "获取排班列表失败: " + e.getMessage()); }
    }

    @PostMapping("/schedule")
    public Result<Schedule> createSchedule(@RequestBody Schedule schedule) {
        try { return Result.success(scheduleRepo.save(schedule)); }
        catch (Exception e) { return Result.error(500, "创建排班失败: " + e.getMessage()); }
    }

    // ===== 加班 =====
    @GetMapping("/overtime")
    public Result<List<Overtime>> getOvertimeList(@RequestParam(defaultValue = "1") Long storeId) {
        try { return Result.success(overtimeRepo.findByStoreIdOrderByCreatedAtDesc(storeId)); }
        catch (Exception e) { return Result.error(500, "获取加班列表失败: " + e.getMessage()); }
    }

    @PostMapping("/overtime")
    public Result<Overtime> createOvertime(@RequestBody Overtime overtime) {
        try { return Result.success(overtimeRepo.save(overtime)); }
        catch (Exception e) { return Result.error(500, "创建加班失败: " + e.getMessage()); }
    }

    // ===== 员工生命周期 =====
    @GetMapping("/lifecycle")
    public Result<List<EmployeeLifecycle>> getLifecycle(@RequestParam(defaultValue = "1") Long storeId) {
        try { return Result.success(lifecycleRepo.findByStoreIdOrderByEventDateDesc(storeId)); }
        catch (Exception e) { return Result.error(500, "获取生命周期失败: " + e.getMessage()); }
    }

    // ===== 考勤 =====
    @GetMapping("/attendance")
    public Result<List<Attendance>> getAttendance(@RequestParam(defaultValue = "1") Long storeId) {
        try { return Result.success(attendanceRepo.findByStoreIdOrderByAttendanceDateDesc(storeId)); }
        catch (Exception e) { return Result.error(500, "获取考勤失败: " + e.getMessage()); }
    }

    @PostMapping("/attendance")
    public Result<Attendance> createAttendance(@RequestBody Attendance attendance) {
        try {
            if (attendance.getStoreId() == null) attendance.setStoreId(1L);
            return Result.success(attendanceRepo.save(attendance));
        } catch (Exception e) { return Result.error(500, "创建考勤失败: " + e.getMessage()); }
    }

    // 注意: /attendance/record, /attendance/summary 在 AttendanceRecordController 中已定义
}
