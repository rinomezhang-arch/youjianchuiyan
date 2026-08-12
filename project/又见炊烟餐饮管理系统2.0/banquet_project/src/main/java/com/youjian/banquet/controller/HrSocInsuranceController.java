package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrSocInsurance;
import com.youjian.banquet.service.HrSocInsuranceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 员工五险一金管理控制器 (HR系统复刻)
 * 来源: HR系统 InsuranceController
 * 路径: /api/hr/soc-insurance
 *
 * 功能:
 * - 五险一金CRUD（新增/删除/编辑/查询）
 * - 五险一金自动计算（养老/医疗/失业/工伤/生育 + 公积金）
 * - 按员工/月份查询
 * - 支付状态管理
 */
@RestController
@RequestMapping("/api/hr-admin/soc-insurance")
@CrossOrigin(origins = "*")
public class HrSocInsuranceController {

    @Autowired
    private HrSocInsuranceService insuranceService;

    /**
     * 新增五险一金记录
     */
    @PostMapping
    public Result<HrSocInsurance> add(@RequestBody HrSocInsurance insurance,
                                       @RequestAttribute(value = "store_id", required = false) Long storeId) {
        if (storeId == null) storeId = 1L;
        insurance.setStoreId(storeId);
        return insuranceService.add(insurance);
    }

    /**
     * 逻辑删除
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Integer id) {
        return insuranceService.deleteById(id);
    }

    /**
     * 批量逻辑删除
     */
    @DeleteMapping("/batch")
    public Result<String> deleteBatch(@RequestBody List<Integer> ids) {
        return insuranceService.deleteBatch(ids);
    }

    /**
     * 编辑更新
     */
    @PutMapping
    public Result<HrSocInsurance> edit(@RequestBody HrSocInsurance insurance) {
        return insuranceService.edit(insurance);
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result<HrSocInsurance> findById(@PathVariable Integer id) {
        return insuranceService.findById(id);
    }

    /**
     * 根据员工ID查询五险一金（返回最近一条）
     */
    @GetMapping("/staff/{staffId}")
    public Result<HrSocInsurance> findByStaffId(@RequestAttribute(value = "store_id", required = false) Long storeId,
                                                  @PathVariable Integer staffId) {
        if (storeId == null) storeId = 1L;
        return insuranceService.findByStaffId(storeId, staffId);
    }

    /**
     * 查询员工所有五险一金记录
     */
    @GetMapping("/staff/{staffId}/all")
    public Result<List<HrSocInsurance>> listByStaff(@RequestAttribute(value = "store_id", required = false) Long storeId,
                                                     @PathVariable Integer staffId) {
        if (storeId == null) storeId = 1L;
        return insuranceService.listByStaff(storeId, staffId);
    }

    /**
     * 按缴纳月份查询
     */
    @GetMapping("/month/{payMonth}")
    public Result<List<HrSocInsurance>> listByMonth(@RequestAttribute(value = "store_id", required = false) Long storeId,
                                                     @PathVariable String payMonth) {
        if (storeId == null) storeId = 1L;
        return insuranceService.listByMonth(storeId, payMonth);
    }

    /**
     * 多条件分页查询
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> listByPage(@RequestParam(defaultValue = "1") Integer current,
                                                   @RequestParam(defaultValue = "10") Integer size,
                                                   @RequestAttribute(value = "store_id", required = false) Long storeId,
                                                   @RequestParam(required = false) String staffName) {
        if (storeId == null) storeId = 1L;
        return insuranceService.listByStore(current, size, storeId, staffName);
    }

    /**
     * 为员工设置五险一金（自动计算）
     * 核心接口：根据城市配置自动计算养老/医疗/失业/工伤/生育/公积金
     * 如果该员工已存在记录则更新，否则新增
     */
    @PostMapping("/set")
    public Result<HrSocInsurance> setInsurance(@RequestBody HrSocInsurance insurance,
                                                @RequestAttribute(value = "store_id", required = false) Long storeId) {
        if (storeId == null) storeId = 1L;
        insurance.setStoreId(storeId);
        return insuranceService.setInsurance(insurance);
    }

    /**
     * 计算并保存五险一金（不判断是否已存在，直接计算新增）
     */
    @PostMapping("/calculate")
    public Result<HrSocInsurance> calculateAndSave(@RequestBody HrSocInsurance insurance,
                                                    @RequestAttribute(value = "store_id", required = false) Long storeId) {
        if (storeId == null) storeId = 1L;
        insurance.setStoreId(storeId);
        return insuranceService.calculateAndSave(insurance);
    }

    /**
     * 更新支付状态
     */
    @PutMapping("/{id}/status")
    public Result<String> updateStatus(@PathVariable Integer id, @RequestParam Integer status) {
        return insuranceService.updateStatus(id, status);
    }
}