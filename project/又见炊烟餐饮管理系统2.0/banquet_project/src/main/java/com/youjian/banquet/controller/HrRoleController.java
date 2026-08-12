package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrRole;
import com.youjian.banquet.entity.HrRoleMenu;
import com.youjian.banquet.entity.HrStaffRole;
import com.youjian.banquet.service.HrRoleMenuService;
import com.youjian.banquet.service.HrRoleService;
import com.youjian.banquet.service.HrStaffRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * HR角色管理控制器
 * 完整保留原系统 RoleController 的所有API端点
 * 原路径：/role → 新路径：/api/hr/role
 * 增加多租户 store_id 支持
 */
@RestController
@RequestMapping("/api/hr-admin/role")
@CrossOrigin(origins = "*")
public class HrRoleController {

    @Autowired
    private HrRoleService roleService;

    @Autowired
    private HrRoleMenuService roleMenuService;

    @Autowired
    private HrStaffRoleService staffRoleService;

    // ==================== 角色CRUD ====================

    /**
     * 新增角色
     * 对应原系统 RoleController.add(Role)
     */
    @PostMapping
    public Result<Void> add(@RequestBody HrRole role) {
        return roleService.add(role);
    }

    /**
     * 逻辑删除角色
     * 对应原系统 RoleController.delete(Integer)
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        return roleService.deleteById(id);
    }

    /**
     * 批量逻辑删除
     * 对应原系统 RoleController.deleteBatch(List<Integer>)
     */
    @DeleteMapping("/batch/{ids}")
    public Result<Void> deleteBatch(@PathVariable List<Integer> ids) {
        return roleService.deleteBatch(ids);
    }

    /**
     * 编辑更新角色
     * 对应原系统 RoleController.edit(Role)
     */
    @PutMapping
    public Result<Void> edit(@RequestBody HrRole role) {
        return roleService.edit(role);
    }

    /**
     * 根据ID查询角色
     * 对应原系统 RoleController.findById(Integer)
     */
    @GetMapping("/{id}")
    public Result<HrRole> findById(@PathVariable Integer id) {
        return roleService.findById(id);
    }

    /**
     * 查询所有角色
     * 对应原系统 RoleController.findAll()
     */
    @GetMapping("/all")
    public Result<List<HrRole>> findAll() {
        return roleService.findAll();
    }

    /**
     * 分页条件查询角色
     * 对应原系统 RoleController.list(Integer, Integer, String)
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long storeId) {
        return roleService.list(current, size, name, storeId);
    }

    // ==================== 角色-菜单关联 ====================

    /**
     * 为角色设置菜单
     * 完整保留原系统 RoleController.setMenu(Integer, List<Integer>) 逻辑
     * 所有不需要的菜单将被禁用（status=0），需要的菜单将被启用或新增（status=1）
     */
    @PostMapping("/menu/{roleId}")
    public Result<Void> setMenu(
            @PathVariable Integer roleId,
            @RequestBody List<Integer> menuIds,
            @RequestParam(defaultValue = "1") Long storeId) {
        return roleMenuService.setMenu(roleId, menuIds, storeId);
    }

    /**
     * 得到角色所属的菜单（启用状态的）
     * 对应原系统 RoleController.getMenu(Integer)
     */
    @GetMapping("/menu/{roleId}")
    public Result<List<HrRoleMenu>> getMenu(
            @PathVariable Integer roleId,
            @RequestParam(defaultValue = "1") Long storeId) {
        return roleMenuService.getMenu(roleId, storeId);
    }

    /**
     * 得到角色所属的菜单ID列表（仅返回menuId，用于前端回显）
     */
    @GetMapping("/menu/{roleId}/ids")
    public Result<List<Integer>> getMenuIds(
            @PathVariable Integer roleId,
            @RequestParam(defaultValue = "1") Long storeId) {
        return roleMenuService.getMenuIds(roleId, storeId);
    }

    // ==================== 员工-角色关联 ====================

    /**
     * 为员工设置角色
     * 完整保留原系统 StaffRoleService.setRole(Integer, List<Integer>) 逻辑
     * 所有不需要的角色将被禁用（status=0），需要的角色将被启用或新增（status=1）
     */
    @PostMapping("/staff/{staffId}")
    public Result<Void> setStaffRole(
            @PathVariable Integer staffId,
            @RequestBody List<Integer> roleIds,
            @RequestParam(defaultValue = "1") Long storeId) {
        return staffRoleService.setRole(staffId, roleIds, storeId);
    }

    /**
     * 得到员工所属的角色（启用状态的）
     * 对应原系统 StaffRoleService.getRole(Integer)
     */
    @GetMapping("/staff/{staffId}")
    public Result<List<HrStaffRole>> getStaffRole(
            @PathVariable Integer staffId,
            @RequestParam(defaultValue = "1") Long storeId) {
        return staffRoleService.getRole(staffId, storeId);
    }

    /**
     * 得到员工所属的角色ID列表（仅返回roleId，用于前端回显）
     */
    @GetMapping("/staff/{staffId}/ids")
    public Result<List<Integer>> getStaffRoleIds(
            @PathVariable Integer staffId,
            @RequestParam(defaultValue = "1") Long storeId) {
        return staffRoleService.getRoleIds(staffId, storeId);
    }
}