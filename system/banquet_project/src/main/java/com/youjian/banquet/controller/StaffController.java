package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.StaffMaster;
import com.youjian.banquet.repository.StaffMasterRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = {"/api/hr", "/api/menu-api"})
@CrossOrigin(origins = "*")
public class StaffController {

    @Autowired
    private StaffMasterRepository staffRepository;

    @Autowired
    private JdbcTemplate jdbc;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 获取员工列表（考勤日历下拉用）
     * GET /api/hr/staff
     * 门店数据隔离：店长仅返回本店员工，总经理返回全部
     */
    @GetMapping("/staff")
    public Result<List<StaffMaster>> getAllStaff() {
        try {
            Long effective = resolveQueryStoreId();
            List<StaffMaster> list;
            if (effective == null) {
                // 总经理：全部门店
                list = staffRepository.findByEmploymentStatus("active");
                if (list.isEmpty()) {
                    list = staffRepository.findAll();
                }
            } else {
                // 店长：仅本店
                list = staffRepository.findByStoreIdAndEmploymentStatus(effective, "active");
                if (list.isEmpty()) {
                    list = staffRepository.findByStoreId(effective);
                }
            }
            return Result.success(list);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取员工列表失败: " + e.getMessage());
        }
    }

    /** POST /api/hr/staff — create staff */
    @PostMapping("/staff")
    public Result<StaffMaster> createStaff(@RequestBody StaffMaster staff) {
        try {
            // 店长仅可新增本店员工：强制覆盖 storeId
            Long userStore = resolveWriteStoreId();
            if (userStore != null) {
                staff.setStoreId(userStore);
            } else if (staff.getStoreId() == null) {
                staff.setStoreId(1L);
            }
            if (staff.getEmploymentStatus() == null) staff.setEmploymentStatus("active");
            if (staff.getStaffPassword() == null || staff.getStaffPassword().isBlank()) {
                return Result.error(400, "新员工必须设置密码");
            }
            staff.setStaffPassword(passwordEncoder.encode(staff.getStaffPassword()));
            staff.setCreatedAt(null);
            staff.setUpdatedAt(null);
            StaffMaster saved = staffRepository.save(staff);
            return Result.success(saved);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建员工失败: " + e.getMessage());
        }
    }

