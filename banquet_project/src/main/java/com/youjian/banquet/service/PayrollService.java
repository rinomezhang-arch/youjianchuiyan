package com.youjian.banquet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.YearMonth;
import java.util.*;

/**
 * 工资保存 → 审批 → 发放记账的唯一入口。
 * <p>
 * 基线 {@code PayrollController} 有四个必须修掉的问题，每一个都能造成真实的钱算错或查不清：
 * <ol>
 *   <li><b>已发放被静默退回</b>：/save 的 UPDATE 硬写 {@code status=1} 且不带任何状态条件，
 *       已经发放(status=3)的月份被再保存一次就退回"已保存"，发放这件事在库里消失；</li>
 *   <li><b>合计信任客户端</b>：{@code gross_pay}/{@code net_pay} 直接取请求体存库，
 *       客户端传多少就是多少；</li>
 *   <li><b>坏数据静默跳过</b>：员工不存在、跨店、缺 emp_id 一律 {@code continue}，
 *       最后仍返回 {@code saved: N} 报成功——调用方以为全存了，实际漏了一片；</li>
 *   <li><b>没有审批环节</b>：状态直接从 1 跳到 3，谁批准的无从查起。</li>
 * </ol>
 * 这里的规则：<b>坏数据整批拒绝并回滚</b>，<b>合计一律服务端重算</b>，
 * <b>已审批/已发放的月份不允许被保存悄悄改回去</b>，<b>审批只认真人</b>。
 * <p>
 * 状态口径：1=已保存　2=已审批　3=已发放记账。
 * <b>status=3 只表示账上记了这一笔，不代表钱已经到员工银行账户</b>；
 * 本服务不执行、也没有能力执行真实工资支付。
 */
@Service
public class PayrollService {

    private static final Logger log = LoggerFactory.getLogger(PayrollService.class);

    private static final BigDecimal STD_DAYS = new BigDecimal("22");
    private static final BigDecimal HOURLY_DIV = new BigDecimal("21.75").multiply(new BigDecimal("8"));
    private static final BigDecimal TAX_THRESHOLD = new BigDecimal("5000");

    public static final int STATUS_SAVED = 1;
    public static final int STATUS_APPROVED = 2;
    public static final int STATUS_PAID = 3;

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 有权批准工资的真人白名单，与业务审批白名单共用同一份配置，避免两处口径漂移。
     * 现阶段公司只授权张婧、张晓秋批复。
     */
    @Value("${approval.approvers:张婧,zhangjing,张晓秋,zhangxiaoqiu,rino}")
    private String approversRaw;

    /** 请求被拒：调用方该改输入或先走完前置流程，不是服务端故障。 */
    public static class PayrollRejectedException extends RuntimeException {
        public PayrollRejectedException(String message) { super(message); }
    }

    /** 当前操作人身份与门店范围，由 Controller 鉴权后传入。 */
    public static class PayrollContext {
        public final boolean allStores;
        public final Long userStoreId;
        public final Long staffId;
        public final String operator;

        public PayrollContext(boolean allStores, Long userStoreId, Long staffId, String operator) {
            this.allStores = allStores;
            this.userStoreId = userStoreId;
            this.staffId = staffId;
            this.operator = operator;
        }
    }

    // ==================== 保存 ====================

    /**
     * 保存本月工资。
     * <p>
     * 校验全部前置：任何一行不合格，整批拒绝，事务回滚，<b>一行都不写</b>。
     * 合计（应发、个税、实发）一律服务端按组成项重算，客户端传来的 gross_pay/net_pay 只用于比对，
     * 不作为落库依据。
     */
    @Transactional
    public Map<String, Object> save(String month, List<Map<String, Object>> rows, PayrollContext ctx) {
        YearMonth ym = parseMonth(month);
        assertScopeComplete(ctx);
        if (rows == null || rows.isEmpty()) {
            throw new PayrollRejectedException("没有可保存的工资数据");
        }

        // 员工档案一次读齐，避免逐行查库；同时拿到门店归属用于跨店判断。
        Map<Integer, Long> staffStore = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("SELECT staff_id, store_id FROM staff_master")) {
            Object sid = r.get("staff_id");
            Object stid = r.get("store_id");
            if (sid != null && stid != null) {
                staffStore.put(((Number) sid).intValue(), ((Number) stid).longValue());
            }
        }

