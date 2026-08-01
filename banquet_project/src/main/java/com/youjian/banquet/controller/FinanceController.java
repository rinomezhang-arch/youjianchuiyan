package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 财务模块控制器（真实业务接口，对接 11 张财务表）。
 * <p>
 * 数据隔离规则同 MemberController：
 * <ul>
 *   <li>总经理（store_id = 0）：可查询全门店汇总</li>
 *   <li>店长（store_id &gt; 0）：仅查询本店数据</li>
 * </ul>
 * <p>
 * 所有数字均从 finance_* 表真实聚合，禁止任何硬编码、随机数、按比例推算。
 */
@RestController
@RequestMapping("/api/finance")
@CrossOrigin(origins = "*")
public class FinanceController {

    @Autowired
    private JdbcTemplate jdbc;

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private Long resolveQueryStoreId(String storeId) {
        if (UserContext.isGeneralManager()) {
            if (storeId == null || storeId.isEmpty() || "all".equalsIgnoreCase(storeId)) {
                return null;
            }
            try {
                return Long.parseLong(storeId);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? null : sid;
    }

    @GetMapping("/today")
    public Result<Map<String, Object>> getTodayFinance(@RequestParam(required = false) String storeId) {
        try {
            Long sid = resolveQueryStoreId(storeId);
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (sid != null) { where.append(" AND store_id = ?"); params.add(sid); }

            StringBuilder payWhere = new StringBuilder(where);
            payWhere.append(" AND payment_date = ?");
            params.add(Date.valueOf(today));
            BigDecimal todayRevenue = sumOrZero(
                    "SELECT COALESCE(SUM(amount),0) FROM finance_payment_record" + payWhere,
                    params.toArray());

            List<Object> onlineParams = new ArrayList<>(params);
            BigDecimal online = sumOrZero(
                    "SELECT COALESCE(SUM(amount),0) FROM finance_payment_record" + payWhere
                            + " AND payment_method IN ('wechat','alipay')",
                    onlineParams.toArray());
            BigDecimal offline = todayRevenue.subtract(online);

            List<Object> yParams = new ArrayList<>();
            StringBuilder yWhere = new StringBuilder(" WHERE 1=1");
            if (sid != null) { yWhere.append(" AND store_id = ?"); yParams.add(sid); }
            yWhere.append(" AND payment_date = ?");
            yParams.add(Date.valueOf(yesterday));
            BigDecimal yesterdayRevenue = sumOrZero(
                    "SELECT COALESCE(SUM(amount),0) FROM finance_payment_record" + yWhere,
                    yParams.toArray());
            double trendPct = yesterdayRevenue.signum() == 0 ? 0.0
                    : todayRevenue.subtract(yesterdayRevenue)
                            .divide(yesterdayRevenue.abs(), 4, java.math.RoundingMode.HALF_UP)
                            .doubleValue() * 100.0;

            List<Object> costParams = new ArrayList<>();
            StringBuilder costWhere = new StringBuilder(" WHERE 1=1");
            if (sid != null) { costWhere.append(" AND store_id = ?"); costParams.add(sid); }
            costWhere.append(" AND cost_date = ?");
            costParams.add(Date.valueOf(today));
            BigDecimal todayCost = sumOrZero(
                    "SELECT COALESCE(SUM(amount),0) FROM finance_cost_record" + costWhere,
                    costParams.toArray());
            BigDecimal grossProfit = todayRevenue.subtract(todayCost);
            double grossMarginRate = todayRevenue.signum() == 0 ? 0.0
                    : grossProfit.divide(todayRevenue, 4, java.math.RoundingMode.HALF_UP)
                            .doubleValue() * 100.0;

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("todayRevenue", todayRevenue);
            data.put("onlineRevenue", online);
            data.put("offlineRevenue", offline);
            data.put("todayCost", todayCost);
            data.put("grossProfit", grossProfit);
            data.put("grossMarginRate", Math.round(grossMarginRate * 10.0) / 10.0);
            data.put("yesterdayRevenue", yesterdayRevenue);
            data.put("trendPct", Math.round(trendPct * 10.0) / 10.0);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "查询今日财务概览失败: " + e.getMessage());
        }
    }

    @GetMapping("/balance")
    public Result<Map<String, Object>> getBalance(@RequestParam(required = false) String storeId) {
        try {
            Long sid = resolveQueryStoreId(storeId);
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (sid != null) { where.append(" AND store_id = ?"); params.add(sid); }

            BigDecimal fundBalance = sumOrZero(
                    "SELECT COALESCE(SUM(current_balance),0) FROM finance_account" + where
                            + " AND is_active = 1",
                    params.toArray());
            BigDecimal receivable = sumOrZero(
                    "SELECT COALESCE(SUM(pending_amount),0) FROM finance_receivable" + where,
                    params.toArray());
            BigDecimal payable = sumOrZero(
                    "SELECT COALESCE(SUM(pending_amount),0) FROM finance_payable" + where,
                    params.toArray());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("fundBalance", fundBalance);
            data.put("receivable", receivable);
            data.put("payable", payable);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "查询资金状况失败: " + e.getMessage());
        }
    }

