package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.AuditLog;
import com.youjian.banquet.entity.ChangeLog;
import com.youjian.banquet.repository.AuditLogRepository;
import com.youjian.banquet.repository.ChangeLogRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * 操作日志 Controller（变更日志 + 审计日志）
 * 表: change_log / audit_logs
 * 路径:
 *   /api/change-logs   变更日志
 *   /api/audit-logs    审计日志
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class OperationLogController {

    @Autowired private ChangeLogRepository changeLogRepo;
    @Autowired private AuditLogRepository auditLogRepo;

    private Long resolveQueryStoreId(Long requestStoreId) {
        Long currentStoreId = UserContext.getCurrentStoreId();
        if (!UserContext.isDataScopeAll() && currentStoreId != null) {
            return currentStoreId;
        }
        return requestStoreId;
    }

    // ============ 变更日志 ============

    @GetMapping("/change-logs")
    public Result<List<ChangeLog>> listChangeLogs(@RequestParam(defaultValue = "1") Long storeId,
                                                    @RequestParam(required = false) String operationType,
                                                    @RequestParam(required = false) String targetType) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<ChangeLog> list = changeLogRepo.findByStoreIdOrderByCreatedAtDesc(storeId);
            // 内存过滤（简单实现）
            if (operationType != null && !operationType.isEmpty()) {
                list.removeIf(l -> !operationType.equals(l.getOperationType()));
            }
            if (targetType != null && !targetType.isEmpty()) {
                list.removeIf(l -> !targetType.equals(l.getTargetType()));
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询变更日志失败: " + e.getMessage());
        }
    }

    @GetMapping("/change-logs/{id}")
    public Result<ChangeLog> getChangeLog(@PathVariable Long id) {
        try {
            ChangeLog log = changeLogRepo.findById(id).orElse(null);
            if (log == null) return Result.error(404, "日志不存在");
            return Result.success(log);
        } catch (Exception e) {
            return Result.error(500, "获取日志失败: " + e.getMessage());
        }
    }

    @PostMapping("/change-logs")
    public Result<ChangeLog> createChangeLog(@RequestBody ChangeLog log) {
        try {
            log.setLogId(null);
            return Result.success(changeLogRepo.save(log));
        } catch (Exception e) {
            return Result.error(500, "创建日志失败: " + e.getMessage());
        }
    }

    // ============ 审计日志 ============

    /** 前端别名：AuditLog.vue 调用 /api/audit/logs */
    @GetMapping("/audit/logs")
    public Result<List<AuditLog>> listAuditLogsAlias(@RequestParam(defaultValue = "1") Long storeId,
                                                      @RequestParam(required = false) String userId,
                                                      @RequestParam(required = false) String action) {
        return listAuditLogs(storeId, userId, action);
    }

    @GetMapping("/audit-logs")
    public Result<List<AuditLog>> listAuditLogs(@RequestParam(defaultValue = "1") Long storeId,
                                                  @RequestParam(required = false) String userId,
                                                  @RequestParam(required = false) String action) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<AuditLog> list;
            if (userId != null && !userId.isEmpty()) {
                list = auditLogRepo.findByUserId(userId);
            } else if (action != null && !action.isEmpty()) {
                list = auditLogRepo.findByAction(action);
            } else {
                list = auditLogRepo.findByStoreId(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询审计日志失败: " + e.getMessage());
        }
    }

    @PostMapping("/audit-logs")
    public Result<AuditLog> createAuditLog(@RequestBody AuditLog log) {
        try {
            log.setId(null);
            if (log.getCreatedAt() == null) {
                log.setCreatedAt(Instant.now().getEpochSecond());
            }
            return Result.success(auditLogRepo.save(log));
        } catch (Exception e) {
            return Result.error(500, "创建审计日志失败: " + e.getMessage());
        }
    }
}
