package com.youjian.banquet.service;

import com.youjian.banquet.entity.RewardPunish;
import com.youjian.banquet.repository.RewardPunishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 奖惩服务
 * 对应规划手册 5.txt - 奖惩模块（含两级审批+同步薪资）
 */
@Service
public class RewardPunishService {

    @Autowired private RewardPunishRepository rpRepo;

    /** 列表 */
    public List<RewardPunish> list(Long storeId, Integer finalStatus) {
        if (finalStatus != null) {
            return rpRepo.findByStoreIdAndFinalStatus(storeId, finalStatus);
        }
        return rpRepo.findByStoreId(storeId);
    }

    /** 详情 */
    public RewardPunish get(Long rpId) {
        return rpRepo.findById(rpId).orElse(null);
    }

    /** 新增（自动生成单号） */
    @Transactional
    public RewardPunish create(RewardPunish rp) {
        if (rp.getRpNo() == null || rp.getRpNo().isEmpty()) {
            rp.setRpNo("RP" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        }
        if (rp.getFinalStatus() == null) rp.setFinalStatus(1);
        if (rp.getApprover1Status() == null) rp.setApprover1Status(1);
        if (rp.getApprover2Status() == null) rp.setApprover2Status(1);
        if (rp.getIsSyncedToSalary() == null) rp.setIsSyncedToSalary(0);
        return rpRepo.save(rp);
    }

    /**
     * 两级审批
     * @param level 1-店长级 2-总经理级
     * @param status 2-通过 3-驳回
     */
    @Transactional
    public RewardPunish approve(Long rpId, int level, Long approverId, int status, String remark) {
        RewardPunish rp = rpRepo.findById(rpId).orElse(null);
        if (rp == null) throw new RuntimeException("奖惩单不存在: " + rpId);

        LocalDateTime now = LocalDateTime.now();
        if (level == 1) {
            rp.setApprover1Id(approverId);
            rp.setApprover1Status(status);
            rp.setApprover1Time(now);
            rp.setApprover1Remark(remark);
            // 店长驳回 -> 终审驳回
            if (status == 3) rp.setFinalStatus(3);
            // 店长通过 -> 等待二级审批，final_status 保持 1
        } else if (level == 2) {
            // 二级审批前必须一级已通过
            if (rp.getApprover1Status() == null || rp.getApprover1Status() != 2) {
                throw new RuntimeException("店长尚未审批通过，无法进行总经理审批");
            }
            rp.setApprover2Id(approverId);
            rp.setApprover2Status(status);
            rp.setApprover2Time(now);
            rp.setApprover2Remark(remark);
            // 总经理通过 -> 已生效；驳回 -> 已驳回
            rp.setFinalStatus(status == 2 ? 2 : 3);
        }
        return rpRepo.save(rp);
    }

    /**
     * 同步奖惩到薪资（标记已同步）
     * 实际金额累加到 month_salary 由 SalaryService 处理
     */
    @Transactional
    public void markSynced(Long rpId, Long salaryId) {
        RewardPunish rp = rpRepo.findById(rpId).orElse(null);
        if (rp == null) throw new RuntimeException("奖惩单不存在: " + rpId);
        rp.setIsSyncedToSalary(1);
        rp.setSyncSalaryId(salaryId);
        rpRepo.save(rp);
    }

    /** 查询某员工已生效但未同步薪资的奖惩 */
    public List<RewardPunish> findUnsynced(Long staffId) {
        return rpRepo.findByStaffIdAndFinalStatus(staffId, 2).stream()
                .filter(r -> r.getIsSyncedToSalary() == null || r.getIsSyncedToSalary() == 0)
                .toList();
    }
}
