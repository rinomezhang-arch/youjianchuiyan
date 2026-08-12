package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.*;
import com.youjian.banquet.repository.*;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工具管理 Controller（统一入口）
 * 表: tool_master / tool_category / tool_issue / tool_return / tool_damage / tool_inventory
 * 路径:
 *   /api/tools            工具台账
 *   /api/tool-categories  工具分类
 *   /api/tool-issues      工具领用
 *   /api/tool-returns     工具归还
 *   /api/tool-damages     工具损坏
 *   /api/tool-inventories 工具盘点
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ToolManagementController {

    @Autowired private ToolMasterRepository toolMasterRepo;
    @Autowired private ToolCategoryRepository toolCategoryRepo;
    @Autowired private ToolIssueRepository toolIssueRepo;
    @Autowired private ToolReturnRepository toolReturnRepo;
    @Autowired private ToolDamageRepository toolDamageRepo;
    @Autowired private ToolInventoryRepository toolInventoryRepo;

    private Long resolveQueryStoreId(Long requestStoreId) {
        Long currentStoreId = UserContext.getCurrentStoreId();
        if (!UserContext.isDataScopeAll() && currentStoreId != null) {
            return currentStoreId;
        }
        return requestStoreId;
    }

    // ============ 工具台账 ============

    @GetMapping("/tools")
    public Result<List<ToolMaster>> listTools(@RequestParam(defaultValue = "1") Long storeId,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) Long categoryId) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<ToolMaster> list;
            if (status != null && !status.isEmpty()) {
                list = toolMasterRepo.findByStoreIdAndStatus(storeId, status);
            } else if (categoryId != null) {
                list = toolMasterRepo.findByStoreIdAndCategoryId(storeId, categoryId);
            } else {
                list = toolMasterRepo.findByStoreId(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询工具列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/tools/{id}")
    public Result<ToolMaster> getTool(@PathVariable Long id) {
        try {
            ToolMaster t = toolMasterRepo.findById(id).orElse(null);
            if (t == null) return Result.error(404, "工具不存在");
            if (t.getStoreId() != null) {
                try { UserContext.assertStoreAccess(t.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            return Result.success(t);
        } catch (Exception e) {
            return Result.error(500, "获取工具失败: " + e.getMessage());
        }
    }

    @PostMapping("/tools")
    @Transactional
    public Result<ToolMaster> createTool(@RequestBody ToolMaster tool) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            if (!UserContext.isDataScopeAll()) {
                tool.setStoreId(UserContext.currentStoreId());
            }
            tool.setToolId(null);
            ToolMaster saved = toolMasterRepo.save(tool);
            return Result.success(saved);
        } catch (Exception e) {
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return Result.error(500, "创建工具失败: " + e.getMessage());
        }
    }

    @PutMapping("/tools/{id}")
    @Transactional
    public Result<ToolMaster> updateTool(@PathVariable Long id, @RequestBody ToolMaster tool) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            ToolMaster existing = toolMasterRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "工具不存在");
            if (!UserContext.isDataScopeAll()) {
                try { UserContext.assertStoreAccess(existing.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            if (tool.getToolName() != null) existing.setToolName(tool.getToolName());
            if (tool.getCategoryId() != null) existing.setCategoryId(tool.getCategoryId());
            if (tool.getSpec() != null) existing.setSpec(tool.getSpec());
            if (tool.getBrand() != null) existing.setBrand(tool.getBrand());
            if (tool.getUnit() != null) existing.setUnit(tool.getUnit());
            if (tool.getUnitPrice() != null) existing.setUnitPrice(tool.getUnitPrice());
            if (tool.getTotalQty() != null) existing.setTotalQty(tool.getTotalQty());
            if (tool.getAvailableQty() != null) existing.setAvailableQty(tool.getAvailableQty());
            if (tool.getStatus() != null) existing.setStatus(tool.getStatus());
            if (tool.getPurchaseDate() != null) existing.setPurchaseDate(tool.getPurchaseDate());
            if (tool.getLocation() != null) existing.setLocation(tool.getLocation());
            if (tool.getRemark() != null) existing.setRemark(tool.getRemark());
            existing.setUpdatedAt(LocalDateTime.now());
            return Result.success(toolMasterRepo.save(existing));
        } catch (Exception e) {
            return Result.error(500, "更新工具失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/tools/{id}")
    @Transactional
    public Result<?> deleteTool(@PathVariable Long id) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            ToolMaster existing = toolMasterRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "工具不存在");
            if (!UserContext.isDataScopeAll()) {
                try { UserContext.assertStoreAccess(existing.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            toolMasterRepo.delete(existing);
            return Result.success("已删除");
        } catch (Exception e) {
            return Result.error(500, "删除工具失败: " + e.getMessage());
        }
    }

    // ============ 工具分类 ============

    @GetMapping("/tool-categories")
    public Result<List<ToolCategory>> listCategories(@RequestParam(required = false) Long parentId) {
        try {
            List<ToolCategory> list;
            if (parentId != null) {
                list = toolCategoryRepo.findByParentId(parentId);
            } else {
                list = toolCategoryRepo.findAll();
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询工具分类失败: " + e.getMessage());
        }
    }

    @PostMapping("/tool-categories")
    @Transactional
    public Result<ToolCategory> createCategory(@RequestBody ToolCategory category) {
        try {
            category.setCategoryId(null);
            return Result.success(toolCategoryRepo.save(category));
        } catch (Exception e) {
            return Result.error(500, "创建分类失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/tool-categories/{id}")
    public Result<?> deleteCategory(@PathVariable Long id) {
        try {
            if (!toolCategoryRepo.existsById(id)) return Result.error(404, "分类不存在");
            toolCategoryRepo.deleteById(id);
            return Result.success("已删除");
        } catch (Exception e) {
            return Result.error(500, "删除分类失败: " + e.getMessage());
        }
    }

    // ============ 工具领用 ============

    @GetMapping("/tool-issues")
    public Result<List<ToolIssue>> listIssues(@RequestParam(defaultValue = "1") Long storeId,
                                                @RequestParam(required = false) String returnStatus,
                                                @RequestParam(required = false) Integer staffId) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<ToolIssue> list;
            if (staffId != null) {
                list = toolIssueRepo.findByStaffId(staffId);
            } else if (returnStatus != null && !returnStatus.isEmpty()) {
                list = toolIssueRepo.findByStoreIdAndReturnStatus(storeId, returnStatus);
            } else {
                list = toolIssueRepo.findByStoreId(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询领用记录失败: " + e.getMessage());
        }
    }

    @PostMapping("/tool-issues")
    @Transactional
    public Result<ToolIssue> createIssue(@RequestBody ToolIssue issue) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            if (!UserContext.isDataScopeAll()) {
                issue.setStoreId(UserContext.currentStoreId());
            }
            issue.setIssueId(null);
            ToolIssue saved = toolIssueRepo.save(issue);
            // 扣减工具可用数量
            ToolMaster tool = toolMasterRepo.findById(saved.getToolId()).orElse(null);
            if (tool != null && tool.getAvailableQty() != null && saved.getQty() != null) {
                tool.setAvailableQty(tool.getAvailableQty().subtract(saved.getQty()));
                tool.setUpdatedAt(LocalDateTime.now());
                toolMasterRepo.save(tool);
            }
            return Result.success(saved);
        } catch (Exception e) {
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return Result.error(500, "创建领用记录失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/tool-issues/{id}")
    @Transactional
    public Result<?> deleteIssue(@PathVariable Long id) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            ToolIssue existing = toolIssueRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "领用记录不存在");
            if (!UserContext.isDataScopeAll()) {
                try { UserContext.assertStoreAccess(existing.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            toolIssueRepo.delete(existing);
            return Result.success("已删除");
        } catch (Exception e) {
            return Result.error(500, "删除领用记录失败: " + e.getMessage());
        }
    }

    // ============ 工具归还 ============

    @GetMapping("/tool-returns")
    public Result<List<ToolReturn>> listReturns(@RequestParam(required = false) Long issueId,
                                                  @RequestParam(required = false) Long toolId) {
        try {
            List<ToolReturn> list;
            if (issueId != null) {
                list = toolReturnRepo.findByIssueId(issueId);
            } else if (toolId != null) {
                list = toolReturnRepo.findByToolId(toolId);
            } else {
                list = toolReturnRepo.findAll();
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询归还记录失败: " + e.getMessage());
        }
    }

    @PostMapping("/tool-returns")
    @Transactional
    public Result<ToolReturn> createReturn(@RequestBody ToolReturn ret) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            ret.setReturnId(null);
            ToolReturn saved = toolReturnRepo.save(ret);
            // 增加工具可用数量
            ToolMaster tool = toolMasterRepo.findById(saved.getToolId()).orElse(null);
            if (tool != null && tool.getAvailableQty() != null && saved.getQty() != null) {
                tool.setAvailableQty(tool.getAvailableQty().add(saved.getQty()));
                tool.setUpdatedAt(LocalDateTime.now());
                toolMasterRepo.save(tool);
            }
            // 更新领用记录的归还状态
            ToolIssue issue = toolIssueRepo.findById(saved.getIssueId()).orElse(null);
            if (issue != null) {
                if (tool != null && tool.getAvailableQty() != null
                        && tool.getTotalQty() != null
                        && tool.getAvailableQty().compareTo(tool.getTotalQty()) >= 0) {
                    issue.setReturnStatus("已归还");
                } else {
                    issue.setReturnStatus("部分归还");
                }
                issue.setUpdatedAt(LocalDateTime.now());
                toolIssueRepo.save(issue);
            }
            return Result.success(saved);
        } catch (Exception e) {
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return Result.error(500, "创建归还记录失败: " + e.getMessage());
        }
    }

    // ============ 工具损坏 ============

    @GetMapping("/tool-damages")
    public Result<List<ToolDamage>> listDamages(@RequestParam(defaultValue = "1") Long storeId,
                                                  @RequestParam(required = false) String status) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<ToolDamage> list;
            if (status != null && !status.isEmpty()) {
                list = toolDamageRepo.findByStoreIdAndStatus(storeId, status);
            } else {
                list = toolDamageRepo.findByStoreId(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询损坏记录失败: " + e.getMessage());
        }
    }

    @PostMapping("/tool-damages")
    @Transactional
    public Result<ToolDamage> createDamage(@RequestBody ToolDamage damage) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            if (!UserContext.isDataScopeAll()) {
                damage.setStoreId(UserContext.currentStoreId());
            }
            damage.setDamageId(null);
            return Result.success(toolDamageRepo.save(damage));
        } catch (Exception e) {
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return Result.error(500, "创建损坏记录失败: " + e.getMessage());
        }
    }

    @PostMapping("/tool-damages/{id}/handle")
    @Transactional
    public Result<ToolDamage> handleDamage(@PathVariable Long id,
                                            @RequestParam(required = false) Long handlerId,
                                            @RequestParam(required = false) String status) {
        try {
            ToolDamage existing = toolDamageRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "损坏记录不存在");
            if (handlerId != null) existing.setHandlerId(handlerId);
            if (status != null) existing.setStatus(status);
            existing.setHandledAt(LocalDateTime.now());
            existing.setUpdatedAt(LocalDateTime.now());
            return Result.success(toolDamageRepo.save(existing));
        } catch (Exception e) {
            return Result.error(500, "处理损坏记录失败: " + e.getMessage());
        }
    }

    // ============ 工具盘点 ============

    @GetMapping("/tool-inventories")
    public Result<List<ToolInventory>> listInventories(@RequestParam(defaultValue = "1") Long storeId,
                                                        @RequestParam(required = false) String status) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<ToolInventory> list;
            if (status != null && !status.isEmpty()) {
                list = toolInventoryRepo.findByStoreIdAndStatus(storeId, status);
            } else {
                list = toolInventoryRepo.findByStoreId(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询盘点记录失败: " + e.getMessage());
        }
    }

    @PostMapping("/tool-inventories")
    @Transactional
    public Result<ToolInventory> createInventory(@RequestBody ToolInventory inventory) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            if (!UserContext.isDataScopeAll()) {
                inventory.setStoreId(UserContext.currentStoreId());
            }
            inventory.setInventoryId(null);
            return Result.success(toolInventoryRepo.save(inventory));
        } catch (Exception e) {
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return Result.error(500, "创建盘点记录失败: " + e.getMessage());
        }
    }
}
