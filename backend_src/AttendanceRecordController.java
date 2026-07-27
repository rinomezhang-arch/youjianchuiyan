package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.dto.AttendanceRecordDTO.*;
import com.youjian.banquet.service.AttendanceRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/attendance")
@CrossOrigin(origins = "*")
public class AttendanceRecordController {

    @Autowired
    private AttendanceRecordService service;

    /**
     * 加载单员工某月考勤记录
     * GET /api/hr/attendance/record?empId={id}&month={YYYY-MM}
     */
    @GetMapping("/record")
    public Result<AttendanceLoadDTO> loadRecord(
            @RequestParam String empId,
            @RequestParam String month) {
        try {
            AttendanceLoadDTO data = service.loadAttendance(empId, month);
            if (data == null) {
                return Result.error(404, "未找到该员工" + month + "的考勤记录");
            }
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "加载考勤失败: " + e.getMessage());
        }
    }

    /**
     * 保存/更新考勤记录
     * POST /api/hr/attendance/record
     * Body: AttendanceSaveDTO (JSON)
     */
    @PostMapping("/record")
    public Result<Void> saveRecord(@RequestBody AttendanceSaveDTO dto) {
        try {
            if (dto.getEmpId() == null || dto.getEmpId().isEmpty()) {
                return Result.error(400, "员工ID不能为空");
            }
            if (dto.getMonth() == null || dto.getMonth().isEmpty()) {
                return Result.error(400, "月份不能为空");
            }
            if (dto.getDays() == null) {
                return Result.error(400, "考勤数据格式错误");
            }
            service.saveAttendance(dto);
            return Result.success();
        } catch (Exception e) {
            return Result.error(500, "保存考勤失败: " + e.getMessage());
        }
    }

    /**
     * 获取某月全员考勤汇总
     * GET /api/hr/attendance/summary?month={YYYY-MM}
     */
    @GetMapping("/summary")
    public Result<List<AttendanceSummaryDTO>> getSummary(@RequestParam String month) {
        try {
            List<AttendanceSummaryDTO> data = service.getSummary(month);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "获取考勤汇总失败: " + e.getMessage());
        }
    }

    /**
     * 删除单条考勤记录
     * DELETE /api/hr/attendance/record/{id}
     */
    @DeleteMapping("/record/{id}")
    public Result<Void> deleteRecord(@PathVariable Integer id) {
        try {
            service.deleteRecord(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(500, "删除考勤记录失败: " + e.getMessage());
        }
    }
}
