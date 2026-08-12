package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrMenu;
import com.youjian.banquet.service.HrMenuService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * HR菜单管理控制器
 * 完整保留原系统 MenuController 的所有API端点
 * 原路径：/menu → 新路径：/api/hr/menu
 * 增加多租户 store_id 支持
 */
@RestController
@RequestMapping("/api/hr-admin/menu")
@CrossOrigin(origins = "*")
public class HrMenuController {

    @Autowired
    private HrMenuService menuService;

    /**
     * 新增菜单
     * 对应原系统 MenuController.add(Menu)
     */
    @PostMapping
    public Result<Void> add(@RequestBody HrMenu menu) {
        return menuService.add(menu);
    }

    /**
     * 逻辑删除菜单
     * 对应原系统 MenuController.deleteById(Integer)
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        return menuService.deleteById(id);
    }

    /**
     * 批量逻辑删除
     * 对应原系统 MenuController.deleteBatch(List<Integer>)
     */
    @DeleteMapping("/batch/{ids}")
    public Result<Void> deleteBatch(@PathVariable List<Integer> ids) {
        return menuService.deleteBatch(ids);
    }

    /**
     * 编辑更新菜单
     * 对应原系统 MenuController.edit(Menu)
     */
    @PutMapping
    public Result<Void> edit(@RequestBody HrMenu menu) {
        return menuService.edit(menu);
    }

    /**
     * 根据ID查询菜单
     * 对应原系统 MenuController.findById(Integer)
     */
    @GetMapping("/{id}")
    public Result<HrMenu> findById(@PathVariable Integer id) {
        return menuService.findById(id);
    }

    /**
     * 查询所有菜单（树形结构）
     * 对应原系统 MenuController.findAll()
     */
    @GetMapping("/all")
    public Result<List<HrMenu>> findAll() {
        return menuService.findAll();
    }

    /**
     * 分页条件查询菜单
     * 对应原系统 MenuController.list(Integer, Integer, String)
     * 增加 storeId 多租户过滤
     */
    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long storeId) {
        return menuService.list(current, size, name, storeId);
    }

    /**
     * 获取当前登录员工的菜单（从JWT Token解析staffId）
     * 对应原系统 MenuController.getStaffMenu(HttpServletRequest)
     * 核心权限校验：通过 staff_id → staff_role → role_menu → menu 链路
     */
    @GetMapping("/staff")
    public Result<List<HrMenu>> getStaffMenu() {
        return menuService.getStaffMenu();
    }

    /**
     * 通过员工ID查询菜单（指定员工，用于管理员查看某员工的菜单权限）
     * 对应原系统 MenuService.getStaffMenuPlus(Integer)
     */
    @GetMapping("/staff/{staffId}")
    public Result<List<HrMenu>> getStaffMenuByStaffId(@PathVariable Integer staffId) {
        return menuService.getStaffMenuByStaffId(staffId);
    }

    /**
     * 多租户：通过员工ID和门店ID查询菜单
     */
    @GetMapping("/staff/{staffId}/store/{storeId}")
    public Result<List<HrMenu>> getStaffMenuByStaffIdAndStoreId(
            @PathVariable Integer staffId,
            @PathVariable Long storeId) {
        return menuService.getStaffMenuByStaffIdAndStoreId(staffId, storeId);
    }
}