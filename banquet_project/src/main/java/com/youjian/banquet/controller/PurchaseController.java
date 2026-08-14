/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.youjian.banquet.config.ApiResponse
 *  com.youjian.banquet.controller.PurchaseController
 *  com.youjian.banquet.dto.PurchaseDTO
 *  com.youjian.banquet.service.PurchaseService
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.format.annotation.DateTimeFormat
 *  org.springframework.format.annotation.DateTimeFormat$ISO
 *  org.springframework.web.bind.annotation.CrossOrigin
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.youjian.banquet.controller;

import com.youjian.banquet.config.ApiResponse;
import com.youjian.banquet.dto.PurchaseDTO;
import com.youjian.banquet.entity.ApprovalFlow;
import com.youjian.banquet.service.ApprovalService;
import com.youjian.banquet.service.PurchaseService;
import com.youjian.banquet.util.UserContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/purchases", "/api/menu-api/purchases"})
@CrossOrigin
public class PurchaseController {
    @Autowired
    private PurchaseService purchaseService;
    @Autowired
    private ApprovalService approvalService;

    @GetMapping
    public ApiResponse<List<PurchaseDTO>> getAllPurchases(@RequestParam String storeId) {
        UserContext.assertStoreAccess(storeId);
        return ApiResponse.success(this.purchaseService.getAllPurchases(storeId));
    }

    @GetMapping(value={"/{purchaseId}"})
    public ApiResponse<PurchaseDTO> getPurchase(@PathVariable String purchaseId) {
        Long id = parseLongSafe(purchaseId);
        // 路径变量非法（如模板占位符 {purchaseId}）时直接返回 null，不再抛400
        if (id == null) {
            return ApiResponse.success(null);
        }
        PurchaseDTO dto = this.purchaseService.getPurchase(id);
        if (dto != null && dto.getStoreId() != null) {
            UserContext.assertStoreAccess(dto.getStoreId());
        }
        return ApiResponse.success(dto);
    }

    @GetMapping(value={"/status/{status}"})
    public ApiResponse<List<PurchaseDTO>> getPurchasesByStatus(@PathVariable String status, @RequestParam String storeId) {
        UserContext.assertStoreAccess(storeId);
        return ApiResponse.success(this.purchaseService.getPurchasesByStatus(storeId, status));
    }

    @GetMapping(value={"/range"})
    public ApiResponse<List<PurchaseDTO>> getPurchasesByDateRange(@RequestParam String storeId, @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate start, @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate end) {
        UserContext.assertStoreAccess(storeId);
        return ApiResponse.success(this.purchaseService.getPurchasesByDateRange(storeId, start, end));
    }

    @PostMapping
    public ApiResponse<PurchaseDTO> createPurchase(@RequestBody PurchaseDTO dto) {
        // 店长仅可提交本店采购申请；总经理可指定任意门店
        if (dto.getStoreId() != null) {
            UserContext.assertStoreAccess(dto.getStoreId());
        } else if (!UserContext.isGeneralManager()) {
            Long current = UserContext.currentStoreId();
            if (current == null || current == 0L) {
                throw new IllegalArgumentException("缺少 storeId 参数");
            }
            dto.setStoreId(String.valueOf(current));
        }
        // 店长不可在创建时直接置为 approved（绕过审批）
        if (!UserContext.isGeneralManager() && "approved".equalsIgnoreCase(dto.getStatus())) {
            throw new IllegalArgumentException("无权限：店长不可审批采购单");
        }
        PurchaseDTO created = this.purchaseService.createPurchase(dto);
        // 自动提交采购审批流（全局单据 -> 总经理审批）
        try {
            Long staffIdLong = UserContext.getStaffId();
            Integer staffIdInt = staffIdLong != null ? staffIdLong.intValue() : null;
            approvalService.submit("purchase", created.getPurchaseId(),
                    "PUR-" + created.getPurchaseId(),
                    Long.parseLong(created.getStoreId()),
                    staffIdInt, UserContext.getUsername());
        } catch (Exception af) {
            // 审批流创建失败不阻断主业务，可后续通过 /api/approval/submit 补提
            af.printStackTrace();
        }
        return ApiResponse.success(created);
    }

