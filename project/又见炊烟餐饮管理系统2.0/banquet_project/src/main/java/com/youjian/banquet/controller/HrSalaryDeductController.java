package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrSalaryDeduct;
import com.youjian.banquet.service.HrSalaryDeductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 薪资扣款配置控制器
 * 对应参考系统 SalaryDeductController
 * 管理按部门配置的迟到/早退/旷工/休假扣款规则
 *
 * 注意：现有系统已有 SalaryDeductController（/api/hr/salary-deduct），
 * 因此本控制器使用 /api/hr/salary-deduct-rule 避免路径冲突
 */
@RestController
@RequestMapping("/api/hr-admin/salary-deduct-rule")
@CrossOrigin(origins = "*")
public class HrSalaryDeductController {

    @Autowired
    private HrSalaryDeductService hrSalaryDeductService;

    /**
     * 新增扣款规则
     * 对应参考系统 POST /salary-deduct
     */
    @PostMapping
    public Result<HrSalaryDeduct> add(@RequestBody HrSalaryDeduct deduct) {
        return hrSalaryDeductService.add(deduct);
    }

    /**
     * 逻辑删除
     * 对应参考系统 DELETE /salary-deduct/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        return hrSalaryDeductService.deleteById(id);
    }

    /**
     * 批量逻辑删除
     * 对应参考系统 DELETE /salary-deduct/batch/{ids}
     */
    @DeleteMapping("/batch/{ids}")
    public Result<String> deleteBatch(@PathVariable List<Integer> ids) {
        return hrSalaryDeductService.deleteBatch(ids);
    }

    /**
     * 编辑更新
     * 对应参考系统 PUT /salary-deduct
     */
    @PutMapping
    public Result<HrSalaryDeduct> edit(@RequestBody HrSalaryDeduct deduct) {
        return hrSalaryDeductService.edit(deduct);
    }

    /**
     * 根据id查询
     * 对应参考系统 GET /salary-deduct/{id}
     */
    @GetMapping("/{id}")
    public Result<HrSalaryDeduct> findById(@PathVariable Integer id) {
        return hrSalaryDeductService.findById(id);
    }

    /**
     * 根据部门和扣款类型查询
     * 对应参考系统 GET /salary-deduct/{deptId}/{typeNum}
     */
    @GetMapping("/{deptId}/{typeNum}")
    public Result<HrSalaryDeduct> find(
            @RequestParam(required = false) Long storeId,
            @PathVariable Integer deptId,
            @PathVariable Integer typeNum) {
        return hrSalaryDeductService.find(storeId, deptId, typeNum);
    }

    /**
     * 设置扣款规则（新增或更新，按部门+类型唯一）
     * 对应参考系统 POST /salary-deduct/set
     */
    @PostMapping("/set")
    public Result<HrSalaryDeduct> setSalaryDeduct(@RequestBody HrSalaryDeduct deduct) {
        return hrSalaryDeductService.setSalaryDeduct(deduct);
    }

    /**
     * 获取所有扣款类型枚举
     * 对应参考系统 GET /salary-deduct/all
     */
    @GetMapping("/all")
    public Result<List<Map<String, Object>>> findAllDeductTypes() {
        return hrSalaryDeductService.findAllDeductTypes();
    }

    /**
     * 按门店查询所有扣款配置
     */
    @GetMapping("/store")
    public Result<List<HrSalaryDeduct>> listByStore(@RequestParam(required = false) Long storeId) {
        return hrSalaryDeductService.listByStore(storeId);
    }

    /**
     * 按门店和部门查询扣款配置
     */
    @GetMapping("/dept/{deptId}")
    public Result<List<HrSalaryDeduct>> listByDept(
            @RequestParam(required = false) Long storeId,
            @PathVariable Integer deptId) {
        return hrSalaryDeductService.listByDept(storeId, deptId);
    }
}