    @GetMapping("/monthly-trend")
    public Result<List<Map<String, Object>>> getMonthlyTrend(@RequestParam(required = false) String storeId) {
        try {
            Long sid = resolveQueryStoreId(storeId);
            List<Map<String, Object>> result = new ArrayList<>();
            LocalDate now = LocalDate.now();
            for (int i = 5; i >= 0; i--) {
                LocalDate month = now.minusMonths(i).withDayOfMonth(1);
                String monthKey = month.format(MONTH_FMT);
                Date start = Date.valueOf(month);
                LocalDate nextMonth = month.plusMonths(1);
                Date end = Date.valueOf(nextMonth);

                StringBuilder revWhere = new StringBuilder(" WHERE 1=1");
                List<Object> revParams = new ArrayList<>();
                if (sid != null) { revWhere.append(" AND store_id = ?"); revParams.add(sid); }
                revWhere.append(" AND payment_date >= ? AND payment_date < ?");
                revParams.add(start); revParams.add(end);
                BigDecimal revenue = sumOrZero(
                        "SELECT COALESCE(SUM(amount),0) FROM finance_payment_record" + revWhere,
                        revParams.toArray());

                StringBuilder costWhere = new StringBuilder(" WHERE 1=1");
                List<Object> costParams = new ArrayList<>();
                if (sid != null) { costWhere.append(" AND store_id = ?"); costParams.add(sid); }
                costWhere.append(" AND cost_date >= ? AND cost_date < ?");
                costParams.add(start); costParams.add(end);
                BigDecimal cost = sumOrZero(
                        "SELECT COALESCE(SUM(amount),0) FROM finance_cost_record" + costWhere,
                        costParams.toArray());

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("month", monthKey);
                m.put("revenue", revenue);
                m.put("cost", cost);
                result.add(m);
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "查询月度趋势失败: " + e.getMessage());
        }
    }

