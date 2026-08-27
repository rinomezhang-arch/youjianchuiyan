package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.StaffLicense;
import com.youjian.banquet.repository.StaffLicenseRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 员工证照管理（License.vue 用）。
 * <p>对应表 staff_license（见 scripts/migrations/create_staff_license_v1.sql）。
 * 前端页面早就完整写好了，此前一直调用不存在的 /api/hr/license，属于
 * "前端画完了，后端没跟上"的 HR 子模块缺口，这里补齐。</p>
 */
@RestController
@RequestMapping("/api/hr/license")
@CrossOrigin(origins = "*")
public class LicenseController {

    @Autowired
    private StaffLicenseRepository licenseRepo;

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        Long storeId = resolveStoreId();
        try {
            String sql = "SELECT l.id, s.staff_name AS staffName, l.license_type AS licenseType, " +
                    "l.license_no AS licenseNo, l.issue_date AS issueDate, l.expire_date AS expireDate, " +
                    "l.status, l.remark, l.staff_id AS staffId " +
                    "FROM staff_license l LEFT JOIN staff_master s ON s.staff_id = l.staff_id " +
                    (storeId != null ? "WHERE l.store_id = ? " : "") +
                    "ORDER BY l.expire_date ASC, l.id DESC";
            List<Map<String, Object>> rows = storeId != null
                    ? jdbc.queryForList(sql, storeId)
                    : jdbc.queryForList(sql);
            return Result.success(rows);
        } catch (Exception e) {
            return Result.error(500, "获取证照列表失败：" + e.getMessage());
        }
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Long storeId = resolveStoreId();
        if (storeId == null) storeId = 1L;
        try {
            StaffLicense l = new StaffLicense();
            l.setStoreId(storeId);
            fillFromBody(l, body);
            StaffLicense saved = licenseRepo.save(l);
            return Result.success(Map.of("id", saved.getId()));
        } catch (Exception e) {
            return Result.error(500, "创建证照记录失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            StaffLicense l = licenseRepo.findById(id).orElse(null);
            if (l == null) return Result.error(404, "证照记录不存在");
            if (l.getStoreId() != null) UserContext.assertStoreAccess(l.getStoreId());
            fillFromBody(l, body);
            licenseRepo.save(l);
            return Result.success(null);
        } catch (IllegalArgumentException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新证照记录失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            StaffLicense l = licenseRepo.findById(id).orElse(null);
            if (l == null) return Result.error(404, "证照记录不存在");
            if (l.getStoreId() != null) UserContext.assertStoreAccess(l.getStoreId());
            licenseRepo.delete(l);
            return Result.success(null);
        } catch (IllegalArgumentException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除证照记录失败：" + e.getMessage());
        }
    }

    private void fillFromBody(StaffLicense l, Map<String, Object> body) {
        if (body.get("staffId") != null) l.setStaffId(Integer.valueOf(body.get("staffId").toString()));
        if (body.get("licenseType") != null) l.setLicenseType(body.get("licenseType").toString());
        if (body.get("licenseNo") != null) l.setLicenseNo(body.get("licenseNo").toString());
        if (body.get("issueDate") != null && !body.get("issueDate").toString().isEmpty()) {
            l.setIssueDate(LocalDate.parse(body.get("issueDate").toString()));
        }
        if (body.get("expireDate") != null && !body.get("expireDate").toString().isEmpty()) {
            l.setExpireDate(LocalDate.parse(body.get("expireDate").toString()));
        }
        if (body.get("status") != null) l.setStatus(body.get("status").toString());
        if (body.get("remark") != null) l.setRemark(body.get("remark").toString());
    }

    private Long resolveStoreId() {
        if (UserContext.isDataScopeAll()) return null;
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? null : sid;
    }
}
