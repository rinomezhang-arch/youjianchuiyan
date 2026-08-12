package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.HrSalary;
import com.youjian.banquet.entity.HrSalaryDeduct;
import com.youjian.banquet.repository.HrSalaryDeductRepository;
import com.youjian.banquet.repository.HrSalaryRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 薪资服务
 * 对应参考系统 SalaryService
 * 完整保留薪资核算逻辑：基础工资 + 加班费 + 补贴 + 奖金 - 迟到扣款 - 早退扣款 - 旷工扣款 - 休假扣款 - 社保 - 公积金
 */
@Service
public class HrSalaryService {

    @Autowired
    private HrSalaryRepository hrSalaryRepo;

    @Autowired
    private HrSalaryDeductRepository hrSalaryDeductRepo;

    // ==================== 扣款类型常量 ====================
    /** 迟到扣款 */
    public static final int DEDUCT_LATE = 0;
    /** 早退扣款 */
    public static final int DEDUCT_LEAVE_EARLY = 1;
    /** 旷工扣款 */
    public static final int DEDUCT_ABSENTEEISM = 2;
    /** 休假扣款 */
    public static final int DEDUCT_LEAVE = 3;

    // ==================== 默认扣款金额 ====================
    public static final int DEFAULT_LATE_DEDUCT = 50;
    public static final int DEFAULT_LEAVE_EARLY_DEDUCT = 50;
    public static final int DEFAULT_ABSENTEEISM_DEDUCT = 100;
    public static final int DEFAULT_LEAVE_DEDUCT = 80;

    // ==================== 基础 CRUD ====================

    public Result<HrSalary> add(HrSalary salary) {
        HrSalary saved = hrSalaryRepo.save(salary);
        return Result.success(saved);
    }

    public Result<String> deleteById(Integer id) {
        hrSalaryRepo.deleteById(id);
        return Result.success("删除成功");
    }

    @Transactional
    public Result<String> deleteBatch(List<Integer> ids) {
        hrSalaryRepo.deleteAllById(ids);
        return Result.success("批量删除成功");
    }

    public Result<HrSalary> edit(HrSalary salary) {
        if (salary.getId() == null) {
            return Result.error(400, "id不能为空");
        }
        HrSalary updated = hrSalaryRepo.save(salary);
        return Result.success(updated);
    }

    public Result<HrSalary> findById(Integer id) {
        return hrSalaryRepo.findById(id)
                .map(Result::success)
                .orElse(Result.error(404, "记录不存在"));
    }

    // ==================== 分页查询（含薪资核算） ====================

