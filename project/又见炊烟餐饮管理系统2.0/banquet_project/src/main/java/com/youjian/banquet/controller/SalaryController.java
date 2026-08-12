package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.Salary;
import com.youjian.banquet.service.SalaryService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/hr-admin/salary")
@CrossOrigin(origins = "*")
public class SalaryController {

    @Autowired
    private SalaryService salaryService;

    @PostMapping
    public Result<Salary> add(@RequestBody Salary salary) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        salary.setStoreId(storeId);
        return salaryService.add(salary);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        return salaryService.deleteById(id);
    }

    @DeleteMapping("/batch/{ids}")
    public Result<Void> deleteBatch(@PathVariable List<Integer> ids) {
        return salaryService.deleteBatch(ids);
    }

    @PutMapping
    public Result<Salary> edit(@RequestBody Salary salary) {
        return salaryService.edit(salary);
    }

    @GetMapping("/detail/{id}")
    public Result<Salary> findById(@PathVariable Integer id) {
        return salaryService.findById(id);
    }

    @GetMapping
    public Result<Page<Salary>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long storeId) {
        if (storeId == null) {
            storeId = UserContext.getCurrentStoreId();
            if (storeId == null) storeId = 1L;
        }
        return salaryService.list(current, size, storeId);
    }

    @GetMapping("/list")
    public Result<List<Salary>> listByStore() {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        return salaryService.listAll(storeId);
    }

    @GetMapping("/month")
    public Result<Page<Salary>> listByMonth(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long storeId,
            @RequestParam String month) {
        if (storeId == null) {
            storeId = UserContext.getCurrentStoreId();
            if (storeId == null) storeId = 1L;
        }
        return salaryService.listByMonth(current, size, storeId, month);
    }

    @PostMapping("/set")
    public Result<Salary> setSalary(@RequestBody Salary salary) {
        Long storeId = UserContext.getCurrentStoreId();
        if (storeId == null) storeId = 1L;
        salary.setStoreId(storeId);
        return salaryService.setSalary(salary);
    }

    @PostMapping("/calculate")
    public Result<BigDecimal> calculateTotalSalary(@RequestBody Salary salary,
                                                    @RequestParam(defaultValue = "0") BigDecimal socialPay,
                                                    @RequestParam(defaultValue = "0") BigDecimal housePay) {
        BigDecimal total = salaryService.calculateTotalSalary(salary, socialPay, housePay);
        return Result.success(total);
    }
}