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
    private static final BigDecimal TWENTY_TWO = new BigDecimal("22");
    private static final BigDecimal STD_DAYS = new BigDecimal("22");
    private static final BigDecimal HOURLY_DIV = new BigDecimal("21.75").multiply(new BigDecimal("8"));
    private static final BigDecimal TAX_THRESHOLD = new BigDecimal("5000");

    private int flagValue(Object value) {
        if (value == null) return 0;
        if (value instanceof Boolean bool) return bool ? 1 : 0;
        if (value instanceof Number number) return number.intValue();
        return ("1".equals(value.toString()) || "true".equalsIgnoreCase(value.toString())) ? 1 : 0;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> getPayroll(@RequestParam(value="month") String month) {
        try {
            // === S级越权漏洞修复：角色权限校验 ===
            Long currentStaffId = UserContext.getStaffId();
            if (currentStaffId == null) {
                return Result.error(401, "未登录，无法获取薪酬数据");
            }
            List<Map<String, Object>> userRows = this.jdbc.queryForList(
                    "SELECT store_id, can_view_all_stores, can_manage_hr FROM staff_master WHERE staff_id = ? LIMIT 1",
                    currentStaffId.intValue());
            if (userRows.isEmpty()) {
                return Result.error(403, "无权查看薪酬数据");
            }
            Map<String, Object> userRow = userRows.get(0);
            int canManageHr = flagValue(userRow.get("can_manage_hr"));
            int canViewAllStores = flagValue(userRow.get("can_view_all_stores"));
            Long userStoreId = userRow.get("store_id") == null ? null : ((Number) userRow.get("store_id")).longValue();
            // 普通员工不可查看薪酬数据
            if (canManageHr != 1) {
                return Result.error(403, "无权查看薪酬数据");
            }
            // 总经理可查看所有门店，店长仅本店
            boolean isAllStores = UserContext.isDataScopeAll() || canViewAllStores == 1;

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
                    + "COALESCE(m.housing_fund_deduction, s.housing_fund, 0) AS housing_fund "
                    + "FROM staff_master s "
                    + "LEFT JOIN month_salary m ON m.staff_id = s.staff_id AND m.salary_month = ? "
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
                result.add(item);
            }
            return Result.success(result);
        }
        catch (Exception e) {
            return Result.error((int)500, (String)("\u83b7\u53d6\u85aa\u916c\u5931\u8d25: " + e.getMessage()));
        }
    }

    @PostMapping(value={"/unlock"})
    public Result<Map<String, String>> unlock(@RequestBody Map<String, String> body) {
        try {
            String code;
            String string = code = body == null ? null : body.get("code");
            if ("002323".equals(code)) {
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

