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

    // ============ 损耗主单 ============

    @GetMapping("/stock-losses")
    public Result<List<StockLoss>> listStockLosses(@RequestParam(defaultValue = "1") Long storeId,
                                                     @RequestParam(required = false) String status) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<StockLoss> list;
            if (status != null && !status.isEmpty()) {
                list = stockLossRepo.findByStoreIdAndStatus(storeId, status);
            } else {
                list = stockLossRepo.findByStoreId(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询损耗单失败: " + e.getMessage());
        }
    }

    @GetMapping("/stock-losses/{id}")
    public Result<Map<String, Object>> getStockLoss(@PathVariable Long id) {
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
            stockLoss.setLossId(null);
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

    @PostMapping("/stock-losses/{id}/approve")
    @Transactional
    public Result<StockLoss> approveStockLoss(@PathVariable Long id,
                                                @RequestParam(required = false) Integer approverId,
                                                @RequestParam(required = false) String approverName,
                                                @RequestParam(required = false) String remark,
                                                @RequestParam(defaultValue = "approved") String status) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            StockLoss existing = stockLossRepo.findById(id).orElse(null);
            if (existing == null) return Result.error(404, "损耗单不存在");
            if (!UserContext.isDataScopeAll()) {
                try { UserContext.assertStoreAccess(existing.getStoreId()); }
                catch (IllegalArgumentException e) { return Result.error(403, "无权限"); }
            }
            existing.setApproverId(approverId);
            existing.setApproverName(approverName);
            existing.setApproveTime(LocalDateTime.now());
            existing.setApproveRemark(remark);
            existing.setStatus(status);
            existing.setUpdatedAt(LocalDateTime.now());
            return Result.success(stockLossRepo.save(existing));
        } catch (Exception e) {
            return Result.error(500, "审批损耗单失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/stock-losses/{id}")
    @Transactional
    public Result<?> deleteStockLoss(@PathVariable Long id) {
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
    public Result<List<StockLossDetail>> listStockLossDetails(@PathVariable Long id) {
        try {
            return Result.success(stockLossDetailRepo.findByLossId(id));
        } catch (Exception e) {
            return Result.error(500, "查询损耗明细失败: " + e.getMessage());
        }
    }

    @PostMapping("/stock-losses/{id}/details")
    @Transactional
    public Result<StockLossDetail> addStockLossDetail(@PathVariable Long id,
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
