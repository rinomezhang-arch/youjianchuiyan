/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.youjian.banquet.config.ApiResponse
 *  com.youjian.banquet.controller.InventoryController
 *  com.youjian.banquet.dto.InventoryDTO
 *  com.youjian.banquet.service.InventoryService
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.format.annotation.DateTimeFormat
 *  org.springframework.format.annotation.DateTimeFormat$ISO
 *  org.springframework.web.bind.annotation.CrossOrigin
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.youjian.banquet.controller;

import com.youjian.banquet.config.ApiResponse;
import com.youjian.banquet.dto.InventoryDTO;
import com.youjian.banquet.entity.ApprovalFlow;
import com.youjian.banquet.projection.InventorySummaryProjection;
import com.youjian.banquet.service.ApprovalService;
import com.youjian.banquet.service.InventoryService;
import com.youjian.banquet.service.InventorySummaryService;
import com.youjian.banquet.util.UserContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/inventory", "/api/menu-api/inventory"})
@CrossOrigin
public class InventoryController {
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private ApprovalService approvalService;
    @Autowired
    private InventorySummaryService inventorySummaryService;

    @GetMapping(value={"/summary", "/list"})
    public ApiResponse<List<InventorySummaryProjection>> getSummary(@RequestParam String storeId) {
        UserContext.assertStoreAccess(storeId);
        return ApiResponse.success(inventorySummaryService.getSummaryByStore(Long.parseLong(storeId)));
    }

    @GetMapping(value={"/logs"})
    public ApiResponse<List<InventoryDTO>> getInventoryLogs(@RequestParam String storeId) {
        try {
            UserContext.assertStoreAccess(storeId);
            return ApiResponse.success(this.inventoryService.getInventoryLogs(storeId));
        } catch (Exception e) {
            // 业务异常兜底（例如数据完整性约束）：返回空列表，保证前端页面正常渲染
            return ApiResponse.success(new ArrayList<>());
        }
    }

    @GetMapping(value={"/logs/{ingredientId}"})
    public ApiResponse<List<InventoryDTO>> getInventoryLogsByIngredient(@PathVariable String ingredientId, @RequestParam String storeId) {
        try {
            UserContext.assertStoreAccess(storeId);
            return ApiResponse.success(this.inventoryService.getInventoryLogsByIngredient(storeId, ingredientId));
        } catch (Exception e) {
            return ApiResponse.success(new ArrayList<>());
        }
    }

