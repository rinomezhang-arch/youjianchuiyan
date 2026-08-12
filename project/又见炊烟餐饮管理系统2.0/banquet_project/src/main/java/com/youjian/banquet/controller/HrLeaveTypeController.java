package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrLeaveType;
import com.youjian.banquet.service.HrLeaveTypeService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 请假类型配置控制器
 * 对应参考系统: LeaveController
 * 完整保留参考系统的所有API接口
 */
@RestController
@RequestMapping("/api/hr-admin/leave-type")
@CrossOrigin(origins = "*")
public class HrLeaveTypeController {

    @Autowired
    private HrLeaveTypeService hrLeaveTypeService;

    /**
     * 新增请假类型配置
     * 对应参考系统：POST /leave → add
     */
    @PostMapping
    public Result<HrLeaveType> add(@RequestBody HrLeaveType leaveType) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        leaveType.setStoreId(storeId);
        return hrLeaveTypeService.add(leaveType);
    }

    /**
     * 逻辑删除
     * 对应参考系统：DELETE /leave/{id} → delete
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        return hrLeaveTypeService.deleteById(id);
    }

    /**
     * 批量逻辑删除
     * 对应参考系统：DELETE /leave/batch/{ids} → deleteBatch
     */
    @DeleteMapping("/batch/{ids}")
    public Result<Void> deleteBatch(@PathVariable List<Integer> ids) {
        return hrLeaveTypeService.deleteBatch(ids);
    }

    /**
     * 编辑更新
     * 对应参考系统：PUT /leave → edit
     */
    @PutMapping
    public Result<HrLeaveType> edit(@RequestBody HrLeaveType leaveType) {
        return hrLeaveTypeService.edit(leaveType);
    }

    /**
     * 根据ID查询
     * 对应参考系统：GET /leave/{id} → findById
     */
    @GetMapping("/detail/{id}")
    public Result<HrLeaveType> findById(@PathVariable Integer id) {
        return hrLeaveTypeService.findById(id);
    }

    /**
     * 根据部门ID和类型查询请假配置
     * 对应参考系统：GET /leave/{deptId}/{typeNum} → find
     */
    @GetMapping("/{deptId}/{typeNum}")
    public Result<HrLeaveType> find(@PathVariable Integer deptId, @PathVariable Integer typeNum) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return hrLeaveTypeService.find(storeId, deptId, typeNum);
    }

    /**
     * 设置假期（不存在则新增，存在则更新）
     * 对应参考系统：POST /leave/set → setLeave
     */
    @PostMapping("/set")
    public Result<Void> setLeave(@RequestBody HrLeaveType leaveType) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        leaveType.setStoreId(storeId);
        return hrLeaveTypeService.setLeave(leaveType);
    }

    /**
     * 根据部门ID查询所有请假类型配置
     * 对应参考系统：GET /leave/dept/{id} → findByDeptId
     */
    @GetMapping("/dept/{deptId}")
    public Result<List<HrLeaveType>> findByDeptId(@PathVariable Integer deptId) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return hrLeaveTypeService.findByDeptId(storeId, deptId);
    }

    /**
     * 获取所有请假类型枚举
     * 对应参考系统：GET /leave/all → findAll
     */
    @GetMapping("/all")
    public Result<List<Map<String, Object>>> findAll() {
        return hrLeaveTypeService.findAll();
    }

    /**
     * 按门店查询请假类型配置列表
     * 对应参考系统扩展：支持多租户门店隔离
     */
    @GetMapping("/list")
    public Result<List<HrLeaveType>> listByStore() {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return hrLeaveTypeService.listAll(storeId);
    }
}