    @GetMapping("/pending-docs")
    public Result<List<Map<String, Object>>> getPendingDocs(
            @RequestParam(required = false) String storeId,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            Long sid = resolveQueryStoreId(storeId);
            if (limit < 1 || limit > 50) limit = 10;

            StringBuilder where = new StringBuilder(" WHERE status IN ('unpaid','partial')");
            List<Object> params = new ArrayList<>();
            if (sid != null) { where.append(" AND store_id = ?"); params.add(sid); }
            List<Object> limitParams = new ArrayList<>(params);
            limitParams.add(limit);

            List<Map<String, Object>> docs = jdbc.queryForList(
                    "SELECT receivable_id AS id, 'cashier' AS type, CONCAT('收银对账 - ', receivable_no) AS title,"
                            + " receivable_date AS doc_date, pending_amount AS amount, '待审核' AS status"
                            + " FROM finance_receivable" + where
                            + " ORDER BY receivable_date DESC LIMIT ?",
                    limitParams.toArray());
            List<Map<String, Object>> payable = jdbc.queryForList(
                    "SELECT payable_id AS id, 'supplier' AS type, CONCAT('供应商结款 - ', supplier_name) AS title,"
                            + " payable_date AS doc_date, pending_amount AS amount, '待支付' AS status"
                            + " FROM finance_payable" + where
                            + " ORDER BY payable_date DESC LIMIT ?",
                    limitParams.toArray());
            List<Map<String, Object>> recon = jdbc.queryForList(
                    "SELECT recon_id AS id, 'banquet' AS type, CONCAT('宴会定金 - ', recon_no) AS title,"
                            + " recon_date AS doc_date, diff_amount AS amount, '待确认' AS status"
                            + " FROM finance_reconciliation" + where
                            + " ORDER BY recon_date DESC LIMIT ?",
                    limitParams.toArray());

            docs.addAll(payable);
            docs.addAll(recon);
            docs.sort((a, b) -> {
                Object da = a.get("doc_date");
                Object db = b.get("doc_date");
                if (da == null && db == null) return 0;
                if (da == null) return 1;
                if (db == null) return -1;
                return db.toString().compareTo(da.toString());
            });
            if (docs.size() > limit) docs = docs.subList(0, limit);
            return Result.success(docs);
        } catch (Exception e) {
            return Result.error(500, "查询待对账单据失败: " + e.getMessage());
        }
    }

    private BigDecimal sumOrZero(String sql, Object... args) {
        try {
            BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, args);
            return v == null ? BigDecimal.ZERO : v;
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return BigDecimal.ZERO;
        }
    }

    // ============ 1. finance_account 账户 ============
    /**
     * 当前操作用户的 storeId（GM 可跨门店，非 GM 强制使用 JWT 中的 storeId）。
     * <p>
     * GM（isDataScopeAll=true）：使用请求参数 sid，sid 为 null/0 时返回 null（表示不限门店）。
     * 非 GM：强制返回 UserContext.currentStoreId()，忽略请求参数，防止跨门店访问。
     */
    private Long storeId(Long sid) {
        if (UserContext.isDataScopeAll()) {
            // GM可以查看所有门店，使用请求参数
            return (sid == null || sid == 0L) ? null : sid;
        }
        // 非GM强制使用当前门店
        return UserContext.currentStoreId();
    }

    /** 从请求体提取 storeId（GM 创建数据时指定门店） */
    private Long bodyStoreId(Map<String, Object> body) {
        Object v = body.get("storeId");
        if (v == null) return null;
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @GetMapping("/account")
    public Result<List<Map<String, Object>>> listAccount(@RequestParam(defaultValue = "1") Long storeId) {
        Long sid = storeId(storeId);
        if (sid != null) {
            return Result.success(jdbc.queryForList(
                "SELECT * FROM finance_account WHERE store_id=? AND is_active=1 ORDER BY sort_order, account_id",
                sid));
        }
        return Result.success(jdbc.queryForList(
            "SELECT * FROM finance_account WHERE is_active=1 ORDER BY sort_order, account_id"));
    }

    @PostMapping("/account")
    public Result<Map<String, Object>> createAccount(@RequestBody Map<String, Object> body) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(bodyStoreId(body));
        if (sid == null) {
            return Result.error(400, "缺少storeId参数：请指定创建数据的门店");
        }
        long id = System.currentTimeMillis();
        String code = (String) body.getOrDefault("accountCode", "ACC" + id);
        String name = (String) body.getOrDefault("accountName", "默认账户");
        String type = (String) body.getOrDefault("accountType", "cash");
        double init = body.get("initialBalance") != null ? Double.parseDouble(body.get("initialBalance").toString()) : 0.0;
        jdbc.update("INSERT INTO finance_account (account_id, store_id, account_code, account_name, account_type, initial_balance, current_balance, is_active, sort_order, created_at) VALUES (?,?,?,?,?,?,?,1,?,NOW())",
            id, sid, code, name, type, init, init, 0);
        return Result.success(Map.of("accountId", id));
    }

    @DeleteMapping("/account/{id}")
    public Result<Void> deleteAccount(@PathVariable Long id) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(null);
        if (sid != null) {
            jdbc.update("DELETE FROM finance_account WHERE account_id=? AND store_id=?", id, sid);
        } else {
            jdbc.update("DELETE FROM finance_account WHERE account_id=?", id);
        }
        return Result.success();
    }

    // ============ 2. finance_voucher 凭证 ============
    @GetMapping("/voucher")
    public Result<List<Map<String, Object>>> listVoucher(@RequestParam(defaultValue = "1") Long storeId) {
        Long sid = storeId(storeId);
        if (sid != null) {
            return Result.success(jdbc.queryForList(
                "SELECT * FROM finance_voucher WHERE store_id=? ORDER BY voucher_date DESC, voucher_id DESC",
                sid));
        }
        return Result.success(jdbc.queryForList(
            "SELECT * FROM finance_voucher ORDER BY voucher_date DESC, voucher_id DESC"));
    }

    @PostMapping("/voucher")
    public Result<Map<String, Object>> createVoucher(@RequestBody Map<String, Object> body) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(bodyStoreId(body));
        if (sid == null) {
            return Result.error(400, "缺少storeId参数：请指定创建数据的门店");
        }
        long id = System.currentTimeMillis();
        String no = (String) body.getOrDefault("voucherNo", "VCH" + id);
        String date = (String) body.getOrDefault("voucherDate", LocalDate.now().toString());
        String type = (String) body.getOrDefault("voucherType", "general");
        String summary = (String) body.getOrDefault("summary", "");
        double debit = body.get("totalDebit") != null ? Double.parseDouble(body.get("totalDebit").toString()) : 0.0;
        double credit = body.get("totalCredit") != null ? Double.parseDouble(body.get("totalCredit").toString()) : 0.0;
        boolean balanced = Math.abs(debit - credit) < 0.01;
        String status = (String) body.getOrDefault("status", "draft");
        jdbc.update("INSERT INTO finance_voucher (voucher_id, store_id, voucher_no, voucher_date, voucher_type, summary, total_debit, total_credit, is_balanced, status, prepared_by, prepared_name, created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
            id, sid, no, date, type, summary, debit, credit, balanced ? 1 : 0, status, 1, "rino");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> details = (List<Map<String, Object>>) body.get("details");
        if (details != null) {
            int line = 1;
            for (Map<String, Object> d : details) {
                double da = d.get("debitAmount") != null ? Double.parseDouble(d.get("debitAmount").toString()) : 0.0;
                double ca = d.get("creditAmount") != null ? Double.parseDouble(d.get("creditAmount").toString()) : 0.0;
                Object ln = d.get("lineNo");
                int lineNo = (ln == null) ? line : Integer.parseInt(ln.toString());
                jdbc.update("INSERT INTO finance_voucher_detail (voucher_id, store_id, line_no, subject_code, subject_name, summary, debit_amount, credit_amount, created_at) VALUES (?,?,?,?,?,?,?,?,NOW())",
                    id, sid, lineNo, d.get("subjectCode"), d.get("subjectName"), d.get("summary"), da, ca);
                line++;
            }
        }
        return Result.success(Map.of("voucherId", id));
    }

    @DeleteMapping("/voucher/{id}")
    public Result<Void> deleteVoucher(@PathVariable Long id) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(null);
        if (sid != null) {
            jdbc.update("DELETE FROM finance_voucher_detail WHERE voucher_id=? AND store_id=?", id, sid);
            jdbc.update("DELETE FROM finance_voucher WHERE voucher_id=? AND store_id=?", id, sid);
        } else {
            jdbc.update("DELETE FROM finance_voucher_detail WHERE voucher_id=?", id);
            jdbc.update("DELETE FROM finance_voucher WHERE voucher_id=?", id);
        }
        return Result.success();
    }

    // ============ 3. finance_transaction 流水 ============
    @GetMapping("/transaction")
    public Result<List<Map<String, Object>>> listTransaction(@RequestParam(defaultValue = "1") Long storeId) {
        Long sid = storeId(storeId);
        if (sid != null) {
            return Result.success(jdbc.queryForList(
                "SELECT * FROM finance_transaction WHERE store_id=? ORDER BY trans_date DESC, trans_id DESC LIMIT 200",
                sid));
        }
        return Result.success(jdbc.queryForList(
            "SELECT * FROM finance_transaction ORDER BY trans_date DESC, trans_id DESC LIMIT 200"));
    }

    @PostMapping("/transaction")
    public Result<Map<String, Object>> createTransaction(@RequestBody Map<String, Object> body) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(bodyStoreId(body));
        if (sid == null) {
            return Result.error(400, "缺少storeId参数：请指定创建数据的门店");
        }
        long id = System.currentTimeMillis();
        String no = (String) body.getOrDefault("transNo", "TX" + id);
        String date = (String) body.getOrDefault("transDate", LocalDate.now().toString());
        String type = (String) body.getOrDefault("transType", "income");
        String category = (String) body.getOrDefault("transCategory", "");
        Long accountId = body.get("accountId") != null ? Long.parseLong(body.get("accountId").toString()) : null;
        double amount = body.get("amount") != null ? Double.parseDouble(body.get("amount").toString()) : 0.0;
        String payer = (String) body.getOrDefault("payerPayee", "");
        String method = (String) body.getOrDefault("paymentMethod", "cash");
        jdbc.update("INSERT INTO finance_transaction (trans_id, store_id, trans_no, trans_date, trans_time, trans_type, trans_category, account_id, amount, payer_payee, payment_method, operator_name, created_at) VALUES (?,?,?,?,NOW(),?,?,?,?,?,?,?,NOW())",
            id, sid, no, date, type, category, accountId, amount, payer, method, UserContext.getUsername() != null ? UserContext.getUsername() : "rino");
        return Result.success(Map.of("transId", id));
    }

    @DeleteMapping("/transaction/{id}")
    public Result<Void> deleteTransaction(@PathVariable Long id) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(null);
        if (sid != null) {
            jdbc.update("DELETE FROM finance_transaction WHERE trans_id=? AND store_id=?", id, sid);
        } else {
            jdbc.update("DELETE FROM finance_transaction WHERE trans_id=?", id);
        }
        return Result.success();
    }

    // ============ 4. finance_payable 应付 ============
    @GetMapping("/payable")
    public Result<List<Map<String, Object>>> listPayable(@RequestParam(defaultValue = "1") Long storeId) {
        Long sid = storeId(storeId);
        if (sid != null) {
            return Result.success(jdbc.queryForList(
                "SELECT * FROM finance_payable WHERE store_id=? ORDER BY due_date ASC, payable_id DESC",
                sid));
        }
        return Result.success(jdbc.queryForList(
            "SELECT * FROM finance_payable ORDER BY due_date ASC, payable_id DESC"));
    }

    @PostMapping("/payable")
    public Result<Map<String, Object>> createPayable(@RequestBody Map<String, Object> body) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(bodyStoreId(body));
        if (sid == null) {
            return Result.error(400, "缺少storeId参数：请指定创建数据的门店");
        }
        long id = System.currentTimeMillis();
        String no = (String) body.getOrDefault("payableNo", "PY" + id);
        String supplier = (String) body.getOrDefault("supplierName", "");
        double total = body.get("totalAmount") != null ? Double.parseDouble(body.get("totalAmount").toString()) : 0.0;
        String date = (String) body.getOrDefault("payableDate", LocalDate.now().toString());
        String due = (String) body.getOrDefault("dueDate", LocalDate.now().plusDays(30).toString());
        jdbc.update("INSERT INTO finance_payable (payable_id, store_id, payable_no, supplier_name, total_amount, paid_amount, pending_amount, payable_date, due_date, status, credit_days, operator_name, created_at) VALUES (?,?,?,?,?,0,?,?,?,'unpaid',30,?,NOW())",
            id, sid, no, supplier, total, total, date, due, UserContext.getUsername() != null ? UserContext.getUsername() : "rino");
        return Result.success(Map.of("payableId", id));
    }

    @DeleteMapping("/payable/{id}")
    public Result<Void> deletePayable(@PathVariable Long id) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(null);
        if (sid != null) {
            jdbc.update("DELETE FROM finance_payable WHERE payable_id=? AND store_id=?", id, sid);
        } else {
            jdbc.update("DELETE FROM finance_payable WHERE payable_id=?", id);
        }
        return Result.success();
    }

    // ============ 5. finance_receivable 应收 ============
    @GetMapping("/receivable")
    public Result<List<Map<String, Object>>> listReceivable(@RequestParam(defaultValue = "1") Long storeId) {
        Long sid = storeId(storeId);
        if (sid != null) {
            return Result.success(jdbc.queryForList(
                "SELECT * FROM finance_receivable WHERE store_id=? ORDER BY due_date ASC, receivable_id DESC",
                sid));
        }
        return Result.success(jdbc.queryForList(
            "SELECT * FROM finance_receivable ORDER BY due_date ASC, receivable_id DESC"));
    }

    @PostMapping("/receivable")
    public Result<Map<String, Object>> createReceivable(@RequestBody Map<String, Object> body) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(bodyStoreId(body));
        if (sid == null) {
            return Result.error(400, "缺少storeId参数：请指定创建数据的门店");
        }
        long id = System.currentTimeMillis();
        String no = (String) body.getOrDefault("receivableNo", "RV" + id);
        String customer = (String) body.getOrDefault("customerName", "");
        double total = body.get("totalAmount") != null ? Double.parseDouble(body.get("totalAmount").toString()) : 0.0;
        String date = (String) body.getOrDefault("receivableDate", LocalDate.now().toString());
        String due = (String) body.getOrDefault("dueDate", LocalDate.now().plusDays(30).toString());
        jdbc.update("INSERT INTO finance_receivable (receivable_id, store_id, receivable_no, customer_name, total_amount, received_amount, pending_amount, receivable_date, due_date, status, credit_days, operator_name, created_at) VALUES (?,?,?,?,?,0,?,?,?,'unpaid',30,?,NOW())",
            id, sid, no, customer, total, total, date, due, UserContext.getUsername() != null ? UserContext.getUsername() : "rino");
        return Result.success(Map.of("receivableId", id));
    }

    @DeleteMapping("/receivable/{id}")
    public Result<Void> deleteReceivable(@PathVariable Long id) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(null);
        if (sid != null) {
            jdbc.update("DELETE FROM finance_receivable WHERE receivable_id=? AND store_id=?", id, sid);
        } else {
            jdbc.update("DELETE FROM finance_receivable WHERE receivable_id=?", id);
        }
        return Result.success();
    }

    // ============ 6. finance_payment_record 收款 ============
    @GetMapping("/payment")
    public Result<List<Map<String, Object>>> listPaymentRecord(@RequestParam(defaultValue = "1") Long storeId) {
        Long sid = storeId(storeId);
        if (sid != null) {
            return Result.success(jdbc.queryForList(
                "SELECT * FROM finance_payment_record WHERE store_id=? ORDER BY payment_date DESC, payment_id DESC LIMIT 200",
                sid));
        }
        return Result.success(jdbc.queryForList(
            "SELECT * FROM finance_payment_record ORDER BY payment_date DESC, payment_id DESC LIMIT 200"));
    }

    @PostMapping("/payment")
    public Result<Map<String, Object>> createPaymentRecord(@RequestBody Map<String, Object> body) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(bodyStoreId(body));
        if (sid == null) {
            return Result.error(400, "缺少storeId参数：请指定创建数据的门店");
        }
        long id = System.currentTimeMillis();
        String no = (String) body.getOrDefault("paymentNo", "PAY" + id);
        String date = (String) body.getOrDefault("paymentDate", LocalDate.now().toString());
        String customer = (String) body.getOrDefault("customerName", "");
        double amount = body.get("amount") != null ? Double.parseDouble(body.get("amount").toString()) : 0.0;
        String method = (String) body.getOrDefault("paymentMethod", "cash");
        Long accountId = body.get("accountId") != null ? Long.parseLong(body.get("accountId").toString()) : null;
        jdbc.update("INSERT INTO finance_payment_record (payment_id, store_id, payment_no, payment_date, customer_name, amount, payment_method, account_id, operator_name, created_at) VALUES (?,?,?,?,?,?,?,?,?,NOW())",
            id, sid, no, date, customer, amount, method, accountId, UserContext.getUsername() != null ? UserContext.getUsername() : "rino");
        return Result.success(Map.of("paymentId", id));
    }

    @DeleteMapping("/payment/{id}")
    public Result<Void> deletePaymentRecord(@PathVariable Long id) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(null);
        if (sid != null) {
            jdbc.update("DELETE FROM finance_payment_record WHERE payment_id=? AND store_id=?", id, sid);
        } else {
            jdbc.update("DELETE FROM finance_payment_record WHERE payment_id=?", id);
        }
        return Result.success();
    }

    // ============ 7. finance_expense 报销 ============
    @GetMapping("/expense")
    public Result<List<Map<String, Object>>> listExpense(@RequestParam(defaultValue = "1") Long storeId) {
        Long sid = storeId(storeId);
        if (sid != null) {
            return Result.success(jdbc.queryForList(
                "SELECT * FROM finance_expense WHERE store_id=? ORDER BY expense_date DESC, expense_id DESC",
                sid));
        }
        return Result.success(jdbc.queryForList(
            "SELECT * FROM finance_expense ORDER BY expense_date DESC, expense_id DESC"));
    }

    @PostMapping("/expense")
    public Result<Map<String, Object>> createExpense(@RequestBody Map<String, Object> body) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(bodyStoreId(body));
        if (sid == null) {
            return Result.error(400, "缺少storeId参数：请指定创建数据的门店");
        }
        long id = System.currentTimeMillis();
        String no = (String) body.getOrDefault("expenseNo", "EX" + id);
        String type = (String) body.getOrDefault("expenseType", "general");
        String date = (String) body.getOrDefault("expenseDate", LocalDate.now().toString());
        String applicant = (String) body.getOrDefault("applicantName", "");
        double amount = body.get("amount") != null ? Double.parseDouble(body.get("amount").toString()) : 0.0;
        jdbc.update("INSERT INTO finance_expense (expense_id, store_id, expense_no, expense_type, expense_date, applicant_name, department, amount, approval_status, payment_status, created_at) VALUES (?,?,?,?,?,?,?,?,'pending','unpaid',NOW())",
            id, sid, no, type, date, applicant, "总经办", amount);
        return Result.success(Map.of("expenseId", id));
    }

    @DeleteMapping("/expense/{id}")
    public Result<Void> deleteExpense(@PathVariable Long id) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(null);
        if (sid != null) {
            jdbc.update("DELETE FROM finance_expense WHERE expense_id=? AND store_id=?", id, sid);
        } else {
            jdbc.update("DELETE FROM finance_expense WHERE expense_id=?", id);
        }
        return Result.success();
    }

    // ============ 8. finance_cost_record 成本 ============
    @GetMapping("/cost")
    public Result<List<Map<String, Object>>> listCostRecord(@RequestParam(defaultValue = "1") Long storeId) {
        Long sid = storeId(storeId);
        if (sid != null) {
            return Result.success(jdbc.queryForList(
                "SELECT * FROM finance_cost_record WHERE store_id=? ORDER BY cost_date DESC, cost_id DESC LIMIT 200",
                sid));
        }
        return Result.success(jdbc.queryForList(
            "SELECT * FROM finance_cost_record ORDER BY cost_date DESC, cost_id DESC LIMIT 200"));
    }

    @PostMapping("/cost")
    public Result<Map<String, Object>> createCostRecord(@RequestBody Map<String, Object> body) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(bodyStoreId(body));
        if (sid == null) {
            return Result.error(400, "缺少storeId参数：请指定创建数据的门店");
        }
        long id = System.currentTimeMillis();
        String date = (String) body.getOrDefault("costDate", LocalDate.now().toString());
        String type = (String) body.getOrDefault("costType", "food");
        String category = (String) body.getOrDefault("costCategory", "");
        double amount = body.get("amount") != null ? Double.parseDouble(body.get("amount").toString()) : 0.0;
        jdbc.update("INSERT INTO finance_cost_record (cost_id, store_id, cost_date, cost_type, cost_category, amount, operator_name, created_at) VALUES (?,?,?,?,?,?,?,NOW())",
            id, sid, date, type, category, amount, UserContext.getUsername() != null ? UserContext.getUsername() : "rino");
        return Result.success(Map.of("costId", id));
    }

    @DeleteMapping("/cost/{id}")
    public Result<Void> deleteCostRecord(@PathVariable Long id) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(null);
        if (sid != null) {
            jdbc.update("DELETE FROM finance_cost_record WHERE cost_id=? AND store_id=?", id, sid);
        } else {
            jdbc.update("DELETE FROM finance_cost_record WHERE cost_id=?", id);
        }
        return Result.success();
    }

    // ============ 9. finance_reconciliation 对账 ============
    @GetMapping("/reconciliation")
    public Result<List<Map<String, Object>>> listReconciliation(@RequestParam(defaultValue = "1") Long storeId) {
        Long sid = storeId(storeId);
        if (sid != null) {
            return Result.success(jdbc.queryForList(
                "SELECT * FROM finance_reconciliation WHERE store_id=? ORDER BY recon_date DESC, recon_id DESC",
                sid));
        }
        return Result.success(jdbc.queryForList(
            "SELECT * FROM finance_reconciliation ORDER BY recon_date DESC, recon_id DESC"));
    }

    @PostMapping("/reconciliation")
    public Result<Map<String, Object>> createReconciliation(@RequestBody Map<String, Object> body) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(bodyStoreId(body));
        if (sid == null) {
            return Result.error(400, "缺少storeId参数：请指定创建数据的门店");
        }
        long id = System.currentTimeMillis();
        String no = (String) body.getOrDefault("reconNo", "RC" + id);
        String date = (String) body.getOrDefault("reconDate", LocalDate.now().toString());
        String account = (String) body.getOrDefault("accountName", "");
        double book = body.get("bookBalance") != null ? Double.parseDouble(body.get("bookBalance").toString()) : 0.0;
        double bank = body.get("bankBalance") != null ? Double.parseDouble(body.get("bankBalance").toString()) : 0.0;
        jdbc.update("INSERT INTO finance_reconciliation (recon_id, store_id, recon_no, recon_date, account_name, book_balance, bank_balance, diff_amount, status, operator_name, created_at) VALUES (?,?,?,?,?,?,?,?,?,'pending',?,NOW())",
            id, sid, no, date, account, book, bank, book - bank, "pending", UserContext.getUsername() != null ? UserContext.getUsername() : "rino");
        return Result.success(Map.of("reconId", id));
    }

    @DeleteMapping("/reconciliation/{id}")
    public Result<Void> deleteReconciliation(@PathVariable Long id) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(null);
        if (sid != null) {
            jdbc.update("DELETE FROM finance_reconciliation WHERE recon_id=? AND store_id=?", id, sid);
        } else {
            jdbc.update("DELETE FROM finance_reconciliation WHERE recon_id=?", id);
        }
        return Result.success();
    }

    // ============ 10. finance_settlement 结算 ============
    @GetMapping("/settlement")
    public Result<List<Map<String, Object>>> listSettlement(@RequestParam(defaultValue = "1") Long storeId) {
        Long sid = storeId(storeId);
        if (sid != null) {
            return Result.success(jdbc.queryForList(
                "SELECT * FROM finance_settlement WHERE store_id=? ORDER BY settlement_date DESC, settlement_id DESC",
                sid));
        }
        return Result.success(jdbc.queryForList(
            "SELECT * FROM finance_settlement ORDER BY settlement_date DESC, settlement_id DESC"));
    }

    @PostMapping("/settlement")
    public Result<Map<String, Object>> createSettlement(@RequestBody Map<String, Object> body) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(bodyStoreId(body));
        if (sid == null) {
            return Result.error(400, "缺少storeId参数：请指定创建数据的门店");
        }
        long id = System.currentTimeMillis();
        String no = (String) body.getOrDefault("settlementNo", "ST" + id);
        String date = (String) body.getOrDefault("settlementDate", LocalDate.now().toString());
        String start = (String) body.getOrDefault("startDate", LocalDate.now().withDayOfMonth(1).toString());
        String end = (String) body.getOrDefault("endDate", LocalDate.now().toString());
        double income = body.get("totalIncome") != null ? Double.parseDouble(body.get("totalIncome").toString()) : 0.0;
        double expense = body.get("totalExpense") != null ? Double.parseDouble(body.get("totalExpense").toString()) : 0.0;
        jdbc.update("INSERT INTO finance_settlement (settlement_id, store_id, settlement_no, settlement_date, settlement_type, start_date, end_date, total_income, total_expense, total_profit, cost_rate, status, operator_name, created_at) VALUES (?,?,?,?,'monthly',?,?,?,?,?,?,'draft',?,NOW())",
            id, sid, no, date, start, end, income, expense, income - expense, expense / Math.max(income, 1) * 100, UserContext.getUsername() != null ? UserContext.getUsername() : "rino");
        return Result.success(Map.of("settlementId", id));
    }

    @DeleteMapping("/settlement/{id}")
    public Result<Void> deleteSettlement(@PathVariable Long id) {
        UserContext.ensureDataScopeFromStoreId();
        Long sid = storeId(null);
        if (sid != null) {
            jdbc.update("DELETE FROM finance_settlement WHERE settlement_id=? AND store_id=?", id, sid);
        } else {
            jdbc.update("DELETE FROM finance_settlement WHERE settlement_id=?", id);
        }
        return Result.success();
    }
}
