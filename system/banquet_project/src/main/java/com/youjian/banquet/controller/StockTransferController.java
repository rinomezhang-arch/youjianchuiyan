package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.StockTransfer;
import com.youjian.banquet.service.StockTransferService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 库存调拨单 API。
 * <ul>
 *   <li>GET  /api/inventory/stock-transfer — 按单号/状态/日期范围查询</li>
 *   <li>POST /api/inventory/stock-transfer — 新增调拨单（含可选明细）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/inventory/stock-transfer")
@CrossOrigin(origins = "*")
public class StockTransferController {

    @Autowired
    private StockTransferService stockTransferService;

    /**
     * 查询调拨单列表。
     * 支持参数（均可选）：storeId, transferNo, status, startDate, endDate
     */
    @GetMapping
    public Result<List<Map<String, Object>>> search(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String transferNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            // 门店数据隔离
            Long effectiveStoreId = storeId;
            if (!UserContext.isGeneralManager()) {
                Long current = UserContext.currentStoreId();
                if (current != null && current != 0L) {
                    effectiveStoreId = current;
                }
            }
            List<Map<String, Object>> list = stockTransferService.search(
                    effectiveStoreId, transferNo, status, startDate, endDate);
            return Result.success(list);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "查询调拨单失败: " + e.getMessage());
        }
    }

    /**
     * 新增调拨单。
     * 请求体字段：storeId, transferNo(可选,自动生成), fromStoreId, toStoreId,
     *            ingredientId, quantity, unit, status(默认草稿),
     *            makerName, makeDate, remark, details(可选列表)
     */
    @PostMapping
    public Result<StockTransfer> create(@RequestBody Map<String, Object> body) {
        try {
            // 门店数据隔离
            if (body.get("storeId") != null) {
                Long sid = Long.valueOf(body.get("storeId").toString());
                UserContext.assertStoreAccess(sid);
            } else if (!UserContext.isGeneralManager()) {
                Long current = UserContext.currentStoreId();
                if (current == null || current == 0L) {
                    throw new IllegalArgumentException("缺少 storeId 参数");
                }
                body.put("storeId", String.valueOf(current));
            }
            if (body.get("makerName") == null) {
                String username = UserContext.getUsername();
                if (username != null && !username.isEmpty()) {
                    body.put("makerName", username);
                }
            }
            StockTransfer saved = stockTransferService.create(body);
            return Result.success(saved);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "新增调拨单失败: " + e.getMessage());
        }
    }
}
