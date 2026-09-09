/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.common.Result
 *  com.youjian.banquet.controller.PayrollController
 *  org.springframework.beans.factory.annotation.Autowired
 *  org.springframework.jdbc.core.JdbcTemplate
 *  org.springframework.web.bind.annotation.CrossOrigin
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.UserContext;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/hr/payroll"})
@CrossOrigin(origins={"*"})
public class PayrollController {
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private com.youjian.banquet.service.PayrollService payrollService;

    /** 工资表解锁码，必须由环境变量注入；未配置时解锁接口一律拒绝。 */
    @org.springframework.beans.factory.annotation.Value("${payroll.unlock-code:}")
    private String unlockCode;
    private static final BigDecimal TWENTY_TWO = new BigDecimal("22");
    private static final BigDecimal STD_DAYS = new BigDecimal("22");
    private static final BigDecimal HOURLY_DIV = new BigDecimal("21.75").multiply(new BigDecimal("8"));
    private static final BigDecimal TAX_THRESHOLD = new BigDecimal("5000");

    /** 校验结果：当前用户能否管理薪酬，以及门店范围。 */
    private Map<String, Object> checkPayrollAccess() {
        Long currentStaffId = UserContext.getStaffId();
        if (currentStaffId == null) {
            throw new SecurityException("未登录，无法访问薪酬数据");
        }
        List<Map<String, Object>> userRows = this.jdbc.queryForList(
                "SELECT store_id, can_view_all_stores, can_manage_hr FROM staff_master WHERE staff_id = ? LIMIT 1",
                currentStaffId.intValue());
        if (userRows.isEmpty()) {
            throw new SecurityException("无权访问薪酬数据");
        }
        Map<String, Object> userRow = userRows.get(0);
        int canManageHr = userRow.get("can_manage_hr") == null ? 0 : ((Number) userRow.get("can_manage_hr")).intValue();
        if (canManageHr != 1) {
            throw new SecurityException("无权访问薪酬数据");
        }
        int canViewAllStores = userRow.get("can_view_all_stores") == null ? 0 : ((Number) userRow.get("can_view_all_stores")).intValue();
        Long userStoreId = userRow.get("store_id") == null ? null : ((Number) userRow.get("store_id")).longValue();
        boolean isAllStores = UserContext.isDataScopeAll() || canViewAllStores == 1;
        if (!isAllStores && (userStoreId == null || userStoreId <= 0)) throw new SecurityException("薪酬门店范围缺失");
        Map<String, Object> access = new HashMap<>();
        access.put("isAllStores", isAllStores);
        access.put("userStoreId", userStoreId);
        access.put("currentStaffId", currentStaffId);
        return access;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> getPayroll(@RequestParam(value="month") String month) {
        try {
            Map<String, Object> access = checkPayrollAccess();
            boolean isAllStores = (Boolean) access.get("isAllStores");
            Long userStoreId = (Long) access.get("userStoreId");

            YearMonth ym = YearMonth.parse(month);
            LocalDate monthStart = ym.atDay(1);
            LocalDate monthEnd = ym.atEndOfMonth();
            // P1-15 薪资字段独立：薪资明细从 month_salary 表读取（LEFT JOIN，无记录时回退到 staff_master）
            //   映射关系：month_salary.base_salary ↔ staff_master.basic_salary(已弃用)
            //             month_salary.performance_salary ↔ staff_master.performance_salary(已弃用)
            //             month_salary.other_allowance ↔ staff_master.subsidy(已弃用)
            //             month_salary.reward_amount ↔ staff_master.bonus(已弃用)
            //             month_salary.social_security_deduction ↔ staff_master.social_insurance(已弃用)
            //             month_salary.housing_fund_deduction ↔ staff_master.housing_fund(已弃用)
            String staffSql = "SELECT s.staff_id, s.staff_name, s.department, "
                    + "COALESCE(m.base_salary, s.basic_salary, COALESCE(s.monthly_salary, 0)) AS basic_salary, "
                    + "COALESCE(m.performance_salary, s.performance_salary, 0) AS performance_salary, "
                    + "COALESCE(m.other_allowance, s.subsidy, 0) AS subsidy, "
                    + "COALESCE(m.reward_amount, s.bonus, 0) AS bonus, "
                    + "COALESCE(m.social_security_deduction, s.social_insurance, 0) AS social_insurance, "
                    + "COALESCE(m.housing_fund_deduction, s.housing_fund, 0) AS housing_fund, "
                    + "m.status AS salary_status, m.salary_id AS persisted_salary_id, m.post_salary_snapshot, m.attendance_pay_snapshot, "
                    + "m.overtime_pay AS saved_overtime, m.gross_salary AS saved_gross, m.tax_amount AS saved_tax, m.net_salary AS saved_net "
                    + "FROM staff_master s "
                    + "LEFT JOIN month_salary m ON m.staff_id = s.staff_id AND m.store_id = s.store_id AND m.salary_month = ? "
                    + "WHERE (s.employment_status <> 'resigned' OR s.employment_status IS NULL)";
            List<Object> staffParams = new ArrayList<>();
            staffParams.add(month);
            if (!isAllStores && userStoreId != null) {
                staffSql += " AND s.store_id = ?";
                staffParams.add(userStoreId);
            }
            staffSql += " ORDER BY s.staff_id";
            List<Map<String, Object>> staffRows = this.jdbc.queryForList(staffSql, staffParams.toArray());
            HashMap<Integer, BigDecimal> presentMap = new HashMap<Integer, BigDecimal>();
            List<Map<String, Object>> attRows = this.jdbc.queryForList("SELECT CAST(staff_id AS UNSIGNED) AS sid, MAX(total_present) AS present FROM attendance_records WHERE month = ? AND staff_id IS NOT NULL GROUP BY sid", new Object[]{month});
            for (Map r : attRows) {
                Number sid = (Number)r.get("sid");
                if (sid == null) continue;
                BigDecimal present = this.toBd(r.get("present"));
                presentMap.put(sid.intValue(), present);
            }
            HashMap<Integer, BigDecimal> otHoursMap = new HashMap<Integer, BigDecimal>();
            List<Map<String, Object>> otRows = this.jdbc.queryForList("SELECT staff_id, COALESCE(SUM(hours),0) AS hrs FROM overtime WHERE overtime_date BETWEEN ? AND ? AND staff_id IS NOT NULL GROUP BY staff_id", new Object[]{monthStart, monthEnd});
            for (Map r : otRows) {
                Number sid = (Number)r.get("staff_id");
                if (sid == null) continue;
                BigDecimal hrs = this.toBd(r.get("hrs"));
                otHoursMap.put(sid.intValue(), hrs);
            }
            HashMap<Integer, Integer> rewardMap = new HashMap<Integer, Integer>();
            List<Map<String, Object>> kpiRows = this.jdbc.queryForList("SELECT staff_id, COALESCE(reward_count,0) AS rc FROM report_staff_kpi WHERE stat_month = ? AND staff_id IS NOT NULL", new Object[]{month});
            for (Map r : kpiRows) {
                Number sid = (Number)r.get("staff_id");
                if (sid == null) continue;
                Number rc = (Number)r.get("rc");
                rewardMap.put(sid.intValue(), rc == null ? 0 : rc.intValue());
            }
            ArrayList result = new ArrayList();
            for (Map s : staffRows) {
                int sid = ((Number)s.get("staff_id")).intValue();
                String name = s.get("staff_name") == null ? "" : s.get("staff_name").toString();
                String dept = s.get("department") == null ? "" : s.get("department").toString();
                BigDecimal base = this.toBd(s.get("basic_salary"));
                BigDecimal post = this.toBd(s.get("performance_salary"));
                BigDecimal subsidy = this.toBd(s.get("subsidy"));
                BigDecimal bonusField = this.toBd(s.get("bonus"));
                BigDecimal social = this.toBd(s.get("social_insurance"));
                BigDecimal housing = this.toBd(s.get("housing_fund"));
                BigDecimal present = (BigDecimal)presentMap.get(sid);
                BigDecimal attendancePay = present != null ? present.divide(STD_DAYS, 6, RoundingMode.HALF_UP).multiply(base).multiply(new BigDecimal("0.3")) : base.multiply(new BigDecimal("0.3"));
                BigDecimal otHours = otHoursMap.getOrDefault(sid, BigDecimal.ZERO);
                BigDecimal overtimePay = BigDecimal.ZERO;
                if (base.signum() > 0 && otHours.signum() > 0) {
                    BigDecimal hourly = base.divide(HOURLY_DIV, 6, RoundingMode.HALF_UP);
                    overtimePay = hourly.multiply(otHours).multiply(new BigDecimal("1.5"));
                }
                int rewardCount = rewardMap.getOrDefault(sid, 0);
                BigDecimal bonus = bonusField.add(new BigDecimal(rewardCount).multiply(new BigDecimal("100")));
                BigDecimal allowance = subsidy;
                BigDecimal gross = base.add(post).add(attendancePay).add(overtimePay).add(bonus).add(allowance);
                BigDecimal dedSocial = social;
                BigDecimal dedOther = housing;
                BigDecimal taxable = gross.subtract(dedSocial).subtract(dedOther).subtract(TAX_THRESHOLD);
                BigDecimal tax = this.calcTax(taxable);
                BigDecimal net = gross.subtract(dedSocial).subtract(tax).subtract(dedOther);
                if (s.get("persisted_salary_id") != null) {
                    // An already saved payroll is a financial snapshot, not a fresh attendance calculation.
                    post = s.get("post_salary_snapshot") == null ? post : this.toBd(s.get("post_salary_snapshot"));
                    attendancePay = this.toBd(s.get("attendance_pay_snapshot"));
                    overtimePay = this.toBd(s.get("saved_overtime"));
                    bonus = bonusField;
                    gross = this.toBd(s.get("saved_gross"));
                    tax = this.toBd(s.get("saved_tax"));
                    net = this.toBd(s.get("saved_net"));
                }
                LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
                item.put("emp_id", sid);
                item.put("emp_name", name);
                item.put("department", dept);
                item.put("base_salary", this.round2(base));
                item.put("post_salary", this.round2(post));
                item.put("attendance_pay", this.round2(attendancePay));
                item.put("overtime_pay", this.round2(overtimePay));
                item.put("bonus", this.round2(bonus));
                item.put("allowance", this.round2(allowance));
                item.put("deduction_social", this.round2(dedSocial));
                item.put("deduction_tax", this.round2(tax));
                item.put("deduction_other", this.round2(dedOther));
                item.put("gross_pay", this.round2(gross));
                item.put("net_pay", this.round2(net));
                Number salaryStatus = (Number) s.get("salary_status");
                item.put("salary_status", salaryStatus == null ? 0 : salaryStatus.intValue());
                result.add(item);
            }
            return Result.success(result);
        }
        catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        }
        catch (Exception e) {
            return Result.error((int)500, (String)("\u83b7\u53d6\u85aa\u916c\u5931\u8d25: " + e.getMessage()));
        }
    }

