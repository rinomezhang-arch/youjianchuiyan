package com.youjian.banquet.service;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.Salary;
import com.youjian.banquet.repository.SalaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class SalaryService {

    @Autowired
    private SalaryRepository salaryRepository;

    public Result<Salary> add(Salary salary) {
        Salary saved = salaryRepository.save(salary);
        return Result.success(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteById(Integer id) {
        Optional<Salary> opt = salaryRepository.findById(id);
        if (opt.isPresent()) {
            Salary entity = opt.get();
            entity.setIsDeleted(1);
            salaryRepository.save(entity);
            return Result.success();
        }
        return Result.error(500, "删除失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteBatch(List<Integer> ids) {
        List<Salary> entities = salaryRepository.findAllById(ids);
        for (Salary entity : entities) {
            entity.setIsDeleted(1);
        }
        salaryRepository.saveAll(entities);
        return Result.success();
    }

    public Result<Salary> edit(Salary salary) {
        if (salary.getId() == null) {
            return Result.error(500, "ID不能为空");
        }
        Salary updated = salaryRepository.save(salary);
        return Result.success(updated);
    }

    public Result<Salary> findById(Integer id) {
        Optional<Salary> opt = salaryRepository.findById(id);
        return opt.map(Result::success).orElse(Result.error(500, "未找到记录"));
    }

    public Result<Page<Salary>> list(Integer current, Integer size, Long storeId) {
        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Salary> page = salaryRepository.findAll(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("storeId"), storeId),
                        cb.equal(root.get("isDeleted"), 0)
                ), pageRequest);
        return Result.success(page);
    }

    public Result<Page<Salary>> listByMonth(Integer current, Integer size, Long storeId, String month) {
        PageRequest pageRequest = PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Salary> page = salaryRepository.findAll(
                (root, query, cb) -> cb.and(
                        cb.equal(root.get("storeId"), storeId),
                        cb.equal(root.get("month"), month),
                        cb.equal(root.get("isDeleted"), 0)
                ), pageRequest);
        return Result.success(page);
    }

    public Result<List<Salary>> listAll(Long storeId) {
        List<Salary> list = salaryRepository.findByStoreIdAndIsDeletedOrderByCreateTimeDesc(storeId, 0);
        return Result.success(list);
    }

    @Transactional
    public Result<Salary> setSalary(Salary salary) {
        Salary existing = salaryRepository
                .findByStoreIdAndStaffIdAndMonth(salary.getStoreId(), salary.getStaffId(), salary.getMonth())
                .orElse(null);
        if (existing != null) {
            existing.setBaseSalary(salary.getBaseSalary());
            existing.setOvertimeSalary(salary.getOvertimeSalary());
            existing.setSubsidy(salary.getSubsidy());
            existing.setBonus(salary.getBonus());
            existing.setLateDeduct(salary.getLateDeduct());
            existing.setLeaveDeduct(salary.getLeaveDeduct());
            existing.setLeaveEarlyDeduct(salary.getLeaveEarlyDeduct());
            existing.setAbsenteeismDeduct(salary.getAbsenteeismDeduct());
            existing.setTotalSalary(salary.getTotalSalary());
            existing.setRemark(salary.getRemark());
            Salary saved = salaryRepository.save(existing);
            return Result.success(saved);
        } else {
            Salary saved = salaryRepository.save(salary);
            return Result.success(saved);
        }
    }

    public BigDecimal calculateTotalSalary(Salary salary, BigDecimal socialPay, BigDecimal housePay) {
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

        return base.add(overtime).add(subsidy).add(bonus)
                .subtract(late).subtract(leaveEarly).subtract(absenteeism).subtract(leave)
                .subtract(sp).subtract(hp);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}