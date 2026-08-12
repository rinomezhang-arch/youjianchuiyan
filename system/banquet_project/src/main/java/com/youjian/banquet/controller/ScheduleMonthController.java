package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.ScheduleDay;
import com.youjian.banquet.entity.ScheduleMonth;
import com.youjian.banquet.service.ScheduleMonthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 排班接口（月度排班+每日明细）
 * 对应规划手册 5.txt 4.3 - 排班模块
 * 路径用 /api/hr/schedule-month 避免与现有 /api/hr/schedule 冲突
 */
@RestController
@RequestMapping("/api/hr/schedule-month")
@CrossOrigin(origins = "*")
public class ScheduleMonthController {

    @Autowired private ScheduleMonthService scheduleService;

    /** 排班主表列表 */
    @GetMapping
    public Result<List<ScheduleMonth>> list(@RequestParam(defaultValue = "1") Long storeId,
                                            @RequestParam(required = false) String month) {
        try {
            return Result.success(scheduleService.listSchedules(storeId, month));
        } catch (Exception e) {
            return Result.error(500, "获取排班列表失败: " + e.getMessage());
        }
    }

    /** 某排班的每日明细 */
    @GetMapping("/{scheduleId}/days")
    public Result<List<ScheduleDay>> days(@PathVariable Long scheduleId) {
        try {
            return Result.success(scheduleService.listDays(scheduleId));
        } catch (Exception e) {
            return Result.error(500, "获取排班明细失败: " + e.getMessage());
        }
    }

    /** 某员工月度排班 */
    @GetMapping("/staff/{staffId}")
    public Result<List<ScheduleDay>> staffSchedule(@PathVariable Long staffId,
                                                   @RequestParam String month) {
        try {
            return Result.success(scheduleService.getStaffSchedule(staffId, month));
        } catch (Exception e) {
            return Result.error(500, "获取员工排班失败: " + e.getMessage());
        }
    }

    /** 生成/覆盖月度排班 */
    @PostMapping
    public Result<ScheduleMonth> generate(@RequestParam(defaultValue = "1") Long storeId,
                                          @RequestParam String month,
                                          @RequestParam Long deptId,
                                          @RequestBody List<ScheduleDay> days) {
        try {
            return Result.success(scheduleService.generateSchedule(storeId, month, deptId, days));
        } catch (Exception e) {
            return Result.error(500, "生成排班失败: " + e.getMessage());
        }
    }

    /** 发布排班 */
    @PutMapping("/{scheduleId}/publish")
    public Result<ScheduleMonth> publish(@PathVariable Long scheduleId,
                                         @RequestParam Long publishedBy) {
        try {
            return Result.success(scheduleService.publish(scheduleId, publishedBy));
        } catch (Exception e) {
            return Result.error(500, "发布排班失败: " + e.getMessage());
        }
    }

    /** 确认排班 */
    @PutMapping("/{scheduleId}/confirm")
    public Result<ScheduleMonth> confirm(@PathVariable Long scheduleId) {
        try {
            return Result.success(scheduleService.confirm(scheduleId));
        } catch (Exception e) {
            return Result.error(500, "确认排班失败: " + e.getMessage());
        }
    }
}