    /**
     * \u4fdd\u5b58/\u6838\u7b97\u672c\u6708\u5de5\u8d44\uff1a\u628a getPayroll() \u7b97\u51fa\u6765\u7684\u6570\u5b57\u843d\u5e93\u5230 month_salary\uff0c
     * \u8fd9\u6837\u6708\u5e95\u53d1\u5de5\u8d44\u624d\u6709\u771f\u6b63\u7684\u5b58\u6863\uff0c\u4e0d\u518d\u662f\u6bcf\u6b21\u5237\u65b0\u90fd\u91cd\u7b97\u7684"\u8fc7\u773c\u4e91\u70df"\u3002
     * \u8bf7\u6c42\u4f53\u53ef\u9009\u4f20 items\uff08\u524d\u7aef\u5df2\u7ecf\u7b97\u597d\u3001\u53ef\u80fd\u5305\u542b\u624b\u5de5\u8c03\u6574\u7684\u6570\u7ec4\uff09\uff0c
     * \u4e0d\u4f20\u5219\u670d\u52a1\u7aef\u6309 getPayroll() \u540c\u4e00\u5957\u903b\u8f91\u91cd\u65b0\u7b97\u4e00\u904d\u518d\u5b58\uff0c\u907f\u514d\u4fe1\u4efb\u5ba2\u6237\u7aef\u6570\u5b57\u3002
     */
    @PostMapping(value={"/save"})
    public Result<Map<String, Object>> savePayroll(
            @RequestParam(value = "month") String month,
            @RequestBody(required = false) List<Map<String, Object>> items) {
        try {
            Map<String, Object> access = checkPayrollAccess();
            List<Map<String, Object>> rows = items != null && !items.isEmpty()
                    ? items
                    : getPayroll(month).getData();
            return Result.success(payrollService.save(month, rows, context(access)));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (com.youjian.banquet.service.PayrollService.PayrollRejectedException e) {
            // 输入或流程不满足条件：整批已回滚，一行都没写。回 400 让调用方知道该改什么，
            // 而不是回 500 让人以为是服务端故障去重试。
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "保存薪资失败: " + e.getMessage());
        }
    }

