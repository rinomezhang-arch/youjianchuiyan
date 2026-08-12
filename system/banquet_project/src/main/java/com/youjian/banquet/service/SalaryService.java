package com.youjian.banquet.service;

import com.youjian.banquet.entity.MonthSalary;
import com.youjian.banquet.entity.RewardPunish;
import com.youjian.banquet.entity.SalaryTemplate;
import com.youjian.banquet.entity.StaffMaster;
import com.youjian.banquet.repository.MonthSalaryRepository;
import com.youjian.banquet.repository.SalaryTemplateRepository;
import com.youjian.banquet.repository.StaffMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 薪资服务
 * 对应规划手册 5.txt - 薪资模块（模板CRUD+月度核算+推送财务）
 */
@Service
public class SalaryService {

    @Autowired private SalaryTemplateRepository templateRepo;
    @Autowired private MonthSalaryRepository salaryRepo;
    @Autowired private StaffMasterRepository staffRepo;
    @Autowired private RewardPunishService rewardPunishService;

    // ===== 薪资模板 =====
    public List<SalaryTemplate> listTemplates(Long storeId) {
        return templateRepo.findByStoreIdAndIsActive(storeId, 1);
    }

    @Transactional
    public SalaryTemplate createTemplate(SalaryTemplate t) {
        return templateRepo.save(t);
    }

    @Transactional
    public SalaryTemplate updateTemplate(Long id, SalaryTemplate t) {
        t.setTemplateId(id);
        return templateRepo.save(t);
    }

    @Transactional
    public void deleteTemplate(Long id) {
        templateRepo.deleteById(id);
    }

    // ===== 月度薪资 =====
    public List<MonthSalary> listSalary(Long storeId, String month) {
        if (month != null && !month.isEmpty()) {
            return salaryRepo.findByStoreIdAndSalaryMonth(storeId, month);
        }
        return salaryRepo.findByStoreId(storeId);
    }

    public MonthSalary getSalaryDetail(Long staffId, String month) {
        return salaryRepo.findByStaffIdAndSalaryMonth(staffId, month).orElse(null);
    }

    /**
     * 月度薪资核算（精简版）
     * 计算规则：基本工资 + 加班费(暂0) + 绩效(暂0) + 奖励 - 处罚 - 社保 - 公积金
     * 加班费/绩效需要考勤/绩效数据，后续补全
     */
    @Transactional
    public int calculateMonthlySalary(Long storeId, String month) {
        List<StaffMaster> staffList = staffRepo.findByStoreId(storeId);
        int count = 0;
        for (StaffMaster staff : staffList) {
            // 跳过离职
            if (staff.getEmploymentStatus() != null
                && !"1".equals(staff.getEmploymentStatus())
                && !"在职".equals(staff.getEmploymentStatus())
                && !"试用期".equals(staff.getEmploymentStatus())) {
                continue;
            }

            // 查薪资模板（按岗位）
            String post = staff.getStaffPosition();
            SalaryTemplate template = null;
            if (post != null) {
                template = templateRepo.findByStoreIdAndPostNameAndIsActive(storeId, post, 1).orElse(null);
            }

            BigDecimal baseSalary = template != null ? template.getBaseSalary() :
                    (staff.getMonthlySalary() != null ? staff.getMonthlySalary() : BigDecimal.ZERO);

            // 奖惩累加
            BigDecimal rewardAmount = BigDecimal.ZERO;
            BigDecimal punishDeduction = BigDecimal.ZERO;
            List<RewardPunish> rewards = rewardPunishService.findUnsynced(staff.getStaffId().longValue());
            for (RewardPunish rp : rewards) {
                if (rp.getAmount() == null) continue;
                if (rp.getRpType() != null && rp.getRpType() == 1) {
                    rewardAmount = rewardAmount.add(rp.getAmount());
                } else {
                    punishDeduction = punishDeduction.add(rp.getAmount().abs());
                }
            }

            // 补贴
            BigDecimal allowance = BigDecimal.ZERO;
            BigDecimal deduction = BigDecimal.ZERO;
            if (template != null) {
                allowance = nz(template.getMealSubsidy()).add(nz(template.getTransportSubsidy()))
                        .add(nz(template.getHousingSubsidy())).add(nz(template.getAttendanceBonus()));
                deduction = nz(template.getSocialSecurityEmployee()).add(nz(template.getHousingFundEmployee()));
            }

            BigDecimal gross = baseSalary.add(rewardAmount).add(allowance);
            BigDecimal net = gross.subtract(punishDeduction).subtract(deduction);

            // 已存在则更新
            MonthSalary ms = salaryRepo.findByStaffIdAndSalaryMonth(staff.getStaffId().longValue(), month)
                    .orElseGet(() -> {
                        MonthSalary n = new MonthSalary();
                        n.setStoreId(storeId);
                        n.setStaffId(staff.getStaffId().longValue());
                        n.setSalaryMonth(month);
                        return n;
                    });

            ms.setBaseSalary(baseSalary);
            ms.setRewardAmount(rewardAmount);
            ms.setPunishDeduction(punishDeduction);
            ms.setOtherAllowance(allowance);
            ms.setSocialSecurityDeduction(template != null ? template.getSocialSecurityEmployee() : BigDecimal.ZERO);
            ms.setHousingFundDeduction(template != null ? template.getHousingFundEmployee() : BigDecimal.ZERO);
            ms.setGrossSalary(gross);
            ms.setNetSalary(net);
            ms.setStatus(1); // 已核算
            salaryRepo.save(ms);

            // 标记奖惩已同步
            for (RewardPunish rp : rewards) {
                rewardPunishService.markSynced(rp.getRpId(), ms.getSalaryId());
            }
            count++;
        }
        return count;
    }

    /**
     * 推送薪资到财务（生成应付职工薪酬凭证）
     * TODO: 对接 FinanceService.createVoucher()
     */
    @Transactional
    public void pushToFinance(Long salaryId) {
        MonthSalary ms = salaryRepo.findById(salaryId).orElse(null);
        if (ms == null) throw new RuntimeException("薪资记录不存在: " + salaryId);
        if (ms.getStatus() == null || ms.getStatus() < 1) {
            throw new RuntimeException("薪资尚未核算，无法推送");
        }
        // TODO: 调用 FinanceService 生成凭证
        // financeService.createVoucher("应付职工薪酬", ms.getNetSalary(), ...)
        ms.setStatus(3); // 已发放
        salaryRepo.save(ms);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
