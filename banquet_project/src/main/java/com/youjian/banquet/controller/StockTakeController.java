package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.StockLoss;
import com.youjian.banquet.entity.StockLossDetail;
import com.youjian.banquet.entity.StockTake;
import com.youjian.banquet.entity.StockTakeDetail;
import com.youjian.banquet.repository.StockLossDetailRepository;
import com.youjian.banquet.repository.StockLossRepository;
import com.youjian.banquet.repository.StockTakeDetailRepository;
import com.youjian.banquet.repository.StockTakeRepository;
import com.youjian.banquet.service.InventoryService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存盘点与损耗 Controller
 * 表: stock_take / stock_take_detail / stock_loss / stock_loss_detail
 * 路径:
 *   /api/stock-takes         (盘点)
 *   /api/stock-takes/{id}/details
 *   /api/stock-losses        (损耗)
 *   /api/stock-losses/{id}/details
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class StockTakeController {

    @Autowired private StockTakeRepository stockTakeRepo;
    @Autowired private StockTakeDetailRepository stockTakeDetailRepo;
    @Autowired private StockLossRepository stockLossRepo;
    @Autowired private StockLossDetailRepository stockLossDetailRepo;
    @Autowired private InventoryService inventoryService;

    private Long resolveQueryStoreId(Long requestStoreId) {
        Long currentStoreId = UserContext.getCurrentStoreId();
        if (!UserContext.isDataScopeAll() && currentStoreId != null) {
            return currentStoreId;
        }
        return requestStoreId;
    }

    // ============ 盘点主单 ============

    @GetMapping("/stock-takes")
    public Result<List<StockTake>> listStockTakes(@RequestParam(defaultValue = "1") Long storeId,
                                                    @RequestParam(required = false) String status) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<StockTake> list;
            if (status != null && !status.isEmpty()) {
                list = stockTakeRepo.findByStoreIdAndStatus(storeId, status);
            } else {
                list = stockTakeRepo.findByStoreId(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询盘点单失败: " + e.getMessage());
        }
    }

    @GetMapping("/stock-takes/{id}")
    public Result<Map<String, Object>> getStockTake(@PathVariable Long id) {
        try {
            StockTake st = stockTakeRepo.findById(id).orElse(null);
            if (st == null) return Result.error(404, "盘点单不存在");
            if (st.getStoreId() != null) {
                try { UserContext.assertStoreAccess(st.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            Map<String, Object> result = new HashMap<>();
            result.put("stockTake", st);
            result.put("details", stockTakeDetailRepo.findByTakeId(id));
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "获取盘点单失败: " + e.getMessage());
        }
    }

    @PostMapping("/stock-takes")
    @Transactional
    public Result<StockTake> createStockTake(@RequestBody StockTake stockTake,
                                              @RequestParam(required = false) List<StockTakeDetail> details) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            if (!UserContext.isDataScopeAll()) {
                stockTake.setStoreId(UserContext.currentStoreId());
            }
            stockTake.setTakeId(null);
            StockTake saved = stockTakeRepo.save(stockTake);
            if (details != null && !details.isEmpty()) {
                for (StockTakeDetail d : details) {
                    d.setDetailId(null);
                    d.setTakeId(saved.getTakeId());
                    d.setStoreId(saved.getStoreId());
                    stockTakeDetailRepo.save(d);
                }
            }
            return Result.success(saved);
        } catch (Exception e) {
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return Result.error(500, "创建盘点单失败: " + e.getMessage());
        }
    }

    @PutMapping("/stock-takes/{id}")
    @Transactional
    public Result<StockTake> updateStockTake(@PathVariable Long id, @RequestBody StockTake stockTake) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            StockTake existing = stockTakeRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "盘点单不存在");
            if (!UserContext.isDataScopeAll()) {
                try { UserContext.assertStoreAccess(existing.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            if (stockTake.getStatus() != null) existing.setStatus(stockTake.getStatus());
            if (stockTake.getOperatorName() != null) existing.setOperatorName(stockTake.getOperatorName());
            if (stockTake.getRemark() != null) existing.setRemark(stockTake.getRemark());
            if (stockTake.getTotalItems() != null) existing.setTotalItems(stockTake.getTotalItems());
            if (stockTake.getTotalDiffItems() != null) existing.setTotalDiffItems(stockTake.getTotalDiffItems());
            if (stockTake.getTotalDiffAmount() != null) existing.setTotalDiffAmount(stockTake.getTotalDiffAmount());
            if (stockTake.getFinishTime() != null) existing.setFinishTime(stockTake.getFinishTime());
            existing.setUpdatedAt(LocalDateTime.now());
            return Result.success(stockTakeRepo.save(existing));
        } catch (Exception e) {
            return Result.error(500, "更新盘点单失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/stock-takes/{id}")
    @Transactional
    public Result<?> deleteStockTake(@PathVariable Long id) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            StockTake existing = stockTakeRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "盘点单不存在");
            if (!UserContext.isDataScopeAll()) {
                try { UserContext.assertStoreAccess(existing.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            stockTakeDetailRepo.deleteByTakeId(id);
            stockTakeRepo.delete(existing);
            return Result.success("已删除");
        } catch (Exception e) {
            return Result.error(500, "删除盘点单失败: " + e.getMessage());
        }
    }

    // ============ 盘点明细 ============

    @GetMapping("/stock-takes/{id}/details")
    public Result<List<StockTakeDetail>> listStockTakeDetails(@PathVariable Long id) {
        try {
            return Result.success(stockTakeDetailRepo.findByTakeId(id));
        } catch (Exception e) {
            return Result.error(500, "查询盘点明细失败: " + e.getMessage());
        }
    }

    @PostMapping("/stock-takes/{id}/details")
    @Transactional
    public Result<StockTakeDetail> addStockTakeDetail(@PathVariable Long id,
                                                        @RequestBody StockTakeDetail detail) {
        try {
            StockTake st = stockTakeRepo.findById(id).orElse(null);
            if (st == null) return Result.error(404, "盘点单不存在");
            detail.setDetailId(null);
            detail.setTakeId(id);
            detail.setStoreId(st.getStoreId());
            return Result.success(stockTakeDetailRepo.save(detail));
        } catch (Exception e) {
            return Result.error(500, "新增盘点明细失败: " + e.getMessage());
        }
    }

    // ============ 损耗主单：厨师长审核 -> 店长审批 -> 财务确认 三级流程 ============

    private String generateLossId(java.time.LocalDate date) {
        String prefix = "LS" + date.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = stockLossRepo.countByLossIdStartingWith(prefix) + 1;
        return prefix + String.format("%03d", seq);
    }

    @GetMapping("/stock-losses")
    public Result<List<StockLoss>> listStockLosses(@RequestParam(defaultValue = "1") Long storeId,
                                                     @RequestParam(required = false) String status) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<StockLoss> list;
            if (status != null && !status.isEmpty()) {
                list = stockLossRepo.findByStoreIdAndLossStatus(storeId, status);
            } else {
                list = stockLossRepo.findByStoreId(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询损耗单失败: " + e.getMessage());
        }
    }

    @GetMapping("/stock-losses/{id}")
    public Result<Map<String, Object>> getStockLoss(@PathVariable String id) {
        try {
            StockLoss sl = stockLossRepo.findById(id).orElse(null);
            if (sl == null) return Result.error(404, "损耗单不存在");
            if (sl.getStoreId() != null) {
                try { UserContext.assertStoreAccess(sl.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            Map<String, Object> result = new HashMap<>();
            result.put("stockLoss", sl);
            result.put("details", stockLossDetailRepo.findByLossId(id));
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "获取损耗单失败: " + e.getMessage());
        }
    }

    @PostMapping("/stock-losses")
    @Transactional
    public Result<StockLoss> createStockLoss(@RequestBody StockLoss stockLoss,
                                              @RequestParam(required = false) List<StockLossDetail> details) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            if (!UserContext.isDataScopeAll()) {
                stockLoss.setStoreId(UserContext.currentStoreId());
            }
            if (stockLoss.getLossTime() == null) stockLoss.setLossTime(LocalDateTime.now());
            if (stockLoss.getReporterId() == null) {
                Long staffId = UserContext.getStaffId();
                stockLoss.setReporterId(staffId != null ? staffId.intValue() : null);
            }
            stockLoss.setLossId(generateLossId(stockLoss.getLossTime().toLocalDate()));
            StockLoss saved = stockLossRepo.save(stockLoss);
            if (details != null && !details.isEmpty()) {
                for (StockLossDetail d : details) {
                    d.setDetailId(null);
                    d.setLossId(saved.getLossId());
                    d.setStoreId(saved.getStoreId());
                    stockLossDetailRepo.save(d);
                }
            }
            return Result.success(saved);
        } catch (Exception e) {
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return Result.error(500, "创建损耗单失败: " + e.getMessage());
        }
    }

    /** 第一级：厨师长审核 */
    @PostMapping("/stock-losses/{id}/chef-approve")
    @Transactional
    public Result<StockLoss> chefApproveStockLoss(@PathVariable String id,
                                                    @RequestParam(defaultValue = "通过") String result,
                                                    @RequestParam(required = false) String remark) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            StockLoss existing = stockLossRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "损耗单不存在");
            if (!UserContext.isDataScopeAll()) {
                try { UserContext.assertStoreAccess(existing.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            Long staffId = UserContext.getStaffId();
            existing.setChefManagerId(staffId != null ? staffId.intValue() : null);
            existing.setChefManagerTime(LocalDateTime.now());
            existing.setChefManagerStatus(result);
            existing.setChefManagerRemark(remark);
            existing.setLossStatus("通过".equals(result) ? "店长审批中" : "已驳回");
            return Result.success(stockLossRepo.save(existing));
        } catch (Exception e) {
            return Result.error(500, "厨师长审核失败: " + e.getMessage());
        }
    }

    /** 第二级：店长审批 */
    @PostMapping("/stock-losses/{id}/store-approve")
    @Transactional
    public Result<StockLoss> storeApproveStockLoss(@PathVariable String id,
                                                     @RequestParam(defaultValue = "通过") String result,
                                                     @RequestParam(required = false) String remark) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            StockLoss existing = stockLossRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "损耗单不存在");
            if (!"店长审批中".equals(existing.getLossStatus())) {
                return Result.error(400, "当前状态不允许店长审批：" + existing.getLossStatus());
            }
            if (!UserContext.isDataScopeAll()) {
                try { UserContext.assertStoreAccess(existing.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            Long staffId = UserContext.getStaffId();
            existing.setStoreManagerId(staffId != null ? staffId.intValue() : null);
            existing.setStoreManagerTime(LocalDateTime.now());
            existing.setStoreManagerStatus(result);
            existing.setStoreManagerRemark(remark);
            existing.setLossStatus("通过".equals(result) ? "已通过" : "已驳回");
            return Result.success(stockLossRepo.save(existing));
        } catch (Exception e) {
            return Result.error(500, "店长审批失败: " + e.getMessage());
        }
    }

    /** 第三级：财务确认 —— 通过后实际执行库存出库 */
    @PostMapping("/stock-losses/{id}/finance-confirm")
    @Transactional
    public Result<StockLoss> financeConfirmStockLoss(@PathVariable String id) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            StockLoss existing = stockLossRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "损耗单不存在");
            if (!"已通过".equals(existing.getLossStatus())) {
                return Result.error(400, "当前状态不允许财务确认：" + existing.getLossStatus());
            }
            if (!UserContext.isDataScopeAll()) {
                try { UserContext.assertStoreAccess(existing.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            List<StockLossDetail> details = stockLossDetailRepo.findByLossId(id);
            for (StockLossDetail d : details) {
                com.youjian.banquet.dto.InventoryDTO dto = new com.youjian.banquet.dto.InventoryDTO();
                dto.setIngredientId(d.getIngredientId());
                dto.setStoreId(String.valueOf(existing.getStoreId()));
                dto.setQuantity(d.getLossQuantity());
                dto.setReferenceId(existing.getLossId());
                dto.setReferenceType("STOCK_LOSS");
                dto.setNotes("报损财务确认出库：" + existing.getLossId());
                inventoryService.stockOut(dto);
            }
            Long staffId = UserContext.getStaffId();
            existing.setFinanceConfirmed(true);
            existing.setFinanceConfirmedBy(staffId != null ? staffId.intValue() : null);
            existing.setFinanceConfirmedTime(LocalDateTime.now());
            existing.setLossStatus("已扣减");
            return Result.success(stockLossRepo.save(existing));
        } catch (Exception e) {
            try { TransactionAspectSupport.currentTransactionStatus().setRollbackOnly(); } catch (Exception ignore) {}
            return Result.error(500, "财务确认失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/stock-losses/{id}")
    @Transactional
    public Result<?> deleteStockLoss(@PathVariable String id) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            StockLoss existing = stockLossRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "损耗单不存在");
            if (!UserContext.isDataScopeAll()) {
                try { UserContext.assertStoreAccess(existing.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            stockLossDetailRepo.deleteByLossId(id);
            stockLossRepo.delete(existing);
            return Result.success("已删除");
        } catch (Exception e) {
            return Result.error(500, "删除损耗单失败: " + e.getMessage());
        }
    }

    // ============ 损耗明细 ============

    @GetMapping("/stock-losses/{id}/details")
    public Result<List<StockLossDetail>> listStockLossDetails(@PathVariable String id) {
        try {
            return Result.success(stockLossDetailRepo.findByLossId(id));
        } catch (Exception e) {
            return Result.error(500, "查询损耗明细失败: " + e.getMessage());
        }
    }

    @PostMapping("/stock-losses/{id}/details")
    @Transactional
    public Result<StockLossDetail> addStockLossDetail(@PathVariable String id,
                                                        @RequestBody StockLossDetail detail) {
        try {
            StockLoss sl = stockLossRepo.findById(id).orElse(null);
            if (sl == null) return Result.error(404, "损耗单不存在");
            detail.setDetailId(null);
            detail.setLossId(id);
            detail.setStoreId(sl.getStoreId());
            return Result.success(stockLossDetailRepo.save(detail));
        } catch (Exception e) {
            return Result.error(500, "新增损耗明细失败: " + e.getMessage());
        }
    }
}