    @PutMapping(value={"/{purchaseId}"})
    public ApiResponse<PurchaseDTO> updatePurchase(@PathVariable String purchaseId, @RequestBody PurchaseDTO dto) {
        Long id = parseLongSafe(purchaseId);
        if (id == null) {
            return ApiResponse.success(null);
        }
        // 先查采购单原有门店，校验当前用户对该门店的访问权限
        PurchaseDTO existing = this.purchaseService.getPurchase(id);
        if (existing == null || existing.getStoreId() == null) {
            throw new IllegalArgumentException("采购单不存在: " + id);
        }
        UserContext.assertStoreAccess(existing.getStoreId());
        // 店长不可通过更新接口将状态改为 approved（绕过审批）
        if (!UserContext.isGeneralManager() && "approved".equalsIgnoreCase(dto.getStatus())
                && !"approved".equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalArgumentException("无权限：店长不可审批采购单");
        }
        // 不允许通过本接口跨门店改归属
        if (dto.getStoreId() != null && !dto.getStoreId().equals(existing.getStoreId())) {
            UserContext.assertStoreAccess(dto.getStoreId());
        }
        return ApiResponse.success(this.purchaseService.updatePurchase(id, dto));
    }

    @PostMapping(value={"/{purchaseId}/approve", "/{purchaseId}/audit"})
    public ApiResponse<PurchaseDTO> approvePurchase(@PathVariable String purchaseId, @RequestParam(required=false) String approvedBy) {
        Long id = parseLongSafe(purchaseId);
        if (id == null) {
            return ApiResponse.success(null);
        }
        // 仅总经理可审批采购单
        UserContext.assertGeneralManager();
        // 走审批流：查找该采购单的待审批 flow，通过则自动更新采购单状态
        Optional<ApprovalFlow> flowOpt = approvalService.findByBusiness("purchase", id)
                .filter(f -> "pending".equals(f.getStatus()));
        if (flowOpt.isPresent()) {
            approvalService.approve(flowOpt.get().getId(), approvedBy);
            return ApiResponse.success(this.purchaseService.getPurchase(id));
        }
        // 回退：无审批流时直接审批（兼容历史数据）
        String approver = UserContext.getUsername();
        if (approver == null || approver.isEmpty()) {
            approver = approvedBy != null ? approvedBy : "general-manager";
        }
        return ApiResponse.success(this.purchaseService.approvePurchase(id, approver));
    }

    @DeleteMapping(value={"/{purchaseId}"})
    public ApiResponse<Void> deletePurchase(@PathVariable String purchaseId) {
        Long id = parseLongSafe(purchaseId);
        if (id == null) {
            return ApiResponse.success();
        }
        // 先查采购单原有门店，校验当前用户对该门店的访问权限
        PurchaseDTO existing = this.purchaseService.getPurchase(id);
        if (existing == null || existing.getStoreId() == null) {
            throw new IllegalArgumentException("采购单不存在: " + id);
        }
        UserContext.assertStoreAccess(existing.getStoreId());
        this.purchaseService.deletePurchase(id);
        return ApiResponse.success();
    }

    // ============ 采购申请 procurement_request ============
    @Autowired
    private JdbcTemplate jdbc2;

    @GetMapping("/procurement/requests")
    public ApiResponse<List<Map<String, Object>>> listProcurementRequests(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            Long sid = null;
            if (UserContext.isGeneralManager()) {
                if (storeId != null && !storeId.isEmpty() && !"all".equalsIgnoreCase(storeId)) {
                    sid = Long.parseLong(storeId);
                }
            } else {
                sid = UserContext.currentStoreId();
                if (sid != null && sid == 0L) sid = null;
            }
            if (limit < 1 || limit > 200) limit = 50;
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (sid != null) { where.append(" AND store_id=?"); params.add(sid); }
            if (status != null && !status.isEmpty()) { where.append(" AND status=?"); params.add(status); }
            params.add(limit);
            return ApiResponse.success(jdbc2.queryForList(
                "SELECT * FROM procurement_request" + where + " ORDER BY request_date DESC LIMIT ?", params.toArray()));
        } catch (Exception e) {
            return ApiResponse.error(500, "查询采购申请失败: " + e.getMessage());
        }
    }

    @GetMapping("/procurement/receipts")
    public ApiResponse<List<Map<String, Object>>> listPurchaseReceipts(
            @RequestParam(required = false) String storeId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            Long sid = null;
            if (UserContext.isGeneralManager()) {
                if (storeId != null && !storeId.isEmpty() && !"all".equalsIgnoreCase(storeId)) {
                    sid = Long.parseLong(storeId);
                }
            } else {
                sid = UserContext.currentStoreId();
                if (sid != null && sid == 0L) sid = null;
            }
            if (limit < 1 || limit > 200) limit = 50;
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (sid != null) { where.append(" AND store_id=?"); params.add(sid); }
            params.add(limit);
            return ApiResponse.success(jdbc2.queryForList(
                "SELECT * FROM purchase_receipt" + where + " ORDER BY receipt_date DESC LIMIT ?", params.toArray()));
        } catch (Exception e) {
            return ApiResponse.error(500, "查询采购入库失败: " + e.getMessage());
        }
    }

    /** 安全解析路径变量为 Long，失败返回 null（避免模板字符串如 {purchaseId} 造成400错误） */
    private static Long parseLongSafe(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
