package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.PerStaffRole;
import com.youjian.banquet.service.PerStaffRoleService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr-admin/staff-role")
@CrossOrigin(origins = "*")
public class PerStaffRoleController {

    @Autowired
    private PerStaffRoleService perStaffRoleService;

    @PostMapping
    public Result<PerStaffRole> add(@RequestBody PerStaffRole perStaffRole) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        perStaffRole.setStoreId(storeId);
        return perStaffRoleService.add(perStaffRole);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        return perStaffRoleService.deleteById(id);
    }

    @DeleteMapping("/batch/{ids}")
    public Result<Void> deleteBatch(@PathVariable List<Integer> ids) {
        return perStaffRoleService.deleteBatch(ids);
    }

    @PutMapping
    public Result<PerStaffRole> edit(@RequestBody PerStaffRole perStaffRole) {
        return perStaffRoleService.edit(perStaffRole);
    }

    @GetMapping("/detail/{id}")
    public Result<PerStaffRole> findById(@PathVariable Integer id) {
        return perStaffRoleService.findById(id);
    }

    @GetMapping
    public Result<Page<PerStaffRole>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long storeId) {
        if (storeId == null) {
            storeId = UserContext.getCurrentStoreId();
            if (storeId == null) storeId = 1L;
        }
        return perStaffRoleService.list(current, size, storeId);
    }

    @GetMapping("/list")
    public Result<List<PerStaffRole>> listByStore() {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return perStaffRoleService.listAll(storeId);
    }

    @GetMapping("/staff/{staffId}")
    public Result<List<PerStaffRole>> findByStaffId(@PathVariable Integer staffId) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return perStaffRoleService.findByStaffId(storeId, staffId);
    }

    @GetMapping("/role/{roleId}")
    public Result<List<PerStaffRole>> findByRoleId(@PathVariable Integer roleId) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return perStaffRoleService.findByRoleId(storeId, roleId);
    }
}