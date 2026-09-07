package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.common.Result;
import com.youjian.banquet.config.WxPayConfig;
import com.youjian.banquet.service.WxPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 宴会定金——微信支付。免登录公开接口（客人还在小程序里，走的是自己的顾客身份，不是员工JWT）。
 * 下单用 openId（客人小程序 wx-login 时拿到的，前端直接带过来，不做额外签名校验——这个 openId
 * 只用来"发起支付"，真正扣不扣钱是客人自己在微信支付弹窗里确认的，跟本地校验松紧无关，风险很低）。
 * 退款走人工流程，不在这个 Controller 里，见 RefundRequestController 注释。
 */
@RestController
@RequestMapping("/api/public/wxpay")
public class WxPayController {

    private static final Logger log = LoggerFactory.getLogger(WxPayController.class);

    @Autowired
    private WxPayConfig config;

    @Autowired
    private WxPayService wxPayService;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/create-deposit-order")
    public Result<Map<String, Object>> createDepositOrder(@RequestBody Map<String, Object> body) {
        if (!config.isConfigured()) {
            return Result.error(500, "支付功能还没配置好，请稍后再试");
        }
        String bookingId = (String) body.get("bookingId");
        String openId = (String) body.get("openId");
        if (bookingId == null || bookingId.isBlank() || openId == null || openId.isBlank()) {
            return Result.error(400, "缺少预定号或微信身份");
        }

        List<Map<String, Object>> bookings = jdbc.queryForList(
                "SELECT booking_id, payment_status FROM booking_master WHERE booking_id = ?", bookingId);
        if (bookings.isEmpty()) {
            return Result.error(404, "预定不存在");
        }
        if ("paid".equals(bookings.get(0).get("payment_status"))) {
            return Result.error(409, "这个预定已经付过定金了");
        }

        List<Map<String, Object>> existing = jdbc.queryForList(
                "SELECT out_trade_no, status FROM booking_payment WHERE booking_id = ?", bookingId);
        String outTradeNo;
        int amountFen = config.getDepositAmountFen();
        if (!existing.isEmpty() && !"closed".equals(existing.get(0).get("status"))) {
            outTradeNo = (String) existing.get(0).get("out_trade_no");
        } else {
            outTradeNo = "DP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6);
            jdbc.update("INSERT INTO booking_payment (booking_id, out_trade_no, amount_fen, open_id, status) VALUES (?,?,?,?,'pending')",
                    bookingId, outTradeNo, amountFen, openId);
        }

        try {
            Map<String, Object> payParams = wxPayService.prepayJsapi(outTradeNo, "又见炊烟-宴会定金-" + bookingId, amountFen, openId);
            payParams.put("outTradeNo", outTradeNo);
            payParams.put("amountFen", amountFen);
            return Result.success(payParams);
        } catch (Exception e) {
            log.error("[WxPay] 创建定金订单失败 bookingId={}", bookingId, e);
            return Result.error(500, "发起支付失败，请稍后重试");
        }
    }

    /** 微信支付异步回调——支付成功后微信服务器主动调用这里，不是客人点出来的 */
    @PostMapping("/notify")
    public Map<String, Object> notify(HttpServletRequest httpRequest, @RequestBody String body) {
        Map<String, Object> resp = new HashMap<>();
        if (!config.isConfigured()) {
            resp.put("code", "FAIL"); resp.put("message", "服务未配置");
            return resp;
        }
        try {
            String serial = httpRequest.getHeader("Wechatpay-Serial");
            String timestamp = httpRequest.getHeader("Wechatpay-Timestamp");
            String nonce = httpRequest.getHeader("Wechatpay-Nonce");
            String signature = httpRequest.getHeader("Wechatpay-Signature");

            if (!wxPayService.verifySignature(serial, timestamp, nonce, body, signature)) {
                log.warn("[WxPay] 回调验签失败，拒绝处理");
                resp.put("code", "FAIL"); resp.put("message", "签名验证失败");
                return resp;
            }

            JsonNode root = objectMapper.readTree(body);
            String eventType = root.has("event_type") ? root.get("event_type").asText() : "";
            if (!"TRANSACTION.SUCCESS".equals(eventType)) {
                resp.put("code", "SUCCESS"); resp.put("message", "成功");
                return resp;
            }
            String resourcePlain = wxPayService.decryptNotifyResource(root.get("resource"));
            JsonNode transaction = objectMapper.readTree(resourcePlain);
            String outTradeNo = transaction.get("out_trade_no").asText();
            String tradeState = transaction.has("trade_state") ? transaction.get("trade_state").asText() : "";
            String transactionId = transaction.has("transaction_id") ? transaction.get("transaction_id").asText() : null;

            if ("SUCCESS".equals(tradeState)) {
                List<Map<String, Object>> rows = jdbc.queryForList(
                        "SELECT booking_id, status FROM booking_payment WHERE out_trade_no = ?", outTradeNo);
                if (!rows.isEmpty() && !"paid".equals(rows.get(0).get("status"))) {
                    String bookingId = (String) rows.get(0).get("booking_id");
                    jdbc.update("UPDATE booking_payment SET status='paid', transaction_id=?, paid_at=? WHERE out_trade_no=?",
                            transactionId, LocalDateTime.now(), outTradeNo);
                    jdbc.update("UPDATE booking_master SET payment_status='paid' WHERE booking_id=?", bookingId);
                    log.info("[WxPay] 定金支付成功 bookingId={} outTradeNo={}", bookingId, outTradeNo);
                }
            }
            resp.put("code", "SUCCESS"); resp.put("message", "成功");
            return resp;
        } catch (Exception e) {
            log.error("[WxPay] 回调处理异常", e);
            resp.put("code", "FAIL"); resp.put("message", "处理失败");
            return resp;
        }
    }

    /** 客人端查询定金支付状态（下单/支付完成后前端轮询或返回页面用） */
    @GetMapping("/status")
    public Result<Map<String, Object>> status(@RequestParam String bookingId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT status, amount_fen, paid_at FROM booking_payment WHERE booking_id = ?", bookingId);
        Map<String, Object> data = new HashMap<>();
        data.put("paid", !rows.isEmpty() && "paid".equals(rows.get(0).get("status")));
        if (!rows.isEmpty()) {
            data.put("amountFen", rows.get(0).get("amount_fen"));
            data.put("paidAt", rows.get(0).get("paid_at"));
        }
        return Result.success(data);
    }
}