    /** PUT /api/hr/staff/{id} — update staff */
    @PutMapping("/staff/{id}")
    public Result<StaffMaster> updateStaff(@PathVariable Integer id, @RequestBody StaffMaster staff) {
        try {
            StaffMaster existing = staffRepository.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "员工不存在");
            // 门店校验：店长仅可编辑本店员工
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权编辑非本店员工");
            }
            if (staff.getStaffName() != null) existing.setStaffName(staff.getStaffName());
            if (staff.getStaffAccount() != null) existing.setStaffAccount(staff.getStaffAccount());
            if (staff.getStaffPassword() != null && !staff.getStaffPassword().isBlank()) {
                existing.setStaffPassword(passwordEncoder.encode(staff.getStaffPassword()));
            }
            if (staff.getStaffGender() != null) existing.setStaffGender(staff.getStaffGender());
            if (staff.getStaffAge() != null) existing.setStaffAge(staff.getStaffAge());
            if (staff.getStaffPhone() != null) existing.setStaffPhone(staff.getStaffPhone());
            if (staff.getStaffPosition() != null) existing.setStaffPosition(staff.getStaffPosition());
            if (staff.getDepartment() != null) existing.setDepartment(staff.getDepartment());
            if (staff.getDeptId() != null) existing.setDeptId(staff.getDeptId());
            if (staff.getHireDate() != null) existing.setHireDate(staff.getHireDate());
            if (staff.getMonthlySalary() != null) existing.setMonthlySalary(staff.getMonthlySalary());
            if (staff.getIdCard() != null) existing.setIdCard(staff.getIdCard());
            if (staff.getHomeAddress() != null) existing.setHomeAddress(staff.getHomeAddress());
            if (staff.getEmergencyContact() != null) existing.setEmergencyContact(staff.getEmergencyContact());
            if (staff.getEmergencyPhone() != null) existing.setEmergencyPhone(staff.getEmergencyPhone());
            if (staff.getEmploymentStatus() != null) existing.setEmploymentStatus(staff.getEmploymentStatus());
            if (staff.getResignReason() != null) existing.setResignReason(staff.getResignReason());
            if (staff.getResignDate() != null) existing.setResignDate(staff.getResignDate());
            if (staff.getRole() != null) existing.setRole(staff.getRole());
            if (staff.getPermissionLevel() != null) existing.setPermissionLevel(staff.getPermissionLevel());
            if (staff.getRemark() != null) existing.setRemark(staff.getRemark());
            // 店长不可跨店调动员工（store_id 修改仅总经理可操作）
            if (userStore != null) {
                existing.setStoreId(existing.getStoreId());
            } else if (staff.getStoreId() != null) {
                existing.setStoreId(staff.getStoreId());
            }
            existing.setUpdatedAt(null);
            StaffMaster saved = staffRepository.save(existing);
            return Result.success(saved);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新员工失败: " + e.getMessage());
        }
    }

    /** PUT /api/hr/staff/{id}/permissions — update permission fields only */
    @PutMapping("/staff/{id}/permissions")
    public Result<StaffMaster> updatePermissions(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        try {
            StaffMaster existing = staffRepository.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "员工不存在");

            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权修改非本店员工权限");
            }

            existing.setPermissionLevel(intValue(body.get("permissionLevel"), existing.getPermissionLevel()));
            existing.setDeptId(intValue(body.get("deptId"), existing.getDeptId()));
            existing.setCanManageKitchen(flagValue(body.get("canManageKitchen")));
            existing.setCanManageSales(flagValue(body.get("canManageSales")));
            existing.setCanManageFinance(flagValue(body.get("canManageFinance")));
            existing.setCanManageHr(flagValue(body.get("canManageHr")));
            existing.setCanViewAllStores(flagValue(body.get("canViewAllStores")));
            existing.setCanEditSystem(flagValue(body.get("canEditSystem")));

            if (userStore == null && body.get("storeId") != null) {
                existing.setStoreId(Long.valueOf(body.get("storeId").toString()));
            }
            return Result.success(staffRepository.save(existing));
        } catch (NumberFormatException e) {
            return Result.error(400, "权限参数格式错误");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新权限失败: " + e.getMessage());
        }
    }

    private Integer intValue(Object value, Integer fallback) {
        return value == null ? fallback : Integer.valueOf(value.toString());
    }

    private Integer flagValue(Object value) {
        if (value == null) return 0;
        if (value instanceof Boolean bool) return bool ? 1 : 0;
        String text = value.toString();
        return ("1".equals(text) || "true".equalsIgnoreCase(text)) ? 1 : 0;
    }

    /** DELETE /api/hr/staff/{id} — soft-delete staff */
    @DeleteMapping("/staff/{id}")
    public Result<?> deleteStaff(@PathVariable Integer id) {
        try {
            StaffMaster existing = staffRepository.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "员工不存在");
            // 门店校验：店长仅可删除本店员工
            Long userStore = resolveWriteStoreId();
            if (userStore != null && (existing.getStoreId() == null || !userStore.equals(existing.getStoreId()))) {
                return Result.error(403, "无权删除非本店员工");
            }
            existing.setEmploymentStatus("resigned");
            staffRepository.save(existing);
            return Result.success("员工已标记离职");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除员工失败: " + e.getMessage());
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

    /**
     * 查询接口：解析有效门店ID。
     * @return null=全局（总经理）；非null=限制到指定门店（店长）
     */
    private Long resolveQueryStoreId() {
        Long currentStaffId = getCurrentStaffId();
        if (currentStaffId == null) {
            throw new SecurityException("未登录，无权访问员工数据");
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT store_id, can_view_all_stores FROM staff_master WHERE staff_id = ? LIMIT 1",
                currentStaffId.intValue());
        if (rows.isEmpty()) {
            throw new SecurityException("无权访问员工数据");
        }
        Map<String, Object> row = rows.get(0);
        int canViewAllStores = flagValue(row.get("can_view_all_stores"));
        Long userStoreId = row.get("store_id") == null ? null : ((Number) row.get("store_id")).longValue();
        boolean isAllStores = UserContext.isDataScopeAll() || canViewAllStores == 1;
        return isAllStores ? null : userStoreId;
    }

    /**
     * 写操作：解析当前用户门店ID。
     * @return 店长门店ID；null=总经理（允许跨门店操作）
     */
    private Long resolveWriteStoreId() {
        return resolveQueryStoreId();
    }
}