    /**
     * 审批本月工资：已保存(1) → 已审批(2)。
     * 只有真人且在批复白名单内可调用；这一步是发放记账的前置。
     */
    @PostMapping(value={"/approve"})
    public Result<Map<String, Object>> approvePayroll(@RequestParam(value = "month") String month) {
        try {
            return Result.success(payrollService.approve(month, context(checkPayrollAccess())));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (com.youjian.banquet.service.PayrollService.PayrollRejectedException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "审批工资失败: " + e.getMessage());
        }
    }

    /**
     * 发放记账：已审批(2) → 已发放记账(3)，并写一条台账。
     * <p>
     * <b>这一步只记账，不付款。</b>系统不对接银行、不执行任何实际资金支付；
     * 是否到账以银行流水为准。路径保留 /pay 是为了前端兼容，语义以 /payout 为准。
     */
    @PostMapping(value={"/pay", "/payout"})
    public Result<Map<String, Object>> payPayroll(@RequestParam(value = "month") String month) {
        try {
            return Result.success(payrollService.payout(month, context(checkPayrollAccess())));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (com.youjian.banquet.service.PayrollService.PayrollRejectedException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "发放记账失败: " + e.getMessage());
        }
    }

    private com.youjian.banquet.service.PayrollService.PayrollContext context(Map<String, Object> access) {
        return new com.youjian.banquet.service.PayrollService.PayrollContext(
                (Boolean) access.get("isAllStores"),
                (Long) access.get("userStoreId"),
                (Long) access.get("currentStaffId"),
                UserContext.getUsername());
    }

