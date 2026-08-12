package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrOvertime;
import com.youjian.banquet.service.HrOvertimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * HR加班配置控制器
 * 来源：HR系统 OvertimeController
 * 完整保留参考系统的所有API接口
 */
@RestController
@RequestMapping("/api/hr-admin/overtime")
@CrossOrigin(origins = "*")
public class HrOvertimeController {

    @Autowired
    private HrOvertimeService hrOvertimeService;

    /**
     * 新增加班配置
     * 对应参考系统：POST /overtime → add
     */
    @PostMapping
    public Result<HrOvertime> add(@RequestBody HrOvertime overtime) {
        return hrOvertimeService.add(overtime);
    }

    /**
     * 逻辑删除
     * 对应参考系统：DELETE /overtime/{id} → delete
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        return hrOvertimeService.deleteById(id);
    }

    /**
     * 批量逻辑删除
     * 对应参考系统：DELETE /overtime/batch/{ids} → deleteBatch
     */
    @DeleteMapping("/batch/{ids}")
    public Result<String> deleteBatch(@PathVariable List<Integer> ids) {
        return hrOvertimeService.deleteBatch(ids);
    }

    /**
     * 编辑更新
     * 对应参考系统：PUT /overtime → edit
     */
    @PutMapping
    public Result<HrOvertime> edit(@RequestBody HrOvertime overtime) {
        return hrOvertimeService.edit(overtime);
    }

    /**
     * 根据ID查询
     * 对应参考系统：GET /overtime/{id} → findById
     */
    @GetMapping("/detail/{id}")
    public Result<HrOvertime> findById(@PathVariable Integer id) {
        return hrOvertimeService.findById(id);
    }

    /**
     * 根据部门ID和加班类型查询
     * 对应参考系统：GET /overtime/{deptId}/{typeNum} → find
     */
    @GetMapping("/{deptId}/{typeNum}")
    public Result<HrOvertime> find(@PathVariable Integer deptId, @PathVariable Integer typeNum) {
        return hrOvertimeService.find(deptId, typeNum);
    }

    /**
     * 设置加班（saveOrUpdate）
     * 对应参考系统：POST /overtime/set → setOvertime
     */
    @PostMapping("/set")
    public Result<HrOvertime> setOvertime(@RequestBody HrOvertime overtime) {
        return hrOvertimeService.setOvertime(overtime);
    }

    /**
     * 获取所有加班类型枚举（含lowerLimit）
     * 对应参考系统：GET /overtime/all → findAll
     */
    @GetMapping("/all")
    public Result<List<Map<String, Object>>> findAll() {
        return hrOvertimeService.findAll();
    }

    /**
     * 按门店查询加班配置列表
     * 对应参考系统扩展：支持多租户门店隔离
     */
    @GetMapping("/list")
    public Result<List<HrOvertime>> listByStore(@RequestParam(defaultValue = "1") Long storeId) {
        return hrOvertimeService.listByStore(storeId);
    }
}