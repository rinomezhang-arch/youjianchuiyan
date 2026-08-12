package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.Contract;
import com.youjian.banquet.service.ContractService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 劳动合同接口
 * 对应规划手册 5.txt 4.3 - 合同模块
 */
@RestController
@RequestMapping("/api/hr/contract")
@CrossOrigin(origins = "*")
public class ContractController {

    @Autowired private ContractService contractService;

    /** 合同列表 */
    @GetMapping
    public Result<List<Contract>> list(@RequestParam(defaultValue = "1") Long storeId,
                                       @RequestParam(required = false) Integer status) {
        try {
            return Result.success(contractService.list(storeId, status));
        } catch (Exception e) {
            return Result.error(500, "获取合同列表失败: " + e.getMessage());
        }
    }

    /** 合同详情 */
    @GetMapping("/{id}")
    public Result<Contract> get(@PathVariable Long id) {
        try {
            Contract c = contractService.get(id);
            if (c == null) return Result.error(404, "合同不存在");
            return Result.success(c);
        } catch (Exception e) {
            return Result.error(500, "获取合同详情失败: " + e.getMessage());
        }
    }

    /** 新增合同 */
    @PostMapping
    public Result<Contract> create(@RequestBody Contract contract) {
        try {
            return Result.success(contractService.create(contract));
        } catch (Exception e) {
            return Result.error(500, "新增合同失败: " + e.getMessage());
        }
    }

    /** 更新合同 */
    @PutMapping("/{id}")
    public Result<Contract> update(@PathVariable Long id, @RequestBody Contract contract) {
        try {
            return Result.success(contractService.update(id, contract));
        } catch (Exception e) {
            return Result.error(500, "更新合同失败: " + e.getMessage());
        }
    }

    /** 删除合同 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            contractService.delete(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, "删除合同失败: " + e.getMessage());
        }
    }

    /** 合同到期预警 */
    @GetMapping("/expiring")
    public Result<List<Contract>> expiring(@RequestParam(defaultValue = "1") Long storeId,
                                           @RequestParam(defaultValue = "30") int days) {
        try {
            return Result.success(contractService.getExpiringContracts(storeId, days));
        } catch (Exception e) {
            return Result.error(500, "获取到期预警失败: " + e.getMessage());
        }
    }
}
