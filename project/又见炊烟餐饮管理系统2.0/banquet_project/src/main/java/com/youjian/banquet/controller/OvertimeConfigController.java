package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.OvertimeConfig;
import com.youjian.banquet.service.OvertimeConfigService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr-admin/overtime-config")
@CrossOrigin(origins = "*")
public class OvertimeConfigController {

    @Autowired
    private OvertimeConfigService overtimeConfigService;

    @PostMapping
    public Result<OvertimeConfig> add(@RequestBody OvertimeConfig overtimeConfig) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        overtimeConfig.setStoreId(storeId);
        return overtimeConfigService.add(overtimeConfig);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        return overtimeConfigService.deleteById(id);
    }

    @DeleteMapping("/batch/{ids}")
    public Result<Void> deleteBatch(@PathVariable List<Integer> ids) {
        return overtimeConfigService.deleteBatch(ids);
    }

    @PutMapping
    public Result<OvertimeConfig> edit(@RequestBody OvertimeConfig overtimeConfig) {
        return overtimeConfigService.edit(overtimeConfig);
    }

    @GetMapping("/detail/{id}")
    public Result<OvertimeConfig> findById(@PathVariable Integer id) {
        return overtimeConfigService.findById(id);
    }

    @GetMapping
    public Result<Page<OvertimeConfig>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long storeId) {
        if (storeId == null) {
            storeId = UserContext.getCurrentStoreId();
            if (storeId == null) storeId = 1L;
        }
        return overtimeConfigService.list(current, size, storeId);
    }

    @GetMapping("/list")
    public Result<List<OvertimeConfig>> listByStore() {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return overtimeConfigService.listAll(storeId);
    }

    @GetMapping("/dept/{deptId}")
    public Result<List<OvertimeConfig>> findByDeptId(@PathVariable Integer deptId) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return overtimeConfigService.findByDeptId(storeId, deptId);
    }
}