    /**
     * 分页条件查询
     * 对应参考系统 list(current, size, name, deptId, month)
     */
    public Result<Page<HrSalary>> list(Integer current, Integer size, Long storeId,
                                       String name, Integer deptId, String month) {
        final Long effectiveStoreId = (storeId != null) ? storeId : 1L;

        Specification<HrSalary> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("storeId"), effectiveStoreId));
            if (month != null && !month.isEmpty()) {
                predicates.add(cb.equal(root.get("month"), month));
            }
            if (deptId != null) {
                predicates.add(cb.equal(root.get("staffId"), deptId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<HrSalary> page = hrSalaryRepo.findAll(spec, pageRequest);
        return Result.success(page);
    }

    // ==================== 薪资核算逻辑 ====================

    /**
     * 设置薪资（新增或更新）
     * 对应参考系统 setSalary(Salary salary)
     * 如果同月同员工已存在记录则更新，否则新增
     */
    @Transactional
    public Result<HrSalary> setSalary(HrSalary salary) {
        // 查找同月同员工是否存在记录
        HrSalary existing = hrSalaryRepo
                .findByStoreIdAndStaffIdAndMonth(salary.getStoreId(), salary.getStaffId(), salary.getMonth())
                .orElse(null);

        if (existing != null) {
            // 更新
            existing.setBaseSalary(salary.getBaseSalary());
            existing.setOvertimeSalary(salary.getOvertimeSalary());
            existing.setSubsidy(salary.getSubsidy());
            existing.setBonus(salary.getBonus());
            existing.setLateDeduct(salary.getLateDeduct());
            existing.setLeaveDeduct(salary.getLeaveDeduct());
            existing.setLeaveEarlyDeduct(salary.getLeaveEarlyDeduct());
            existing.setAbsenteeismDeduct(salary.getAbsenteeismDeduct());
            existing.setRemark(salary.getRemark());
            existing.setTotalSalary(salary.getTotalSalary());
            HrSalary saved = hrSalaryRepo.save(existing);
            return Result.success(saved);
        } else {
            // 新增
            HrSalary saved = hrSalaryRepo.save(salary);
            return Result.success(saved);
        }
    }

    /**
     * 计算总工资
     * 公式：基础工资 + 加班费 + 补贴 + 奖金 - 迟到扣款 - 早退扣款 - 旷工扣款 - 休假扣款 - 社保 - 公积金
     *
     * @param salary      薪资记录
     * @param socialPay   社保个人缴纳
     * @param housePay    公积金个人缴纳
     * @return 计算后的总工资
     */
    public BigDecimal calculateTotalSalary(HrSalary salary, BigDecimal socialPay, BigDecimal housePay) {
        BigDecimal base = nz(salary.getBaseSalary());
        BigDecimal overtime = nz(salary.getOvertimeSalary());
        BigDecimal subsidy = nz(salary.getSubsidy());
        BigDecimal bonus = nz(salary.getBonus());
        BigDecimal late = nz(salary.getLateDeduct());
        BigDecimal leaveEarly = nz(salary.getLeaveEarlyDeduct());
        BigDecimal absenteeism = nz(salary.getAbsenteeismDeduct());
        BigDecimal leave = nz(salary.getLeaveDeduct());
        BigDecimal sp = nz(socialPay);
        BigDecimal hp = nz(housePay);

        return base.add(overtime)
                .add(subsidy)
                .add(bonus)
                .subtract(late)
                .subtract(leaveEarly)
                .subtract(absenteeism)
                .subtract(leave)
                .subtract(sp)
                .subtract(hp);
    }

    // ==================== 扣款金额计算 ====================

    /**
     * 获取每次迟到扣款金额
     * 对应参考系统 getPerLateDeduct(StaffSalaryVO)
     *
     * @param storeId 门店id
     * @param deptId  部门id
     * @return 每次迟到扣款金额
     */
    public Integer getPerLateDeduct(Long storeId, Integer deptId) {
        return hrSalaryDeductRepo
                .findByStoreIdAndDeptIdAndTypeNum(storeId, deptId, DEDUCT_LATE)
                .map(HrSalaryDeduct::getDeduct)
                .orElse(DEFAULT_LATE_DEDUCT);
    }

    /**
     * 获取每次早退扣款金额
     * 对应参考系统 getPerLeaveEarlyDeduct(StaffSalaryVO)
     */
    public Integer getPerLeaveEarlyDeduct(Long storeId, Integer deptId) {
        return hrSalaryDeductRepo
                .findByStoreIdAndDeptIdAndTypeNum(storeId, deptId, DEDUCT_LEAVE_EARLY)
                .map(HrSalaryDeduct::getDeduct)
                .orElse(DEFAULT_LEAVE_EARLY_DEDUCT);
    }

    /**
     * 获取每次旷工扣款金额
     * 对应参考系统 getPerAbsenteeismDeduct(StaffSalaryVO)
     */
    public Integer getPerAbsenteeismDeduct(Long storeId, Integer deptId) {
        return hrSalaryDeductRepo
                .findByStoreIdAndDeptIdAndTypeNum(storeId, deptId, DEDUCT_ABSENTEEISM)
                .map(HrSalaryDeduct::getDeduct)
                .orElse(DEFAULT_ABSENTEEISM_DEDUCT);
    }

    /**
     * 获取每次休假扣款金额
     * 对应参考系统 getPerLeaveDeduct(StaffSalaryVO)
     */
    public Integer getPerLeaveDeduct(Long storeId, Integer deptId) {
        return hrSalaryDeductRepo
                .findByStoreIdAndDeptIdAndTypeNum(storeId, deptId, DEDUCT_LEAVE)
                .map(HrSalaryDeduct::getDeduct)
                .orElse(DEFAULT_LEAVE_DEDUCT);
    }

    /**
     * 根据扣款类型获取扣款金额
     * 通用方法，对应参考系统中的4个getPerXxxDeduct方法
     */
    public Integer getDeductAmount(Long storeId, Integer deptId, Integer deductType) {
        return hrSalaryDeductRepo
                .findByStoreIdAndDeptIdAndTypeNum(storeId, deptId, deductType)
                .map(HrSalaryDeduct::getDeduct)
                .orElseGet(() -> getDefaultDeduct(deductType));
    }

    private int getDefaultDeduct(Integer deductType) {
        if (deductType == null) return 0;
        return switch (deductType) {
            case DEDUCT_LATE -> DEFAULT_LATE_DEDUCT;
            case DEDUCT_LEAVE_EARLY -> DEFAULT_LEAVE_EARLY_DEDUCT;
            case DEDUCT_ABSENTEEISM -> DEFAULT_ABSENTEEISM_DEDUCT;
            case DEDUCT_LEAVE -> DEFAULT_LEAVE_DEDUCT;
            default -> 0;
        };
    }

    // ==================== 工具方法 ====================

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}