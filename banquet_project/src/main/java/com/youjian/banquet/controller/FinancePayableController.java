package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.FinancePayable;
import com.youjian.banquet.service.FinancePayableService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 供应商应付管理。对应 GET/POST /api/finance/payables。
 * <p>POST 兼具新增与结算：body 含 payableId + settleAmount 时为结算，否则为新增。
 * 与 FinanceController 的 /api/finance/payable(JdbcTemplate 版) 共存。
 */
@RestController
@RequestMapping("/api/finance/payables")
@CrossOrigin(origins = "*")
public class FinancePayableController {

    @ExceptionHandler(FinancePayableService.PayableAccessDeniedException.class)
    public org.springframework.http.ResponseEntity<Result<Void>> accessDenied(
            FinancePayableService.PayableAccessDeniedException error) {
        return org.springframework.http.ResponseEntity.status(403).body(Result.error(403,error.getMessage()));
    }

    /**
     * 幂等键冲突走 409，不走 400。
     * <p>
     * 400 的含义是"你这个请求本身写错了"，而幂等冲突是"请求没问题，但和已经发生的另一笔撞上了"，
     * 调用方的处置方式完全不同：前者改参数，后者要么沿用原参数当重试、要么换新 key。
     * 这两个异常继承自 IllegalArgumentException，若不单独处理会被下面的 400 处理器吃掉。
     */
    @ExceptionHandler(FinancePayableService.SettlementConflictException.class)
    public org.springframework.http.ResponseEntity<Result<Void>> settlementConflict(
            FinancePayableService.SettlementConflictException error) {
        return org.springframework.http.ResponseEntity.status(409).body(Result.error(409, error.getMessage()));
    }

    /** 同键创建在途：本次未建单，用同一 requestId 重试可取回原回执。冲突语义，409。 */
    @ExceptionHandler(FinancePayableService.CreateInFlightException.class)
    public org.springframework.http.ResponseEntity<Result<Void>> createInFlight(
            FinancePayableService.CreateInFlightException error) {
        return org.springframework.http.ResponseEntity.status(409).body(Result.error(409, error.getMessage()));
    }

