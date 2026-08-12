package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.PerRoleMenu;
import com.youjian.banquet.service.PerRoleMenuService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr-admin/role-menu")
@CrossOrigin(origins = "*")
public class PerRoleMenuController {

    @Autowired
    private PerRoleMenuService perRoleMenuService;

    @PostMapping
    public Result<PerRoleMenu> add(@RequestBody PerRoleMenu perRoleMenu) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        perRoleMenu.setStoreId(storeId);
        return perRoleMenuService.add(perRoleMenu);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        return perRoleMenuService.deleteById(id);
    }

    @DeleteMapping("/batch/{ids}")
    public Result<Void> deleteBatch(@PathVariable List<Integer> ids) {
        return perRoleMenuService.deleteBatch(ids);
    }

    @PutMapping
    public Result<PerRoleMenu> edit(@RequestBody PerRoleMenu perRoleMenu) {
        return perRoleMenuService.edit(perRoleMenu);
    }

    @GetMapping("/detail/{id}")
    public Result<PerRoleMenu> findById(@PathVariable Integer id) {
        return perRoleMenuService.findById(id);
    }

    @GetMapping
    public Result<Page<PerRoleMenu>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long storeId) {
        if (storeId == null) {
            storeId = UserContext.getCurrentStoreId();
            if (storeId == null) storeId = 1L;
        }
        return perRoleMenuService.list(current, size, storeId);
    }

    @GetMapping("/list")
    public Result<List<PerRoleMenu>> listByStore() {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return perRoleMenuService.listAll(storeId);
    }

    @GetMapping("/role/{roleId}")
    public Result<List<PerRoleMenu>> findByRoleId(@PathVariable Integer roleId) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return perRoleMenuService.findByRoleId(storeId, roleId);
    }

    @GetMapping("/menu/{menuId}")
    public Result<List<PerRoleMenu>> findByMenuId(@PathVariable Integer menuId) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return perRoleMenuService.findByMenuId(storeId, menuId);
    }
}