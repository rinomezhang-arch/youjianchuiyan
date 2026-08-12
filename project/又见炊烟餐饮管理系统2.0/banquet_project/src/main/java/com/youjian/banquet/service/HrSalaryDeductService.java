package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrSalaryDeduct;
import com.youjian.banquet.repository.HrSalaryDeductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 薪资扣款配置服务
 * 对应参考系统 SalaryDeductService
 * 管理按部门配置的迟到/早退/旷工/休假扣款规则
 */
@Service
public class HrSalaryDeductService {

    @Autowired
    private HrSalaryDeductRepository hrSalaryDeductRepo;

    // ==================== 扣款类型常量 ====================
    public static final int DEDUCT_LATE = 0;
    public static final int DEDUCT_LEAVE_EARLY = 1;
    public static final int DEDUCT_ABSENTEEISM = 2;
    public static final int DEDUCT_LEAVE = 3;

    // ==================== 基础 CRUD ====================

    public Result<HrSalaryDeduct> add(HrSalaryDeduct deduct) {
        HrSalaryDeduct saved = hrSalaryDeductRepo.save(deduct);
        return Result.success(saved);
    }

    public Result<String> deleteById(Integer id) {
        hrSalaryDeductRepo.deleteById(id);
        return Result.success("删除成功");
    }

    @Transactional
    public Result<String> deleteBatch(List<Integer> ids) {
        hrSalaryDeductRepo.deleteAllById(ids);
        return Result.success("批量删除成功");
    }

    public Result<HrSalaryDeduct> edit(HrSalaryDeduct deduct) {
        if (deduct.getId() == null) {
            return Result.error(400, "id不能为空");
        }
        HrSalaryDeduct updated = hrSalaryDeductRepo.save(deduct);
        return Result.success(updated);
    }

    public Result<HrSalaryDeduct> findById(Integer id) {
        return hrSalaryDeductRepo.findById(id)
                .map(Result::success)
                .orElse(Result.error(404, "记录不存在"));
    }

    // ==================== 业务方法 ====================

    /**
     * 根据部门和扣款类型查询
     * 对应参考系统 find(deptId, typeNum)
     */
    public Result<HrSalaryDeduct> find(Long storeId, Integer deptId, Integer typeNum) {
        if (storeId == null) storeId = 1L;
        return hrSalaryDeductRepo.findByStoreIdAndDeptIdAndTypeNum(storeId, deptId, typeNum)
                .map(Result::success)
                .orElse(Result.error(404, "未找到该扣款配置"));
    }

    /**
     * 设置扣款规则（新增或更新）
     * 对应参考系统 setSalaryDeduct(SalaryDeduct)
     * 如果同部门同类型已存在则更新，否则新增
     */
    @Transactional
    public Result<HrSalaryDeduct> setSalaryDeduct(HrSalaryDeduct deduct) {
        Long storeId = deduct.getStoreId() != null ? deduct.getStoreId() : 1L;
        HrSalaryDeduct existing = hrSalaryDeductRepo
                .findByStoreIdAndDeptIdAndTypeNum(storeId, deduct.getDeptId(), deduct.getTypeNum())
                .orElse(null);

        if (existing != null) {
            existing.setDeduct(deduct.getDeduct());
            existing.setRemark(deduct.getRemark());
            HrSalaryDeduct saved = hrSalaryDeductRepo.save(existing);
            return Result.success(saved);
        } else {
            HrSalaryDeduct saved = hrSalaryDeductRepo.save(deduct);
            return Result.success(saved);
        }
    }

    /**
     * 获取所有扣款类型枚举
     * 对应参考系统 findAll()
     */
    public Result<List<Map<String, Object>>> findAllDeductTypes() {
        List<Map<String, Object>> types = List.of(
                createTypeMap(DEDUCT_LATE, "迟到扣款", 50),
                createTypeMap(DEDUCT_LEAVE_EARLY, "早退扣款", 50),
                createTypeMap(DEDUCT_ABSENTEEISM, "旷工扣款", 100),
                createTypeMap(DEDUCT_LEAVE, "休假扣款", 80)
        );
        return Result.success(types);
    }

    /**
     * 按门店查询所有扣款配置
     */
    public Result<List<HrSalaryDeduct>> listByStore(Long storeId) {
        if (storeId == null) storeId = 1L;
        return Result.success(hrSalaryDeductRepo.findByStoreId(storeId));
    }

    /**
     * 按门店和部门查询扣款配置
     */
    public Result<List<HrSalaryDeduct>> listByDept(Long storeId, Integer deptId) {
        if (storeId == null) storeId = 1L;
        return Result.success(hrSalaryDeductRepo.findByStoreIdAndDeptId(storeId, deptId));
    }

    private Map<String, Object> createTypeMap(int code, String message, int defaultValue) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("message", message);
        map.put("defaultValue", defaultValue);
        return map;
    }
}