    /** 并发撞锁：本次未记账，可用同一 requestId 安全重试。同样是冲突语义，用 409。 */
    @ExceptionHandler(FinancePayableService.SettlementRetryableException.class)
    public org.springframework.http.ResponseEntity<Result<Void>> settlementRetryable(
            FinancePayableService.SettlementRetryableException error) {
        return org.springframework.http.ResponseEntity.status(409).body(Result.error(409, error.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public org.springframework.http.ResponseEntity<Result<Void>> invalidRequest(IllegalArgumentException error) {
        String message=error instanceof NumberFormatException ? "单据编号或金额格式不正确" : error.getMessage();
        return org.springframework.http.ResponseEntity.badRequest().body(Result.error(400,message));
    }

    @Autowired
    private FinancePayableService financePayableService;

    /**
     * 解析本次查询的门店，<b>fail-closed</b>。
     * <p>
     * 原实现有两个口子：非总经理解析不出门店时回退成 1 号店——等于把别人的应付单给了他；
     * 总经理的 storeId 参数带 {@code defaultValue = "1"}，不传也静默按 1 号店查。
     * 范围判定宁可拒绝也不能放大，这两个回退都去掉。
     */
    private Long resolveStoreId(Long requested) {
        if (UserContext.get() == null) throw new FinancePayableService.PayableAccessDeniedException();
        if (requested != null && requested <= 0L) throw new IllegalArgumentException("请选择有效门店");
        if (UserContext.isGeneralManager()) {
            if (requested == null || requested <= 0L) {
                throw new IllegalArgumentException("总经理查询应付单必须显式指定有效门店");
            }
            return requested;
        }
        Long sid = UserContext.currentStoreId();
        if (sid == null || sid <= 0L) {
            throw new FinancePayableService.PayableAccessDeniedException();
        }
        // 非总经理传了别的门店：直接拒绝，不静默改成本店，免得调用方以为查到的是自己要的那家
        if (requested != null && !sid.equals(requested)) {
            throw new FinancePayableService.PayableAccessDeniedException();
        }
        return sid;
    }

    /** 新增应付时的门店解析，与查询口径一致：宁可拒绝，不静默改写。 */
    private Long resolveCreateStoreId(Object requestedRaw) {
        Long requested = requestedRaw == null ? null : Long.valueOf(requestedRaw.toString());
        if (UserContext.isGeneralManager()) {
            if (requested == null || requested <= 0L) {
                throw new IllegalArgumentException("总经理新增应付单必须显式指定有效门店");
            }
            return requested;
        }
        Long own = UserContext.currentStoreId();
        if (own == null || own <= 0L) {
            throw new FinancePayableService.PayableAccessDeniedException();
        }
        if (requested != null && requested > 0L && !own.equals(requested)) {
            throw new FinancePayableService.PayableAccessDeniedException();
        }
        return own;
    }

    @GetMapping
    public Result<List<FinancePayable>> list(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status) {
        return Result.success(financePayableService.list(resolveStoreId(storeId), supplierId, status));
    }

    /** 某张应付单的结算流水，供前端展示"这笔钱分几次结的、谁结的"。 */
    @GetMapping("/{payableId}/settlements")
    public Result<List<com.youjian.banquet.entity.PayableSettlementRecord>> settlements(
            @PathVariable Long payableId) {
        return Result.success(financePayableService.settlements(payableId));
    }

    @PostMapping
    public Result<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        Object payableIdObj = body.get("payableId");
        if (payableIdObj != null) {
            // 结算分支：body 含 payableId 即为结算，必须带 requestId 幂等键。
            Long payableId = Long.parseLong(payableIdObj.toString());
            BigDecimal settleAmount = body.get("settleAmount") != null
                    ? new BigDecimal(body.get("settleAmount").toString())
                    : null;
            Object requestIdObj = body.get("requestId");
            FinancePayableService.SettlementResult result = financePayableService.settle(
                    payableId, settleAmount, requestIdObj == null ? null : requestIdObj.toString());
            java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("payableId", result.payable.getPayableId());
            data.put("settlementNo", result.record.getSettlementNo());
            data.put("settleAmount", result.record.getSettleAmount());
            data.put("paidAmount", result.record.getPaidAfter());
            data.put("pendingAmount", result.record.getPendingAfter());
            data.put("status", result.record.getStatusAfter());
            // replayed=true 表示这次是重试，返回的是原来那一笔，没有产生新结算。
            data.put("replayed", result.replayed);
            data.put("message", result.replayed
                    ? "该请求此前已结算，返回原结算记录，未重复记账"
                    : "结算记账完成。这是账务记录，不代表银行已付款");
            return Result.success(data);
        }

        FinancePayable payable = new FinancePayable();
        // 门店解析与列表口径保持一致：非总经理显式指定了别的门店时**明确拒绝**，
        // 不静默改写成本店——否则调用方以为给 1 号店建了单，实际建在了自己店里，
        // 事后对账才发现，比直接报错难查得多。
        payable.setStoreId(resolveCreateStoreId(body.get("storeId")));
        if (body.get("supplierName") != null) payable.setSupplierName(body.get("supplierName").toString());
        if (body.get("payableNo") != null) payable.setPayableNo(body.get("payableNo").toString());
        // 原来这里完全不解析日期：客户端传了 payableDate/dueDate 也被静默丢掉，
        // 落库永远是"今天"，而旧路由 /api/finance/payable 是解析的 —— 两条入口口径不一致，
        // 同一份单据从不同入口进来会得到不同的应付日期。
        if (body.get("payableDate") != null) {
            payable.setPayableDate(java.time.LocalDate.parse(body.get("payableDate").toString()));
        }
        if (body.get("dueDate") != null) {
            payable.setDueDate(java.time.LocalDate.parse(body.get("dueDate").toString()));
        }
        if (body.get("supplierId") != null) {
            payable.setSupplierId(Integer.valueOf(body.get("supplierId").toString()));
        }
        if (body.get("totalAmount") != null) {
            payable.setTotalAmount(new BigDecimal(body.get("totalAmount").toString()));
        }
        if (body.get("paidAmount") != null) {
            payable.setPaidAmount(new BigDecimal(body.get("paidAmount").toString()));
        }
        if (body.get("status") != null) {
            payable.setStatus(body.get("status").toString());
        }
        if (body.get("remark") != null) {
            payable.setRemark(body.get("remark").toString());
        }
        // 新增分支同样要幂等键：没有它，超时重发和手抖点两次都会多出一张单。
        Object createRequestId = body.get("requestId");
        FinancePayableService.CreateResult created = financePayableService.create(
                payable, createRequestId == null ? null : createRequestId.toString());
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("requestId", created.requestId);
        data.put("payableId", created.payable.getPayableId());
        data.put("payableNo", created.payable.getPayableNo());
        data.put("storeId", created.payable.getStoreId());
        data.put("totalAmount", created.payable.getTotalAmount());
        data.put("status", created.payable.getStatus());
        // replayed=true 表示这次是重试，返回的是原来那张单，没有新建。前端据此避免提示两次"创建成功"。
        data.put("replayed", created.replayed);
        data.put("message", created.replayed
                ? "该请求此前已创建过应付单，返回原单回执，未重复新建"
                : "应付单已创建");
        return Result.success(data);
    }
}