        // ---- 第一遍：只校验，不写 ----
        List<Integer> staffIds = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        int line = 0;
        for (Map<String, Object> row : rows) {
            line++;
            Object rawId = row.get("emp_id");
            if (rawId == null) {
                throw new PayrollRejectedException("第 " + line + " 行缺少员工编号，整批未保存");
            }
            int empId;
            try {
                empId = rawId instanceof Number ? ((Number) rawId).intValue()
                        : Integer.parseInt(rawId.toString().trim());
            } catch (NumberFormatException e) {
                throw new PayrollRejectedException("第 " + line + " 行员工编号非法：" + rawId + "，整批未保存");
            }
            if (!seen.add(empId)) {
                // 重复员工会让后一条覆盖前一条，且合计对不上，必须拒绝而不是挑一条存。
                throw new PayrollRejectedException("员工 " + empId + " 在本次提交中重复出现，整批未保存");
            }
            Long store = staffStore.get(empId);
            if (store == null) {
                throw new PayrollRejectedException("员工 " + empId + " 不存在于花名册，整批未保存");
            }
            if (!ctx.allStores && ctx.userStoreId != null && !ctx.userStoreId.equals(store)) {
                throw new PayrollRejectedException("员工 " + empId + " 不属于当前门店，无权保存其工资，整批未保存");
            }
            validateAmounts(line, row);
            staffIds.add(empId);
        }

        // ---- 已审批/已发放的月份不允许被保存悄悄改回去 ----
        List<Map<String, Object>> locked = jdbc.queryForList(
                "SELECT staff_id, status FROM month_salary WHERE salary_month = ? AND status >= ? "
                        + "AND staff_id IN (" + placeholders(staffIds.size()) + ") FOR UPDATE",
                mergeArgs(new Object[]{month, STATUS_APPROVED}, staffIds));
        if (!locked.isEmpty()) {
            List<String> who = new ArrayList<>();
            for (Map<String, Object> r : locked) {
                int st = ((Number) r.get("status")).intValue();
                who.add(r.get("staff_id") + "(" + (st == STATUS_PAID ? "已发放记账" : "已审批") + ")");
            }
            throw new PayrollRejectedException(
                    "以下员工本月工资已进入审批或发放阶段，不能直接保存覆盖：" + String.join("、", who)
                            + "。当前没有撤回审批的接口，需要改动请联系张婧或张晓秋处理，整批未保存");
        }

