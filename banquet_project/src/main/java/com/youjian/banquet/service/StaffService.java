/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.StaffDTO
 *  com.youjian.banquet.entity.StaffMaster
 *  com.youjian.banquet.repository.StaffMasterRepository
 *  com.youjian.banquet.service.StaffService
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.youjian.banquet.service;

import com.youjian.banquet.dto.StaffDTO;
import com.youjian.banquet.entity.MonthSalary;
import com.youjian.banquet.entity.StaffMaster;
import com.youjian.banquet.repository.MonthSalaryRepository;
import com.youjian.banquet.repository.StaffMasterRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffService {
    @Autowired
    private StaffMasterRepository staffMasterRepository;
    @Autowired
    private MonthSalaryRepository monthSalaryRepository;

    public List<StaffDTO> getAllStaff(String storeId) {
        return this.staffMasterRepository.findByStoreId(Long.valueOf(Long.parseLong(storeId))).stream().map(arg_0 -> this.toDTO(arg_0)).collect(Collectors.toList());
    }

    public StaffDTO getStaff(String staffId, String storeId) {
        return this.staffMasterRepository.findByStaffIdAndStoreId(Integer.valueOf(Integer.parseInt(staffId)), Long.valueOf(Long.parseLong(storeId))).map(arg_0 -> this.toDTO(arg_0)).orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
    }

    @Transactional
    public StaffDTO createStaff(StaffDTO dto) {
        StaffMaster staff = new StaffMaster();
        staff.setStoreId(Long.valueOf(Long.parseLong(dto.getStoreId())));
        staff.setStaffName(dto.getStaffName());
        staff.setStaffPhone(dto.getPhone());
        staff.setStaffGender(dto.getGender());
        staff.setRole(dto.getRole());
        staff.setStaffPosition(dto.getPosition());
        if (dto.getHireDate() != null) {
            staff.setHireDate(LocalDate.parse(dto.getHireDate()));
        }
        staff.setMonthlySalary(dto.getSalary());
        staff.setEmploymentStatus(dto.getStatus() != null ? dto.getStatus() : "active");
        staff.setRemark(dto.getNotes());
        this.staffMasterRepository.save(staff);
        // P1-15 薪资字段独立：创建员工时同步生成当月 month_salary 记录
        if (dto.getSalary() != null && dto.getSalary().compareTo(BigDecimal.ZERO) > 0) {
            this.upsertMonthSalaryForCurrentMonth(staff);
        }
        return this.toDTO(staff);
    }

    @Transactional
    public StaffDTO updateStaff(String staffId, String storeId, StaffDTO dto) {
        StaffMaster staff = (StaffMaster)this.staffMasterRepository.findByStaffIdAndStoreId(Integer.valueOf(Integer.parseInt(staffId)), Long.valueOf(Long.parseLong(storeId))).orElseThrow(() -> new IllegalArgumentException("Staff not found: " + staffId));
        if (dto.getStaffName() != null) {
            staff.setStaffName(dto.getStaffName());
        }
        if (dto.getPhone() != null) {
            staff.setStaffPhone(dto.getPhone());
        }
        if (dto.getGender() != null) {
            staff.setStaffGender(dto.getGender());
        }
        if (dto.getRole() != null) {
            staff.setRole(dto.getRole());
        }
        if (dto.getPosition() != null) {
            staff.setStaffPosition(dto.getPosition());
        }
        if (dto.getHireDate() != null) {
            staff.setHireDate(LocalDate.parse(dto.getHireDate()));
        }
        if (dto.getSalary() != null) {
            staff.setMonthlySalary(dto.getSalary());
        }
        if (dto.getStatus() != null) {
            staff.setEmploymentStatus(dto.getStatus());
        }
        if (dto.getNotes() != null) {
            staff.setRemark(dto.getNotes());
        }
        this.staffMasterRepository.save(staff);
        // P1-15 薪资字段独立：薪资变更时同步更新当月 month_salary 记录
        if (dto.getSalary() != null && dto.getSalary().compareTo(BigDecimal.ZERO) > 0) {
            this.upsertMonthSalaryForCurrentMonth(staff);
        }
        return this.toDTO(staff);
    }

    @Transactional
    public void deleteStaff(String staffId, String storeId) {
        this.staffMasterRepository.findByStaffIdAndStoreId(Integer.valueOf(Integer.parseInt(staffId)), Long.valueOf(Long.parseLong(storeId))).ifPresent(arg_0 -> ((StaffMasterRepository)this.staffMasterRepository).delete(arg_0));
    }

    private StaffDTO toDTO(StaffMaster e) {
        StaffDTO dto = new StaffDTO();
        dto.setStaffId(String.valueOf(e.getStaffId()));
        dto.setStoreId(String.valueOf(e.getStoreId()));
        dto.setStaffName(e.getStaffName());
        dto.setPhone(e.getStaffPhone());
        dto.setGender(e.getStaffGender());
        dto.setRole(e.getRole());
        dto.setPosition(e.getStaffPosition());
        dto.setHireDate(e.getHireDate() != null ? e.getHireDate().toString() : null);
        dto.setSalary(e.getMonthlySalary());
        dto.setStatus(e.getEmploymentStatus());
        dto.setNotes(e.getRemark());
        return dto;
    }

    // ==================== P1-15 薪资字段独立：month_salary 表操作 ====================

    /**
     * 获取指定员工某月的薪资记录
     * @param staffId 员工ID
     * @param salaryMonth 薪资月份（YYYY-MM）
     */
    public Optional<MonthSalary> getMonthSalary(Integer staffId, String salaryMonth) {
        return this.monthSalaryRepository.findByStaffIdAndSalaryMonth(Long.valueOf(staffId), salaryMonth);
    }

    /**
     * 获取门店某月所有员工的薪资列表
     * @param storeId 门店ID
     * @param salaryMonth 薪资月份（YYYY-MM）
     */
    public List<MonthSalary> listMonthSalary(Long storeId, String salaryMonth) {
        return this.monthSalaryRepository.findByStoreIdAndSalaryMonth(storeId, salaryMonth);
    }

    /**
     * 保存或更新月度薪资记录（upsert）
     * 如果 (staff_id, salary_month) 已存在则更新，否则新建
     */
    @Transactional
    public MonthSalary saveMonthSalary(MonthSalary salary) {
        if (salary.getSalaryMonth() == null || salary.getSalaryMonth().isEmpty()) {
            salary.setSalaryMonth(YearMonth.now().toString());
        }
        if (salary.getBaseSalary() == null) {
            salary.setBaseSalary(BigDecimal.ZERO);
        }
        if (salary.getGrossSalary() == null) {
            salary.setGrossSalary(salary.getBaseSalary()
                    .add(salary.getPerformanceSalary() == null ? BigDecimal.ZERO : salary.getPerformanceSalary())
                    .add(salary.getRewardAmount() == null ? BigDecimal.ZERO : salary.getRewardAmount())
                    .add(salary.getOtherAllowance() == null ? BigDecimal.ZERO : salary.getOtherAllowance()));
        }
        if (salary.getNetSalary() == null) {
            BigDecimal gross = salary.getGrossSalary();
            BigDecimal deductions = (salary.getSocialSecurityDeduction() == null ? BigDecimal.ZERO : salary.getSocialSecurityDeduction())
                    .add(salary.getHousingFundDeduction() == null ? BigDecimal.ZERO : salary.getHousingFundDeduction())
                    .add(salary.getOtherDeduction() == null ? BigDecimal.ZERO : salary.getOtherDeduction())
                    .add(salary.getTaxAmount() == null ? BigDecimal.ZERO : salary.getTaxAmount());
            salary.setNetSalary(gross.subtract(deductions));
        }
        // 查询是否已有记录
        Optional<MonthSalary> existing = this.monthSalaryRepository
                .findByStaffIdAndSalaryMonth(salary.getStaffId(), salary.getSalaryMonth());
        if (existing.isPresent()) {
            MonthSalary db = existing.get();
            db.setBaseSalary(salary.getBaseSalary());
            db.setPerformanceSalary(salary.getPerformanceSalary());
            db.setOvertimePay(salary.getOvertimePay());
            db.setRewardAmount(salary.getRewardAmount());
            db.setPunishDeduction(salary.getPunishDeduction());
            db.setLeaveDeduction(salary.getLeaveDeduction());
            db.setSocialSecurityDeduction(salary.getSocialSecurityDeduction());
            db.setHousingFundDeduction(salary.getHousingFundDeduction());
            db.setOtherAllowance(salary.getOtherAllowance());
            db.setOtherDeduction(salary.getOtherDeduction());
            db.setGrossSalary(salary.getGrossSalary());
            db.setNetSalary(salary.getNetSalary());
            db.setTaxAmount(salary.getTaxAmount());
            if (salary.getStatus() != null) {
                db.setStatus(salary.getStatus());
            }
            if (salary.getRemark() != null) {
                db.setRemark(salary.getRemark());
            }
            return this.monthSalaryRepository.save(db);
        }
        return this.monthSalaryRepository.save(salary);
    }

    /**
     * 审批月度薪资（status: 0-草稿 → 1-已核算 → 2-已审批 → 3-已发放）
     */
    @Transactional
    public MonthSalary approveMonthSalary(Long salaryId, Integer targetStatus) {
        MonthSalary salary = this.monthSalaryRepository.findById(salaryId)
                .orElseThrow(() -> new IllegalArgumentException("薪资记录不存在: " + salaryId));
        salary.setStatus(targetStatus);
        return this.monthSalaryRepository.save(salary);
    }

    /**
     * 内部方法：根据 staff_master 的 monthly_salary 同步生成/更新当月 month_salary 记录
     * 将 staff_master.monthly_salary 作为 base_salary 写入 month_salary
     */
    @Transactional
    protected void upsertMonthSalaryForCurrentMonth(StaffMaster staff) {
        String currentMonth = YearMonth.now().toString();
        Optional<MonthSalary> existing = this.monthSalaryRepository
                .findByStaffIdAndSalaryMonth(Long.valueOf(staff.getStaffId()), currentMonth);
        BigDecimal base = staff.getMonthlySalary() != null ? staff.getMonthlySalary() : BigDecimal.ZERO;
        if (existing.isPresent()) {
            MonthSalary db = existing.get();
            db.setBaseSalary(base);
            db.setGrossSalary(base
                    .add(db.getPerformanceSalary() == null ? BigDecimal.ZERO : db.getPerformanceSalary())
                    .add(db.getRewardAmount() == null ? BigDecimal.ZERO : db.getRewardAmount())
                    .add(db.getOtherAllowance() == null ? BigDecimal.ZERO : db.getOtherAllowance()));
            db.setNetSalary(db.getGrossSalary()
                    .subtract(db.getSocialSecurityDeduction() == null ? BigDecimal.ZERO : db.getSocialSecurityDeduction())
                    .subtract(db.getHousingFundDeduction() == null ? BigDecimal.ZERO : db.getHousingFundDeduction()));
            this.monthSalaryRepository.save(db);
        } else {
            MonthSalary ms = new MonthSalary();
            ms.setStoreId(staff.getStoreId());
            ms.setStaffId(Long.valueOf(staff.getStaffId()));
            ms.setSalaryMonth(currentMonth);
            ms.setBaseSalary(base);
            ms.setGrossSalary(base);
            ms.setNetSalary(base);
            ms.setStatus(0);
            ms.setRemark("StaffService 自动同步：staff_master.monthly_salary 变更");
            this.monthSalaryRepository.save(ms);
        }
    }
}

