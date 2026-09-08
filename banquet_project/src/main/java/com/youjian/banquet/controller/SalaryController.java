package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.MonthSalary;
import com.youjian.banquet.entity.SalaryTemplate;
import com.youjian.banquet.service.SalaryService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 薪资接口
 * 对应规划手册 5.txt 4.3 - 薪资模块
 * <p>
 * TL-OPS-PAYROLL-PAYOUT-GUARD-11：与 {@code PayrollController} 对齐，
 * 全接口补 HR 权限（can_manage_hr）与门店范围校验，不信任请求方传入的 storeId 越权。
 */
@RestController
@RequestMapping("/api/hr/salary")
@CrossOrigin(origins = "*")
public class SalaryController {

    @Autowired private SalaryService salaryService;
    @Autowired private JdbcTemplate jdbc;

    /** 校验结果：当前用户能否管理薪酬，以及门店范围。与 PayrollController.checkPayrollAccess 同款。 */
    private Map<String, Object> checkHrAccess() {
        Long currentStaffId = UserContext.getStaffId();
        if (currentStaffId == null) {
            throw new SecurityException("未登录，无法访问薪资数据");
        }
        List<Map<String, Object>> userRows = this.jdbc.queryForList(
                "SELECT store_id, can_view_all_stores, can_manage_hr FROM staff_master WHERE staff_id = ? LIMIT 1",
                currentStaffId.intValue());
        if (userRows.isEmpty()) {
            throw new SecurityException("无权访问薪资数据");
        }
        Map<String, Object> userRow = userRows.get(0);
        int canManageHr = userRow.get("can_manage_hr") == null ? 0 : ((Number) userRow.get("can_manage_hr")).intValue();
        if (canManageHr != 1) {
            throw new SecurityException("无权访问薪资数据");
        }
        int canViewAllStores = userRow.get("can_view_all_stores") == null ? 0 : ((Number) userRow.get("can_view_all_stores")).intValue();
        Long userStoreId = userRow.get("store_id") == null ? null : ((Number) userRow.get("store_id")).longValue();
        boolean isAllStores = UserContext.isDataScopeAll() || UserContext.isGeneralManager() || canViewAllStores == 1;
        if (!isAllStores && (userStoreId == null || userStoreId <= 0)) {
            throw new SecurityException("薪资门店范围缺失");
        }
        Map<String, Object> access = new HashMap<>();
        access.put("isAllStores", isAllStores);
        access.put("userStoreId", userStoreId);
        access.put("currentStaffId", currentStaffId);
        return access;
    }

    /**
     * 门店范围约束：非全店用户强制用本店 storeId，不信任请求参数。
     * 全店用户可用请求参数（可空则回退本店）。
     */
    private Long resolveStore(Map<String, Object> access, Long requestStoreId) {
        boolean isAllStores = (Boolean) access.get("isAllStores");
        Long userStoreId = (Long) access.get("userStoreId");
        if (isAllStores) {
            return requestStoreId != null ? requestStoreId : userStoreId;
        }
        if (requestStoreId != null && !requestStoreId.equals(userStoreId)) {
            throw new SecurityException("无权限访问其他门店薪资数据");
        }
        return userStoreId;
    }

    // ===== 薪资模板 =====
    @GetMapping("/template")
    public Result<List<SalaryTemplate>> listTemplates(@RequestParam(required = false) Long storeId) {
        try {
            Map<String, Object> access = checkHrAccess();
            Long store = resolveStore(access, storeId);
            return Result.success(salaryService.listTemplates(store));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取薪资模板失败: " + e.getMessage());
        }
    }