    @PostMapping(value={"/unlock"})
    public Result<Map<String, String>> unlock(@RequestBody Map<String, String> body) {
        try {
            String code;
            String string = code = body == null ? null : body.get("code");
            // 原来这里硬编码着一串六位数字当解锁码，源码里明文可见、改一次要发一次版，
            // 属于上线清单里"无硬编码密码/Token"那一条的反例。改为从配置读，未配置一律拒绝。
            if (unlockCode != null && !unlockCode.isBlank() && unlockCode.equals(code)) {
                String token = "payroll-" + System.currentTimeMillis();
                HashMap<String, String> data = new HashMap<>();
                data.put("token", token);
                return Result.success(data);
            }
            return Result.error((int)401, (String)"\u9a8c\u8bc1\u7801\u9519\u8bef");
        }
        catch (Exception e) {
            return Result.error((int)500, (String)("\u89e3\u9501\u5931\u8d25: " + e.getMessage()));
        }
    }

    @PostMapping(value={"/lock"})
    public Result<Void> lock(@RequestBody(required=false) Map<String, String> body) {
        return Result.success();
    }

    private BigDecimal toBd(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        if (o instanceof BigDecimal) {
            return (BigDecimal)o;
        }
        if (o instanceof Number) {
            return new BigDecimal(o.toString());
        }
        try {
            return new BigDecimal(o.toString());
        }
        catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal round2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcTax(BigDecimal taxable) {
        if (taxable == null || taxable.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal tax = taxable.compareTo(new BigDecimal("3000")) <= 0 ? taxable.multiply(new BigDecimal("0.03")) : (taxable.compareTo(new BigDecimal("12000")) <= 0 ? taxable.multiply(new BigDecimal("0.1")).subtract(new BigDecimal("210")) : (taxable.compareTo(new BigDecimal("25000")) <= 0 ? taxable.multiply(new BigDecimal("0.2")).subtract(new BigDecimal("1410")) : (taxable.compareTo(new BigDecimal("35000")) <= 0 ? taxable.multiply(new BigDecimal("0.25")).subtract(new BigDecimal("2660")) : (taxable.compareTo(new BigDecimal("55000")) <= 0 ? taxable.multiply(new BigDecimal("0.3")).subtract(new BigDecimal("4410")) : (taxable.compareTo(new BigDecimal("80000")) <= 0 ? taxable.multiply(new BigDecimal("0.35")).subtract(new BigDecimal("7160")) : taxable.multiply(new BigDecimal("0.45")).subtract(new BigDecimal("15160")))))));
        return tax.max(BigDecimal.ZERO);
    }
}