        // ---- 第二遍：重算并落库 ----
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        List<Map<String, Object>> ignoredClientTotals = new ArrayList<>();
        int saved = 0;
        line = 0;
        for (Map<String, Object> row : rows) {
            line++;
            int empId = ((Number) toNumber(row.get("emp_id"))).intValue();
            Long storeId = staffStore.get(empId);

            BigDecimal base = amount(row, "base_salary");
            BigDecimal post = amount(row, "post_salary");
            BigDecimal attendancePay = amount(row, "attendance_pay");
            BigDecimal overtimePay = amount(row, "overtime_pay");
            BigDecimal bonus = amount(row, "bonus");
            BigDecimal allowance = amount(row, "allowance");
            BigDecimal dedSocial = amount(row, "deduction_social");
            BigDecimal dedOther = amount(row, "deduction_other");

            // 合计一律重算。客户端传来的 gross_pay/net_pay/deduction_tax 不参与落库，
            // 只在对不上时记下来一并返回，方便查是谁在改数。
            BigDecimal gross = base.add(post).add(attendancePay).add(overtimePay).add(bonus).add(allowance);
            BigDecimal tax = calcTax(gross.subtract(dedSocial).subtract(dedOther).subtract(TAX_THRESHOLD));
            BigDecimal net = gross.subtract(dedSocial).subtract(tax).subtract(dedOther);
            gross = round2(gross);
            tax = round2(tax);
            net = round2(net);

            recordIfDifferent(ignoredClientTotals, empId, "gross_pay", row.get("gross_pay"), gross);
            recordIfDifferent(ignoredClientTotals, empId, "net_pay", row.get("net_pay"), net);
            recordIfDifferent(ignoredClientTotals, empId, "deduction_tax", row.get("deduction_tax"), tax);

            List<Map<String, Object>> existing = jdbc.queryForList(
                    "SELECT salary_id FROM month_salary WHERE staff_id = ? AND salary_month = ?", empId, month);
            if (!existing.isEmpty()) {
                long salaryId = ((Number) existing.get(0).get("salary_id")).longValue();
                // status 条件不能省：上面虽已拦下 >=2 的记录，这里再挡一次，
                // 避免并发下别的事务刚把它推进到已审批。
                int rows2 = jdbc.update(
                        "UPDATE month_salary SET base_salary=?, overtime_pay=?, performance_salary=?, "
                                + "reward_amount=?, other_allowance=?, social_security_deduction=?, "
                                + "housing_fund_deduction=?, tax_amount=?, gross_salary=?, net_salary=?, "
                                + "status=?, post_salary_snapshot=?, attendance_pay_snapshot=?, updated_at=NOW() WHERE salary_id=? AND status < ?",
                        base, overtimePay, post.add(attendancePay), bonus, allowance,
                        dedSocial, dedOther, tax, gross, net, STATUS_SAVED, post, attendancePay, salaryId, STATUS_APPROVED);
                if (rows2 == 0) {
                    throw new PayrollRejectedException(
                            "员工 " + empId + " 的本月工资状态在保存过程中被改动，整批未保存，请刷新后重试");
                }
            } else {
                jdbc.update(
                        "INSERT INTO month_salary (store_id, staff_id, salary_month, base_salary, "
                                + "overtime_pay, performance_salary, reward_amount, other_allowance, "
                                + "social_security_deduction, housing_fund_deduction, tax_amount, "
                                + "gross_salary, net_salary, status, post_salary_snapshot, attendance_pay_snapshot, created_at, updated_at) "
                                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                        storeId, empId, month, base, overtimePay, post.add(attendancePay), bonus, allowance,
                        dedSocial, dedOther, tax, gross, net, STATUS_SAVED, post, attendancePay);
            }
            totalGross = totalGross.add(gross);
            totalNet = totalNet.add(net);
            saved++;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("month", ym.toString());
        data.put("saved", saved);
        data.put("totalGross", round2(totalGross));
        data.put("totalNet", round2(totalNet));
        data.put("ignoredClientTotals", ignoredClientTotals);
        if (!ignoredClientTotals.isEmpty()) {
            log.warn("【工资保存】{} 提交的 {} 个合计与服务端重算不一致，已按服务端为准",
                    ctx.operator, ignoredClientTotals.size());
        }
        log.info("【工资保存】{} 保存 {} 月工资 {} 人，应发合计 {}，实发合计 {}",
                ctx.operator, month, saved, data.get("totalGross"), data.get("totalNet"));
        return data;
    }

    // ==================== 审批 ====================

    /**
     * 审批本月工资：1 → 2。
     * <p>
     * <b>只认真人</b>：必须是登录员工，且在批复人白名单内。AI 不能冒充真人批准——
     * 服务只接受由 JWT 解析出的 staffId，任何 AI 令牌都到不了这里。
     */
    @Transactional
    public Map<String, Object> approve(String month, PayrollContext ctx) {
        parseMonth(month);
        assertScopeComplete(ctx);
        assertRealApprover(ctx);

        List<Object> args = new ArrayList<>(List.of(STATUS_APPROVED, ctx.operator, month, STATUS_SAVED));
        StringBuilder sql = new StringBuilder(
                "UPDATE month_salary SET status=?, approved_by=?, approved_at=NOW(), updated_at=NOW() "
                        + "WHERE salary_month=? AND status=?");
        if (!ctx.allStores && ctx.userStoreId != null) {
            sql.append(" AND store_id=?");
            args.add(ctx.userStoreId);
        }
        int approved = jdbc.update(sql.toString(), args.toArray());
        if (approved == 0) {
            throw new PayrollRejectedException("没有待审批的工资记录：请先保存本月工资，或本月已审批过");
        }
        log.info("【工资审批】{} 审批 {} 月工资 {} 人", ctx.operator, month, approved);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("month", month);
        data.put("approved", approved);
        data.put("approvedBy", ctx.operator);
        return data;
    }

