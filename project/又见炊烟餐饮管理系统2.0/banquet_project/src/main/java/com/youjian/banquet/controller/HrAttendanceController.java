package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrAttendance;
import com.youjian.banquet.service.HrAttendanceService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 考勤管理控制器
 * 复刻自HR系统 AttendanceController，@RequestMapping 改为 /api/hr/attendance
 *
 * @author cow
 * @since 2022-03-29
 */
@RestController
@RequestMapping("/api/hr-admin/attendance")
@CrossOrigin(origins = "*")
public class HrAttendanceController {

    @Autowired
    private HrAttendanceService hrAttendanceService;

    /**
     * 新增考勤记录
     */
    @PostMapping
    public Result<HrAttendance> add(@RequestBody HrAttendance attendance) {
        return hrAttendanceService.add(attendance);
    }

    /**
     * 逻辑删除
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        return hrAttendanceService.deleteById(id);
    }

    /**
     * 批量逻辑删除
     */
    @DeleteMapping("/batch/{ids}")
    public Result<String> deleteBatch(@PathVariable List<Integer> ids) {
        return hrAttendanceService.deleteBatch(ids);
    }

    /**
     * 编辑更新
     */
    @PutMapping
    public Result<HrAttendance> edit(@RequestBody HrAttendance attendance) {
        return hrAttendanceService.edit(attendance);
    }

    /**
     * 按ID查询
     */
    @GetMapping("/{id}")
    public Result<HrAttendance> findById(@PathVariable Integer id) {
        return hrAttendanceService.findById(id);
    }

    /**
     * 分页条件查询（含日历视图）
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer deptId,
            @RequestParam(required = false) String month) {
        return hrAttendanceService.list(current, size, name, deptId, month);
    }

    /**
     * 数据导出接口（CSV格式）
     */
    @GetMapping("/export/{month}")
    public Result<String> export(HttpServletResponse response, @PathVariable String month) throws IOException {
        return hrAttendanceService.export(response, month);
    }

    /**
     * 数据导入接口（CSV格式）
     */
    @PostMapping("/import")
    public Result<String> imp(@RequestParam("file") MultipartFile file) throws IOException {
        return hrAttendanceService.imp(file);
    }

    /**
     * 按员工ID查询最近一次休假记录
     */
    @GetMapping("/staff/{id}")
    public Result<HrAttendance> findByStaffId(@PathVariable Integer id) {
        return hrAttendanceService.findByStaffId(id);
    }

    /**
     * 按员工ID和日期查询
     */
    @GetMapping("/staff/{id}/{date}")
    public Result<HrAttendance> findByStaffIdAndDate(@PathVariable Integer id, @PathVariable String date) {
        return hrAttendanceService.findByStaffIdAndDate(id, date);
    }

    /**
     * 设置考勤（存在则更新，不存在则新增）
     */
    @PutMapping("/set")
    public Result<HrAttendance> setAttendance(@RequestBody HrAttendance attendance) {
        return hrAttendanceService.setAttendance(attendance);
    }

    /**
     * 获取所有考勤状态枚举
     */
    @GetMapping("/all")
    public Result<List<Map<String, Object>>> findAll() {
        return hrAttendanceService.findAll();
    }
}