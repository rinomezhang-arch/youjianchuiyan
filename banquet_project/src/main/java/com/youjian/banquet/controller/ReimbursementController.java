package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.Reimbursement;
import com.youjian.banquet.repository.ReimbursementRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报销管理 Controller
 * 表: reimbursement
 * 路径: /api/reimbursements
 */
@RestController
@RequestMapping("/api/reimbursements")
@CrossOrigin(origins = "*")
public class ReimbursementController {

    @Autowired
    private ReimbursementRepository reimbursementRepo;

    private Long resolveQueryStoreId(Long requestStoreId) {
        Long currentStoreId = UserContext.getCurrentStoreId();
        if (!UserContext.isDataScopeAll() && currentStoreId != null) {
            return currentStoreId;
        }
        return requestStoreId;
    }

    @GetMapping
    public Result<List<Reimbursement>> list(@RequestParam(defaultValue = "1") Long storeId,
                                             @RequestParam(required = false) String status) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<Reimbursement> list;
            if (status != null && !status.isEmpty()) {
                list = reimbursementRepo.findByStoreIdAndStatus(storeId, status);
            } else {
                list = reimbursementRepo.findByStoreId(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询报销列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Result<Reimbursement> detail(@PathVariable Long id) {
        try {
            Reimbursement r = reimbursementRepo.findById(id).orElse(null);
            if (r == null) return Result.error(404, "报销单不存在");
            if (r.getStoreId() != null) {
                try {
                    UserContext.assertStoreAccess(r.getStoreId());
                } catch (IllegalArgumentException e) {
                    return Result.error(403, "无权限：仅可查看本店报销单");
                }
            }
            return Result.success(r);
        } catch (Exception e) {
            return Result.error(500, "获取报销单失败: " + e.getMessage());
        }
    }

    @PostMapping
    @Transactional
    public Result<Reimbursement> create(@RequestBody Reimbursement reimbursement) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            if (!UserContext.isDataScopeAll()) {
                reimbursement.setStoreId(UserContext.currentStoreId());
            }
            reimbursement.setReimbursementId(null);
            if (reimbursement.getStatus() == null) reimbursement.setStatus("pending");
            Reimbursement saved = reimbursementRepo.save(reimbursement);
            return Result.success(saved);
        } catch (Exception e) {
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return Result.error(500, "创建报销单失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public Result<Reimbursement> update(@PathVariable Long id, @RequestBody Reimbursement reimbursement) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            Reimbursement existing = reimbursementRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "报销单不存在");
            if (!UserContext.isDataScopeAll()) {
                try {
                    UserContext.assertStoreAccess(existing.getStoreId());
                } catch (IllegalArgumentException e) {
                    return Result.error(403, "无权限：仅可更新本店报销单");
                }
            }
            if (reimbursement.getApplicantName() != null) existing.setApplicantName(reimbursement.getApplicantName());
            if (reimbursement.getDepartmentName() != null) existing.setDepartmentName(reimbursement.getDepartmentName());
            if (reimbursement.getReimburseDate() != null) existing.setReimburseDate(reimbursement.getReimburseDate());
            if (reimbursement.getTotalAmount() != null) existing.setTotalAmount(reimbursement.getTotalAmount());
            if (reimbursement.getStatus() != null) existing.setStatus(reimbursement.getStatus());
            if (reimbursement.getReimburseType() != null) existing.setReimburseType(reimbursement.getReimburseType());
            if (reimbursement.getPurpose() != null) existing.setPurpose(reimbursement.getPurpose());
            if (reimbursement.getRemark() != null) existing.setRemark(reimbursement.getRemark());
            existing.setUpdatedAt(LocalDateTime.now());
            Reimbursement saved = reimbursementRepo.save(existing);
            return Result.success(saved);
        } catch (Exception e) {
            return Result.error(500, "更新报销单失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public Result<Reimbursement> approve(@PathVariable Long id,
                                          @RequestParam(required = false) Integer approverId,
                                          @RequestParam(required = false) String approverName,
                                          @RequestParam(required = false) String comment,
                                          @RequestParam(defaultValue = "approved") String status) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            Reimbursement existing = reimbursementRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "报销单不存在");
            if (!UserContext.isDataScopeAll()) {
                try {
                    UserContext.assertStoreAccess(existing.getStoreId());
                } catch (IllegalArgumentException e) {
                    return Result.error(403, "无权限");
                }
            }
            existing.setApproverId(approverId);
            existing.setApproverName(approverName);
            existing.setApproveTime(LocalDateTime.now());
            existing.setApproveComment(comment);
            existing.setStatus(status);
            existing.setUpdatedAt(LocalDateTime.now());
            return Result.success(reimbursementRepo.save(existing));
        } catch (Exception e) {
            return Result.error(500, "审批报销单失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<?> delete(@PathVariable Long id) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            Reimbursement existing = reimbursementRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "报销单不存在");
            if (!UserContext.isDataScopeAll()) {
                try {
                    UserContext.assertStoreAccess(existing.getStoreId());
                } catch (IllegalArgumentException e) {
                    return Result.error(403, "无权限：仅可删除本店报销单");
                }
            }
            reimbursementRepo.delete(existing);
            return Result.success("已删除");
        } catch (Exception e) {
            return Result.error(500, "删除报销单失败: " + e.getMessage());
        }
    }
}