    // ==================== 发放记账 ====================

    /**
     * 发放记账：2 → 3，并写一条台账。
     * <p>
     * <b>这一步只是记账，不是付款。</b>系统不对接银行，不执行任何实际资金支付；
     * status=3 的含义是"账上记了这一笔"，钱是否到账要以银行流水为准。
     * <p>
     * 必须先审批：还存在未审批(status=1)的记录时整批拒绝，不允许绕过审批直接记账。
     * 重复调用不会重复记账，会明确告知本月已记过。
     */
    @Transactional
    public Map<String, Object> payout(String month, PayrollContext ctx) {
        parseMonth(month);
        assertScopeComplete(ctx);

        String scope = ctx.allStores ? "" : " AND store_id=?";
        List<Object> scopeArgs = ctx.allStores ? List.of() : List.of(ctx.userStoreId);

        Integer pending = jdbc.queryForObject(
                "SELECT COUNT(*) FROM month_salary WHERE salary_month=? AND status=?" + scope,
                Integer.class, mergeArgs(new Object[]{month, STATUS_SAVED}, scopeArgs));
        if (pending != null && pending > 0) {
            throw new PayrollRejectedException(
                    "本月还有 " + pending + " 条工资未审批，不能发放记账。请先完成审批");
        }

        // 锁定并取出**本批**要记账的具体行。
        // 之前是"UPDATE 本批、SUM 全月"，分批记账时第二批的台账会把第一批的金额也加进去，
        // 记 1 个人却写出两个人的合计。台账金额只能来自本批锁定的这几行。
        List<Map<String, Object>> batch = jdbc.queryForList(
                "SELECT salary_id, net_salary FROM month_salary WHERE salary_month=? AND status=?" + scope
                        + " ORDER BY salary_id FOR UPDATE",
                mergeArgs(new Object[]{month, STATUS_APPROVED}, scopeArgs));

        if (batch.isEmpty()) {
            // 空批次有三种完全不同的原因，不能一律说成"此前已完成记账"——
            // 从没保存过工资的月份也回"已发放"，那是假成功。
            Integer total = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM month_salary WHERE salary_month=?" + scope,
                    Integer.class, mergeArgs(new Object[]{month}, scopeArgs));
            if (total == null || total == 0) {
                throw new PayrollRejectedException(ctx.allStores
                        ? "本月没有任何工资记录，无法发放记账。请先保存并审批本月工资"
                        : "本门店本月没有任何工资记录，无法发放记账。请先保存并审批本月工资");
            }
            Integer paidAlready = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM month_salary WHERE salary_month=? AND status=?" + scope,
                    Integer.class, mergeArgs(new Object[]{month, STATUS_PAID}, scopeArgs));
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("month", month);
            data.put("paid", 0);
            data.put("alreadyRecorded", true);
            data.put("previouslyRecorded", paidAlready);
            data.put("message", "本月这 " + paidAlready + " 条工资此前已完成发放记账，本次未产生新的记账");
            return data;
        }

        List<Long> ids = new ArrayList<>();
        BigDecimal totalNet = BigDecimal.ZERO;
        for (Map<String, Object> r : batch) {
            ids.add(((Number) r.get("salary_id")).longValue());
            Object net = r.get("net_salary");
            if (net != null) totalNet = totalNet.add(new BigDecimal(net.toString()));
        }
        BigDecimal batchTotal = round2(totalNet);

        // 先写台账拿到批次号，再把本批工资行指回这个批次，两边可互相反查。
        KeyHolder keys = new GeneratedKeyHolder();
        Long storeForRecord = ctx.allStores ? null : ctx.userStoreId;
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO payroll_payout_record (salary_month, store_id, headcount, total_net, recorded_by, note) "
                            + "VALUES (?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, month);
            if (storeForRecord == null) ps.setNull(2, java.sql.Types.BIGINT);
            else ps.setLong(2, storeForRecord);
            ps.setInt(3, ids.size());
            ps.setBigDecimal(4, batchTotal);
            ps.setString(5, ctx.operator);
            ps.setString(6, "仅为账务记录，不代表银行实际到账");
            return ps;
        }, keys);
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException("发放记账台账未返回批次号");
        long payoutId = key.longValue();

        // 只更新本批锁定的这些 id，不按时间或操作人模糊圈定。
        List<Object> updateArgs = new ArrayList<>(List.of(STATUS_PAID, ctx.operator, payoutId));
        updateArgs.addAll(ids);
        updateArgs.add(STATUS_APPROVED);
        int paid = jdbc.update(
                "UPDATE month_salary SET status=?, paid_by=?, paid_at=NOW(), payout_id=?, updated_at=NOW() "
                        + "WHERE salary_id IN (" + placeholders(ids.size()) + ") AND status=?",
                updateArgs.toArray());
        if (paid != ids.size()) {
            // 锁已经拿到，正常不该少更新；真出现就整批回滚，不留一本对不上的账。
            throw new IllegalStateException(
                    "发放记账行数与本批不符：预期 " + ids.size() + " 实际 " + paid + "，已回滚");
        }

        log.info("【工资发放记账】{} 记账 {} 月批次 {}，{} 人，本批实发合计 {}",
                ctx.operator, month, payoutId, paid, batchTotal);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("month", month);
        data.put("payoutId", payoutId);
        data.put("paid", paid);
        data.put("totalNet", batchTotal);
        data.put("recordedBy", ctx.operator);
        data.put("message", "已完成本批发放记账。这是账务记录，不代表银行已到账，请以银行流水为准");
        return data;
    }

    // ==================== 内部 ====================

    /**
     * 上下文必须完整才能动工资。
     * <p>
     * 原来各处写的是 {@code !allStores && userStoreId != null} 才加门店条件——
     * 一旦某个非全店用户的 storeId 解析不出来，门店条件被整条丢掉，
     * 店长的操作**悄悄退化成全店**。范围判定必须 fail-closed：宁可拒绝，不可放大。
     */
    private void assertScopeComplete(PayrollContext ctx) {
        if (!ctx.allStores && ctx.userStoreId == null) {
            throw new PayrollRejectedException(
                    "当前登录身份没有解析出门店，无法确定可操作范围，已拒绝。请重新登录或联系管理员核对门店归属");
        }
    }

    /** 审批人必须是登录真人且在白名单内。 */
    private void assertRealApprover(PayrollContext ctx) {
        if (ctx.staffId == null) {
            throw new PayrollRejectedException("未登录，无法审批工资");
        }
        Set<String> allowed = new LinkedHashSet<>();
        for (String s : approversRaw.split(",")) {
            String v = s.trim().toLowerCase(Locale.ROOT);
            if (!v.isEmpty()) allowed.add(v);
        }
        // 登录支持姓名/账号/手机号/英文名四种输入，subject 可能是其中任意一种，
        // 所以回查花名册把这条员工的几种写法都拿出来比对。
        Set<String> identities = new LinkedHashSet<>();
        if (ctx.operator != null) identities.add(ctx.operator.trim().toLowerCase(Locale.ROOT));
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT staff_name, staff_account, staff_en_name FROM staff_master WHERE staff_id=? LIMIT 1",
                ctx.staffId);
        for (Map<String, Object> r : rows) {
            for (Object v : r.values()) {
                if (v != null && !v.toString().isBlank()) {
                    identities.add(v.toString().trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        if (identities.stream().noneMatch(allowed::contains)) {
            log.warn("【工资审批】{}（staffId={}）不在批复白名单内，拒绝", ctx.operator, ctx.staffId);
            throw new PayrollRejectedException("当前仅张婧、张晓秋可以审批工资，请转交他们处理");
        }
    }

    private void validateAmounts(int line, Map<String, Object> row) {
        for (String field : List.of("base_salary", "post_salary", "attendance_pay", "overtime_pay",
                "bonus", "allowance", "deduction_social", "deduction_other")) {
            Object v = row.get(field);
            if (v == null) continue;
            BigDecimal bd;
            try {
                bd = new BigDecimal(v.toString().trim());
            } catch (NumberFormatException e) {
                throw new PayrollRejectedException("第 " + line + " 行 " + field + " 不是合法金额：" + v + "，整批未保存");
            }
            if (bd.signum() < 0) {
                throw new PayrollRejectedException("第 " + line + " 行 " + field + " 不能为负：" + bd + "，整批未保存");
            }
            if (bd.scale() > 2) {
                throw new PayrollRejectedException("第 " + line + " 行 " + field + " 最多两位小数：" + bd + "，整批未保存");
            }
        }
    }

    private void recordIfDifferent(List<Map<String, Object>> sink, int empId, String field,
                                   Object clientValue, BigDecimal serverValue) {
        if (clientValue == null) return;
        BigDecimal client;
        try {
            client = new BigDecimal(clientValue.toString().trim());
        } catch (NumberFormatException e) {
            client = null;
        }
        if (client == null || client.compareTo(serverValue) != 0) {
            Map<String, Object> diff = new LinkedHashMap<>();
            diff.put("empId", empId);
            diff.put("field", field);
            diff.put("client", clientValue);
            diff.put("server", serverValue);
            sink.add(diff);
        }
    }

    private static YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (RuntimeException e) {
            throw new PayrollRejectedException("月份格式非法，应为 YYYY-MM：" + month);
        }
    }

    private static String placeholders(int n) {
        return String.join(",", Collections.nCopies(Math.max(n, 1), "?"));
    }

    private static Object[] mergeArgs(Object[] head, List<?> tail) {
        List<Object> all = new ArrayList<>(Arrays.asList(head));
        all.addAll(tail);
        return all.toArray();
    }

    private static Object toNumber(Object o) {
        return o instanceof Number ? o : new BigDecimal(o.toString().trim());
    }

    private BigDecimal amount(Map<String, Object> row, String field) {
        Object v = row.get(field);
        if (v == null) return BigDecimal.ZERO;
        try {
            return new BigDecimal(v.toString().trim());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public static BigDecimal round2(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    /** 个税：与基线同一套速算扣除表，抽到服务里统一维护，避免两处各算一遍。 */
    public static BigDecimal calcTax(BigDecimal taxable) {
        if (taxable == null || taxable.signum() <= 0) return BigDecimal.ZERO;
        BigDecimal tax;
        if (taxable.compareTo(new BigDecimal("3000")) <= 0) {
            tax = taxable.multiply(new BigDecimal("0.03"));
        } else if (taxable.compareTo(new BigDecimal("12000")) <= 0) {
            tax = taxable.multiply(new BigDecimal("0.1")).subtract(new BigDecimal("210"));
        } else if (taxable.compareTo(new BigDecimal("25000")) <= 0) {
            tax = taxable.multiply(new BigDecimal("0.2")).subtract(new BigDecimal("1410"));
        } else if (taxable.compareTo(new BigDecimal("35000")) <= 0) {
            tax = taxable.multiply(new BigDecimal("0.25")).subtract(new BigDecimal("2660"));
        } else if (taxable.compareTo(new BigDecimal("55000")) <= 0) {
            tax = taxable.multiply(new BigDecimal("0.3")).subtract(new BigDecimal("4410"));
        } else if (taxable.compareTo(new BigDecimal("80000")) <= 0) {
            tax = taxable.multiply(new BigDecimal("0.35")).subtract(new BigDecimal("7160"));
        } else {
            tax = taxable.multiply(new BigDecimal("0.45")).subtract(new BigDecimal("15160"));
        }
        return tax.max(BigDecimal.ZERO);
    }
}
