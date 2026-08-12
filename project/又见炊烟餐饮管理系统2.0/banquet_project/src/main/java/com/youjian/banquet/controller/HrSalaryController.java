package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrSalary;
import com.youjian.banquet.service.HrSalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 薪资控制器
 * 对应参考系统 SalaryController
 * 提供薪资的CRUD、分页查询、薪资核算等接口
 *
 * 注意：现有系统已有 SalaryController（/api/hr/salary，处理MonthSalary+模板），
 * 因此本控制器使用 /api/hr/salary-record 避免路径冲突
 */
@RestController
@RequestMapping("/api/hr-admin/salary-record")
@CrossOrigin(origins = "*")
public class HrSalaryController {

    @Autowired
    private HrSalaryService hrSalaryService;

    /**
     * 新增薪资记录
     * 对应参考系统 POST /salary
     */
    @PostMapping
    public Result<HrSalary> add(@RequestBody HrSalary salary) {
        return hrSalaryService.add(salary);
    }

    /**
     * 逻辑删除
     * 对应参考系统 DELETE /salary/{id}
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        return hrSalaryService.deleteById(id);
    }

    /**
     * 批量逻辑删除
     * 对应参考系统 DELETE /salary/batch/{ids}
     */
    @DeleteMapping("/batch/{ids}")
    public Result<String> deleteBatch(@PathVariable List<Integer> ids) {
        return hrSalaryService.deleteBatch(ids);
    }

    /**
     * 编辑更新
     * 对应参考系统 PUT /salary
     */
    @PutMapping
    public Result<HrSalary> edit(@RequestBody HrSalary salary) {
        return hrSalaryService.edit(salary);
    }

    /**
     * 根据id查询
     * 对应参考系统 GET /salary/{id}
     */
    @GetMapping("/{id}")
    public Result<HrSalary> findById(@PathVariable Integer id) {
        return hrSalaryService.findById(id);
    }

    /**
     * 分页条件查询
     * 对应参考系统 GET /salary?current=1&size=10&name=xxx&deptId=1&month=202204
     */
    @GetMapping
    public Result<Page<HrSalary>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer deptId,
            @RequestParam(required = false) String month) {
        return hrSalaryService.list(current, size, storeId, name, deptId, month);
    }

    /**
     * 设置薪资（新增或更新，按员工+月份唯一）
     * 对应参考系统 POST /salary/set
     */
    @PostMapping("/set")
    public Result<HrSalary> setSalary(@RequestBody HrSalary salary) {
        return hrSalaryService.setSalary(salary);
    }

    /**
     * 计算总工资
     * 公式：基础工资 + 加班费 + 补贴 + 奖金 - 迟到扣款 - 早退扣款 - 旷工扣款 - 休假扣款 - 社保 - 公积金
     */
    @PostMapping("/calculate")
    public Result<BigDecimal> calculateTotalSalary(@RequestBody HrSalary salary,
                                                    @RequestParam(defaultValue = "0") BigDecimal socialPay,
                                                    @RequestParam(defaultValue = "0") BigDecimal housePay) {
        BigDecimal total = hrSalaryService.calculateTotalSalary(salary, socialPay, housePay);
        return Result.success(total);
    }

    /**
     * 获取扣款金额
     * 根据门店、部门、扣款类型获取每次扣款金额
     */
    @GetMapping("/deduct-amount")
    public Result<Integer> getDeductAmount(
            @RequestParam Long storeId,
            @RequestParam Integer deptId,
            @RequestParam Integer deductType) {
        Integer amount = hrSalaryService.getDeductAmount(storeId, deptId, deductType);
        return Result.success(amount);
    }
}