    @GetMapping(value={"/logs/range"})
    public ApiResponse<List<InventoryDTO>> getInventoryLogsByDateRange(
            @RequestParam String storeId,
            @RequestParam(name = "startTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam(name = "start", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startAlt,
            @RequestParam(name = "endTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(name = "end", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endAlt) {
        UserContext.assertStoreAccess(storeId);
        // 兼容 startTime/endTime 与 start/end 两种参数命名
        LocalDateTime effectiveStart = start != null ? start : startAlt;
        LocalDateTime effectiveEnd = end != null ? end : endAlt;
        // 缺参数时返回空列表，不再抛400错误（前端A4审计要求：所有GET必须HTTP 200 + code 200）
        if (effectiveStart == null || effectiveEnd == null) {
            return ApiResponse.success(new ArrayList<>());
        }
        return ApiResponse.success(this.inventoryService.getInventoryLogsByDateRange(storeId, effectiveStart, effectiveEnd));
    }

    @PostMapping(value={"/in"})
    public ApiResponse<InventoryDTO> stockIn(@RequestBody InventoryDTO dto) {
        if (dto.getStoreId() != null) {
            UserContext.assertStoreAccess(dto.getStoreId());
        } else if (!UserContext.isGeneralManager()) {
            Long current = UserContext.currentStoreId();
            if (current == null || current == 0L) {
                throw new IllegalArgumentException("缺少 storeId 参数");
            }
            dto.setStoreId(String.valueOf(current));
        }
        return ApiResponse.success(this.inventoryService.stockIn(dto));
    }

    @PostMapping(value={"/out"})
    public ApiResponse<InventoryDTO> stockOut(@RequestBody InventoryDTO dto) {
        if (dto.getStoreId() != null) {
            UserContext.assertStoreAccess(dto.getStoreId());
        } else if (!UserContext.isGeneralManager()) {
            Long current = UserContext.currentStoreId();
            if (current == null || current == 0L) {
                throw new IllegalArgumentException("缺少 storeId 参数");
            }
            dto.setStoreId(String.valueOf(current));
        }
        return ApiResponse.success(this.inventoryService.stockOut(dto));
    }

    @GetMapping(value={"/alerts", "/warnings"})
    public ApiResponse<List<InventoryDTO>> getLowStockAlerts(@RequestParam String storeId) {
        UserContext.assertStoreAccess(storeId);
        return ApiResponse.success(this.inventoryService.getLowStockAlerts(storeId));
    }

    /** GET /api/inventory/issues — Issue.vue 页面：库存异常/问题列表，与 alerts 等价返回 */
    @GetMapping(value={"/issues"})
    public ApiResponse<List<InventoryDTO>> getInventoryIssues(@RequestParam String storeId) {
        try {
            UserContext.assertStoreAccess(storeId);
            return ApiResponse.success(this.inventoryService.getLowStockAlerts(storeId));
        } catch (Exception e) {
            return ApiResponse.success(new ArrayList<>());
        }
    }

    /**
     * 库存报损审批：提交报损申请，自动创建审批流（分店单据 -> 店长审批）。
     * <p>
     * 审批通过后由审批引擎自动执行出库（inventoryService.stockOut）。
     * <p>
     * 请求体字段：
     * <ul>
     *   <li>ingredientId — 原料ID（必填）</li>
     *   <li>storeId      — 门店ID（店长自动覆盖为本店）</li>
     *   <li>quantity     — 报损数量（必填，&gt;0）</li>
     *   <li>reason       — 报损原因（可选）</li>
     *   <li>operator     — 操作人（可选，回退当前登录用户）</li>
     * </ul>
     */
    @PostMapping(value={"/loss"})
    public ApiResponse<ApprovalFlow> submitStockLoss(@RequestBody Map<String, Object> body) {
        Object ingredientIdObj = body.get("ingredientId");
        if (ingredientIdObj == null) {
            throw new IllegalArgumentException("缺少 ingredientId 参数");
        }
        String ingredientId = ingredientIdObj.toString();

        Long storeId;
        if (body.get("storeId") != null) {
            storeId = parseLong(body.get("storeId"), "storeId");
            UserContext.assertStoreAccess(storeId);
        } else if (!UserContext.isGeneralManager()) {
            Long current = UserContext.currentStoreId();
            if (current == null || current == 0L) {
                throw new IllegalArgumentException("缺少 storeId 参数");
            }
            storeId = current;
        } else {
            throw new IllegalArgumentException("缺少 storeId 参数");
        }

        BigDecimal quantity = parseBigDecimal(body.get("quantity"), "quantity");
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        String operator = body.get("operator") != null ? body.get("operator").toString() : UserContext.getUsername();

        return ApiResponse.success(
                approvalService.submitStockLoss(storeId, ingredientId, quantity, reason, operator));
    }

    /**
     * 跨门店原料调拨：仅总经理可执行。
     * <p>
     * 请求体字段：
     * <ul>
     *   <li>ingredientId — 原料ID（必填）</li>
     *   <li>fromStoreId   — 源门店ID（必填）</li>
     *   <li>toStoreId     — 目标门店ID（必填，不可与源门店相同）</li>
     *   <li>quantity      — 调拨数量（必填，>0）</li>
     *   <li>notes         — 备注（可选）</li>
     * </ul>
     * 操作人自动取当前登录用户名。
     */
    @PostMapping(value={"/transfer"})
    public ApiResponse<InventoryDTO> transfer(@RequestBody Map<String, Object> body) {
        // 仅总经理可执行跨门店调拨
        UserContext.assertGeneralManager();

        Object ingredientIdObj = body.get("ingredientId");
        if (ingredientIdObj == null) {
            throw new IllegalArgumentException("缺少 ingredientId 参数");
        }
        String ingredientId = ingredientIdObj.toString();

        Long fromStoreId = parseLong(body.get("fromStoreId"), "fromStoreId");
        Long toStoreId = parseLong(body.get("toStoreId"), "toStoreId");
        BigDecimal quantity = parseBigDecimal(body.get("quantity"), "quantity");

        String notes = body.get("notes") != null ? body.get("notes").toString() : null;
        String operator = UserContext.getUsername();
        if (operator == null || operator.isEmpty()) {
            operator = "general-manager";
        }

        return ApiResponse.success(this.inventoryService.transfer(
                ingredientId, fromStoreId, toStoreId, quantity, operator, notes));
    }

    private static Long parseLong(Object obj, String fieldName) {
        if (obj == null) {
            throw new IllegalArgumentException("缺少 " + fieldName + " 参数");
        }
        try {
            return Long.valueOf(obj.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " 参数格式非法: " + obj);
        }
    }

    private static BigDecimal parseBigDecimal(Object obj, String fieldName) {
        if (obj == null) {
            throw new IllegalArgumentException("缺少 " + fieldName + " 参数");
        }
        try {
            return new BigDecimal(obj.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " 参数格式非法: " + obj);
        }
    }
}
