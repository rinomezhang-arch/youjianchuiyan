package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.dto.AttendanceRecordDTO.*;
import com.youjian.banquet.service.AttendanceRecordService;
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
@RequestMapping("/api/hr/attendance")
@CrossOrigin(origins = "*")
public class AttendanceRecordController {

    @Autowired
    private AttendanceRecordService service;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 加载单员工某月考勤记录
     * GET /api/hr/attendance/record?empId={id}&month={YYYY-MM}
     */
    @GetMapping("/record")
    public Result<AttendanceLoadDTO> loadRecord(
            @RequestParam String empId,
            @RequestParam String month) {
        try {
            Long effectiveStoreId = resolveEffectiveStoreId();
            AttendanceLoadDTO data = service.loadAttendance(empId, month, effectiveStoreId);
            if (data == null) {
                return Result.error(404, "未找到该员工" + month + "的考勤记录");
            }
            return Result.success(data);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
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
            Long effectiveStoreId = resolveEffectiveStoreId();
            service.saveAttendance(dto, effectiveStoreId);
            return Result.success();
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "保存考勤失败: " + e.getMessage());
        }
    }

    /**
     * 获取某月全员考勤汇总
     * GET /api/hr/attendance/summary?month={YYYY-MM}
     * S级越权漏洞修复：普通员工（can_manage_hr=0）返回 403，店长仅本店，总经理全部
     */
    @GetMapping("/summary")
    public Result<List<AttendanceSummaryDTO>> getSummary(@RequestParam String month) {
        try {
            Long currentStaffId = getCurrentStaffId();
            if (currentStaffId == null) {
                return Result.error(401, "未登录，无法获取考勤汇总");
            }
            // 查询当前用户权限
            List<Map<String, Object>> userRows = jdbc.queryForList(
                    "SELECT store_id, can_view_all_stores, can_manage_hr FROM staff_master WHERE staff_id = ? LIMIT 1",
                    currentStaffId.intValue());
            if (userRows.isEmpty()) {
                return Result.error(403, "无权查看考勤汇总");
            }
            Map<String, Object> userRow = userRows.get(0);
            int canManageHr = userRow.get("can_manage_hr") == null ? 0 : ((Number) userRow.get("can_manage_hr")).intValue();
            int canViewAllStores = userRow.get("can_view_all_stores") == null ? 0 : ((Number) userRow.get("can_view_all_stores")).intValue();
            Long userStoreId = userRow.get("store_id") == null ? null : ((Number) userRow.get("store_id")).longValue();
            // 普通员工不可查看考勤汇总
            if (canManageHr != 1) {
                return Result.error(403, "无权查看考勤汇总");
            }
            // 总经理可查看所有门店，店长仅本店
            boolean isAllStores = UserContext.isDataScopeAll() || canViewAllStores == 1;
            Long effectiveStoreId = isAllStores ? null : userStoreId;
            List<AttendanceSummaryDTO> data = service.getSummary(month, effectiveStoreId);
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
            Long effectiveStoreId = resolveEffectiveStoreId();
            service.deleteRecord(id, effectiveStoreId);
            return Result.success();
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除考勤记录失败: " + e.getMessage());
        }
    }

    // ======== 门店数据隔离辅助方法 ========

    /**
     * 解析当前请求用户的 staffId。
     * GET 请求由 StoreDataScopeAspect 写入 UserContext；POST/DELETE 回退到 JwtAuthInterceptor 设置的请求属性。
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
     * 解析当前用户的有效门店ID。
     * @return null=全局（总经理）；非null=限制到指定门店（店长/普通员工）
     * @throws SecurityException 当未登录时
     */
    private Long resolveEffectiveStoreId() {
        Long currentStaffId = getCurrentStaffId();
        if (currentStaffId == null) {
            throw new SecurityException("未登录，无权操作考勤数据");
        }
        List<Map<String, Object>> userRows = jdbc.queryForList(
                "SELECT store_id, can_view_all_stores FROM staff_master WHERE staff_id = ? LIMIT 1",
                currentStaffId.intValue());
        if (userRows.isEmpty()) {
            throw new SecurityException("无权操作考勤数据");
        }
        Map<String, Object> userRow = userRows.get(0);
        int canViewAllStores = userRow.get("can_view_all_stores") == null ? 0 : ((Number) userRow.get("can_view_all_stores")).intValue();
        Long userStoreId = userRow.get("store_id") == null ? null : ((Number) userRow.get("store_id")).longValue();
        boolean isAllStores = UserContext.isDataScopeAll() || canViewAllStores == 1;
        return isAllStores ? null : userStoreId;
    }
}