    @PostMapping("/template")
    public Result<SalaryTemplate> createTemplate(@RequestBody SalaryTemplate t) {
        try {
            Map<String, Object> access = checkHrAccess();
            Long store = resolveStore(access, t != null ? t.getStoreId() : null);
            if (t != null) t.setStoreId(store);
            return Result.success(salaryService.createTemplate(t));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "新增薪资模板失败: " + e.getMessage());
        }
    }

    @PutMapping("/template/{id}")
    public Result<SalaryTemplate> updateTemplate(@PathVariable Long id, @RequestBody SalaryTemplate t) {
        try {
            Map<String, Object> access = checkHrAccess();
            // 非全店用户：先读已有模板门店，防止越店定位 id 覆盖/搬动他人模板
            if (!(Boolean) access.get("isAllStores")) {
                Long userStoreId = (Long) access.get("userStoreId");
                List<Map<String, Object>> rows = jdbc.queryForList(
                        "SELECT store_id FROM salary_template WHERE template_id = ? LIMIT 1", id);
                if (rows.isEmpty()) {
                    return Result.error(404, "薪资模板不存在");
                }
                Long tplStore = ((Number) rows.get(0).get("store_id")).longValue();
                if (!tplStore.equals(userStoreId)) {
                    return Result.error(403, "无权限更新其他门店薪资模板");
                }
            }
            Long store = resolveStore(access, t != null ? t.getStoreId() : null);
            if (t != null) t.setStoreId(store);
            return Result.success(salaryService.updateTemplate(id, t));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新薪资模板失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/template/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        try {
            Map<String, Object> access = checkHrAccess();
            // 删除需校验目标模板门店范围
            if (!(Boolean) access.get("isAllStores")) {
                Long userStoreId = (Long) access.get("userStoreId");
                List<Map<String, Object>> rows = jdbc.queryForList(
                        "SELECT store_id FROM salary_template WHERE template_id = ? LIMIT 1", id);
                if (rows.isEmpty()) {
                    return Result.error(404, "薪资模板不存在");
                }
                Long tplStore = ((Number) rows.get(0).get("store_id")).longValue();
                if (!tplStore.equals(userStoreId)) {
                    return Result.error(403, "无权限删除其他门店薪资模板");
                }
            }
            salaryService.deleteTemplate(id);
            return Result.success(null);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除薪资模板失败: " + e.getMessage());
        }
    }

    // ===== 月度薪资 =====
    @GetMapping
    public Result<List<MonthSalary>> list(@RequestParam(required = false) Long storeId,
                                          @RequestParam(required = false) String month) {
        try {
            Map<String, Object> access = checkHrAccess();
            Long store = resolveStore(access, storeId);
            return Result.success(salaryService.listSalary(store, month));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取薪资列表失败: " + e.getMessage());
        }
    }

    /** 薪资核算 */
    @PostMapping("/calculate")
    public Result<Integer> calculate(@RequestParam(required = false) Long storeId,
                                     @RequestParam String month) {
        try {
            Map<String, Object> access = checkHrAccess();
            Long store = resolveStore(access, storeId);
            return Result.success(salaryService.calculateMonthlySalary(store, month));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "薪资核算失败: " + e.getMessage());
        }
    }

    /** 推送财务 */
    @PostMapping("/{id}/push-finance")
    public Result<Void> pushToFinance(@PathVariable Long id) {
        try {
            Map<String, Object> access = checkHrAccess();
            // 校验目标薪资记录门店范围
            if (!(Boolean) access.get("isAllStores")) {
                Long userStoreId = (Long) access.get("userStoreId");
                List<Map<String, Object>> rows = jdbc.queryForList(
                        "SELECT store_id FROM month_salary WHERE salary_id = ? LIMIT 1", id);
                if (rows.isEmpty()) {
                    return Result.error(404, "薪资记录不存在");
                }
                Long recStore = ((Number) rows.get(0).get("store_id")).longValue();
                if (!recStore.equals(userStoreId)) {
                    return Result.error(403, "无权限推送其他门店薪资");
                }
            }
            salaryService.pushToFinance(id);
            return Result.success(null);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "推送财务失败: " + e.getMessage());
        }
    }

    /** 员工薪资详情 */
    @GetMapping("/staff/{staffId}")
    public Result<MonthSalary> detail(@PathVariable Long staffId,
                                      @RequestParam String month) {
        try {
            Map<String, Object> access = checkHrAccess();
            // 详情按员工查，需校验该员工门店范围
            if (!(Boolean) access.get("isAllStores")) {
                Long userStoreId = (Long) access.get("userStoreId");
                List<Map<String, Object>> rows = jdbc.queryForList(
                        "SELECT store_id FROM staff_master WHERE staff_id = ? LIMIT 1", staffId.intValue());
                if (rows.isEmpty()) {
                    return Result.error(404, "员工不存在");
                }
                Long staffStore = ((Number) rows.get(0).get("store_id")).longValue();
                if (!staffStore.equals(userStoreId)) {
                    return Result.error(403, "无权限查看其他门店员工薪资");
                }
            }
            return Result.success(salaryService.getSalaryDetail(staffId, month));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取薪资详情失败: " + e.getMessage());
        }
    }
}
