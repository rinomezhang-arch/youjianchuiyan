package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrOvertime;
import com.youjian.banquet.repository.HrOvertimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * HR加班配置服务
 * 来源：HR系统 OvertimeService（MyBatis Plus → JPA）
 * 完整保留参考系统的所有业务逻辑
 */
@Service
public class HrOvertimeService {

    @Autowired
    private HrOvertimeRepository hrOvertimeRepo;

    // ==================== 加班类型枚举定义 ====================

    /**
     * 加班类型枚举（对应参考系统 OvertimeEnum）
     */
    public enum OvertimeType {
        WORKDAY_OVERTIME(0, "工作日加班", 1.5),
        HOLIDAY_OVERTIME(1, "节假日加班", 3.0),
        DAY_OFF_OVERTIME(2, "休息日加班", 2.0);

        private final Integer code;
        private final String message;
        private final Double lowerLimit;

        OvertimeType(Integer code, String message, Double lowerLimit) {
            this.code = code;
            this.message = message;
            this.lowerLimit = lowerLimit;
        }

        public Integer getCode() { return code; }
        public String getMessage() { return message; }
        public Double getLowerLimit() { return lowerLimit; }
    }

    // ==================== 参考系统业务逻辑 ====================

    /**
     * 新增加班配置
     * 对应参考系统：add(Overtime overtime)
     */
    @Transactional
    public Result<HrOvertime> add(HrOvertime overtime) {
        HrOvertime saved = hrOvertimeRepo.save(overtime);
        if (saved != null) {
            return Result.success(saved);
        }
        return Result.error(500, "新增加班配置失败");
    }

    /**
     * 逻辑删除
     * 对应参考系统：deleteById(Integer id)
     * MyBatis Plus @TableLogic 逻辑删除 → JPA 手动设置 isDeleted = 1
     */
    @Transactional
    public Result<String> deleteById(Integer id) {
        int affected = hrOvertimeRepo.softDeleteById(id);
        if (affected > 0) {
            return Result.success("删除成功");
        }
        return Result.error(500, "删除失败");
    }

    /**
     * 批量逻辑删除
     * 对应参考系统：deleteBatch(List<Integer> ids)
     */
    @Transactional
    public Result<String> deleteBatch(List<Integer> ids) {
        int affected = hrOvertimeRepo.softDeleteBatch(ids);
        if (affected > 0) {
            return Result.success("批量删除成功");
        }
        return Result.error(500, "批量删除失败");
    }

    /**
     * 编辑更新
     * 对应参考系统：edit(Overtime overtime)
     */
    @Transactional
    public Result<HrOvertime> edit(HrOvertime overtime) {
        if (overtime.getId() == null) {
            return Result.error(400, "ID不能为空");
        }
        Optional<HrOvertime> existing = hrOvertimeRepo.findById(overtime.getId());
        if (existing.isEmpty()) {
            return Result.error(404, "加班配置不存在");
        }
        HrOvertime updated = hrOvertimeRepo.save(overtime);
        if (updated != null) {
            return Result.success(updated);
        }
        return Result.error(500, "更新失败");
    }

    /**
     * 根据ID查询
     * 对应参考系统：findById(Integer id)
     */
    public Result<HrOvertime> findById(Integer id) {
        Optional<HrOvertime> overtime = hrOvertimeRepo.findById(id);
        if (overtime.isPresent() && overtime.get().getIsDeleted() == 0) {
            return Result.success(overtime.get());
        }
        return Result.error(404, "加班配置不存在");
    }

    /**
     * 根据部门ID和加班类型查询
     * 对应参考系统：find(Integer deptId, Integer typeNum)
     */
    public Result<HrOvertime> find(Integer deptId, Integer typeNum) {
        Optional<HrOvertime> overtime = hrOvertimeRepo.findByDeptIdAndTypeNumAndIsDeleted(deptId, typeNum, 0);
        if (overtime.isPresent()) {
            return Result.success(overtime.get());
        }
        return Result.error(404, "未找到对应加班配置");
    }

    /**
     * 设置加班（saveOrUpdate逻辑）
     * 对应参考系统：setOvertime(Overtime overtime)
     * 如果 deptId + typeNum 已有记录则更新，否则新增
     */
    @Transactional
    public Result<HrOvertime> setOvertime(HrOvertime overtime) {
        if (overtime.getDeptId() == null || overtime.getTypeNum() == null) {
            return Result.error(400, "部门ID和加班类型不能为空");
        }
        // 查找是否已存在（不区分删除状态，类似 MyBatis Plus 的 saveOrUpdate）
        Optional<HrOvertime> existing = hrOvertimeRepo.findByDeptIdAndTypeNum(overtime.getDeptId(), overtime.getTypeNum());
        if (existing.isPresent()) {
            // 已存在，更新
            HrOvertime exist = existing.get();
            exist.setSalaryMultiple(overtime.getSalaryMultiple());
            exist.setMultipleSalary(overtime.getMultipleSalary());
            exist.setBonus(overtime.getBonus());
            exist.setCountType(overtime.getCountType());
            exist.setMakeUp(overtime.getMakeUp());
            exist.setStatus(overtime.getStatus());
            exist.setRemark(overtime.getRemark());
            if (overtime.getStoreId() != null) {
                exist.setStoreId(overtime.getStoreId());
            }
            // 如果之前被逻辑删除过，恢复
            if (exist.getIsDeleted() != null && exist.getIsDeleted() == 1) {
                exist.setIsDeleted(0);
            }
            HrOvertime updated = hrOvertimeRepo.save(exist);
            return Result.success(updated);
        } else {
            // 不存在，新增
            HrOvertime saved = hrOvertimeRepo.save(overtime);
            return Result.success(saved);
        }
    }

    /**
     * 获取所有加班类型枚举（含lowerLimit）
     * 对应参考系统：findAll()
     */
    public Result<List<Map<String, Object>>> findAll() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (OvertimeType type : OvertimeType.values()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("code", type.getCode());
            map.put("message", type.getMessage());
            map.put("lowerLimit", type.getLowerLimit());
            list.add(map);
        }
        return Result.success(list);
    }

    // ==================== 扩展业务方法 ====================

    /**
     * 按门店ID查询所有加班配置（未删除）
     */
    public Result<List<HrOvertime>> listByStore(Long storeId) {
        List<HrOvertime> list = hrOvertimeRepo.findByStoreIdAndIsDeletedOrderByCreateTimeDesc(storeId, 0);
        return Result.success(list);
    }

    /**
     * 按门店ID查询所有加班配置（含已删除）
     */
    public Result<List<HrOvertime>> listByStoreAll(Long storeId) {
        List<HrOvertime> list = hrOvertimeRepo.findByStoreIdOrderByCreateTimeDesc(storeId);
        return Result.success(list);
    }
}