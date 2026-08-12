package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.MonthSalary;
import com.youjian.banquet.entity.SalaryTemplate;
import com.youjian.banquet.service.SalaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 薪资接口
 * 对应规划手册 5.txt 4.3 - 薪资模块
 */
@RestController
@RequestMapping("/api/hr/salary")
@CrossOrigin(origins = "*")
public class SalaryController {

    @Autowired private SalaryService salaryService;

    // ===== 薪资模板 =====
    @GetMapping("/template")
    public Result<List<SalaryTemplate>> listTemplates(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            return Result.success(salaryService.listTemplates(storeId));
        } catch (Exception e) {
            return Result.error(500, "获取薪资模板失败: " + e.getMessage());
        }
    }

    @PostMapping("/template")
    public Result<SalaryTemplate> createTemplate(@RequestBody SalaryTemplate t) {
        try {
            return Result.success(salaryService.createTemplate(t));
        } catch (Exception e) {
            return Result.error(500, "新增薪资模板失败: " + e.getMessage());
        }
    }

    @PutMapping("/template/{id}")
    public Result<SalaryTemplate> updateTemplate(@PathVariable Long id, @RequestBody SalaryTemplate t) {
        try {
            return Result.success(salaryService.updateTemplate(id, t));
        } catch (Exception e) {
            return Result.error(500, "更新薪资模板失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/template/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        try {
            salaryService.deleteTemplate(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, "删除薪资模板失败: " + e.getMessage());
        }
    }

    // ===== 月度薪资 =====
    @GetMapping
    public Result<List<MonthSalary>> list(@RequestParam(defaultValue = "1") Long storeId,
                                          @RequestParam(required = false) String month) {
        try {
            return Result.success(salaryService.listSalary(storeId, month));
        } catch (Exception e) {
            return Result.error(500, "获取薪资列表失败: " + e.getMessage());
        }
    }

    /** 薪资核算 */
    @PostMapping("/calculate")
    public Result<Integer> calculate(@RequestParam(defaultValue = "1") Long storeId,
                                     @RequestParam String month) {
        try {
            int count = salaryService.calculateMonthlySalary(storeId, month);
            return Result.success(count);
        } catch (Exception e) {
            return Result.error(500, "薪资核算失败: " + e.getMessage());
        }
    }

    /** 推送财务 */
    @PostMapping("/{id}/push-finance")
    public Result<Void> pushToFinance(@PathVariable Long id) {
        try {
            salaryService.pushToFinance(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, "推送财务失败: " + e.getMessage());
        }
    }

    /** 员工薪资详情 */
    @GetMapping("/staff/{staffId}")
    public Result<MonthSalary> detail(@PathVariable Long staffId,
                                      @RequestParam String month) {
        try {
            return Result.success(salaryService.getSalaryDetail(staffId, month));
        } catch (Exception e) {
            return Result.error(500, "获取薪资详情失败: " + e.getMessage());
        }
    }
}
