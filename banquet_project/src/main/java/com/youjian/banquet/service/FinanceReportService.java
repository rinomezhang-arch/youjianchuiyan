package com.youjian.banquet.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 财务报表服务（JdbcTemplate 聚合）。
 * 对应 GET /api/finance/profit-report 与 GET /api/finance/balance-report。
 * <p>storeId 为 null 时（总经理查全门店）不按门店过滤；所有 SUM 查询均使用 sumOrZero 兜底。
 */
@Service
public class FinanceReportService {

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 利润表：营业收入(booking_master) / 食材成本(goods_receipt) / 人工成本(month_salary) / 费用(finance_expense)。
     */
    public Map<String, Object> profitReport(Long storeId, String month) {
        YearMonth ym = parseMonth(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.plusMonths(1).atDay(1);

        // 营业收入
        List<Object> revParams = new ArrayList<>();
        String revSql = "SELECT COALESCE(SUM(COALESCE(final_amount, total_amount, 0)),0) FROM booking_master"
                + " WHERE booking_date >= ? AND booking_date < ?";
        revParams.add(Date.valueOf(start));
        revParams.add(Date.valueOf(end));
        if (storeId != null) {
            revSql += " AND store_id=?";
            revParams.add(storeId);
        }
        BigDecimal revenue = sumOrZero(revSql, revParams.toArray());

        // 食材成本
        List<Object> foodParams = new ArrayList<>();
        String foodSql = "SELECT COALESCE(SUM(total_amount),0) FROM goods_receipt"
                + " WHERE receipt_date >= ? AND receipt_date < ?";
        foodParams.add(Date.valueOf(start));
        foodParams.add(Date.valueOf(end));
        if (storeId != null) {
            foodSql += " AND store_id=?";
            foodParams.add(storeId);
        }
        BigDecimal foodCost = sumOrZero(foodSql, foodParams.toArray());

        // 人工成本
        List<Object> laborParams = new ArrayList<>();
        String laborSql = "SELECT COALESCE(SUM(gross_salary),0) FROM month_salary WHERE salary_month=?";
        laborParams.add(month);
        if (storeId != null) {
            laborSql += " AND store_id=?";
            laborParams.add(storeId);
        }
        BigDecimal laborCost = sumOrZero(laborSql, laborParams.toArray());

        // 费用
        List<Object> expParams = new ArrayList<>();
        String expSql = "SELECT COALESCE(SUM(amount),0) FROM finance_expense"
                + " WHERE expense_date >= ? AND expense_date < ?";
        expParams.add(Date.valueOf(start));
        expParams.add(Date.valueOf(end));
        if (storeId != null) {
            expSql += " AND store_id=?";
            expParams.add(storeId);
        }
        BigDecimal expense = sumOrZero(expSql, expParams.toArray());

        BigDecimal totalCost = foodCost.add(laborCost).add(expense);
        BigDecimal grossProfit = revenue.subtract(foodCost);
        BigDecimal operatingProfit = grossProfit.subtract(laborCost).subtract(expense);

        Map<String, Object> revenueItems = new LinkedHashMap<>();
        revenueItems.put("营业总收入", revenue);

        Map<String, Object> costItems = new LinkedHashMap<>();
        costItems.put("食材成本", foodCost);
        costItems.put("人工成本", laborCost);
        costItems.put("费用支出", expense);
        costItems.put("成本合计", totalCost);

        Map<String, Object> expenseItems = new LinkedHashMap<>();
        expenseItems.put("费用", expense);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("month", month);
        data.put("revenue", revenue);
        data.put("revenueItems", revenueItems);
        data.put("costItems", costItems);
        data.put("expenseItems", expenseItems);
        data.put("totalCost", totalCost);
        data.put("grossProfit", grossProfit);
        data.put("operatingProfit", operatingProfit);
        data.put("netProfit", operatingProfit);
        data.put("grossMargin", percent(grossProfit, revenue));
        data.put("netMargin", percent(operatingProfit, revenue));
        return data;
    }

    /**
     * 资产负债表：资产(finance_account余额 + 库存价值) / 负债(finance_payable未付) / 权益。
     */
    public Map<String, Object> balanceReport(Long storeId, String month) {
        parseMonth(month); // 仅做格式校验

        BigDecimal fundBalance = sumByStore("SELECT COALESCE(SUM(current_balance),0) FROM finance_account",
                " AND is_active=1", storeId);

        BigDecimal inventoryValue = sumByStore("SELECT COALESCE(SUM(total_cost),0) FROM inventory_summary",
                "", storeId);

        BigDecimal payable = sumByStore(
                "SELECT COALESCE(SUM(pending_amount),0) FROM finance_payable",
                "", storeId);

        BigDecimal receivable = sumByStore("SELECT COALESCE(SUM(pending_amount),0) FROM finance_receivable",
                "", storeId);

        BigDecimal totalAssets = fundBalance.add(inventoryValue);
        BigDecimal totalLiabilities = payable;
        BigDecimal equity = totalAssets.subtract(totalLiabilities);

        Map<String, Object> assets = new LinkedHashMap<>();
        assets.put("资金账户余额", fundBalance);
        assets.put("库存价值", inventoryValue);
        assets.put("资产合计", totalAssets);

        Map<String, Object> liabilities = new LinkedHashMap<>();
        liabilities.put("应付账款", payable);
        liabilities.put("应收账款", receivable);
        liabilities.put("负债合计", totalLiabilities);

        Map<String, Object> equityItems = new LinkedHashMap<>();
        equityItems.put("所有者权益", equity);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("month", month);
        data.put("assets", assets);
        data.put("liabilities", liabilities);
        data.put("equity", equityItems);
        data.put("totalAssets", totalAssets);
        data.put("totalLiabilities", totalLiabilities);
        data.put("totalEquity", equity);
        return data;
    }

    /** 按 storeId 聚合：storeId 为 null 时不加门店过滤，extraWhere 为附加固定条件（如 is_active=1）。 */
    private BigDecimal sumByStore(String selectPart, String extraWhere, Long storeId) {
        StringBuilder sql = new StringBuilder(selectPart).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (extraWhere != null && !extraWhere.isEmpty()) {
            sql.append(extraWhere);
        }
        if (storeId != null) {
            sql.append(" AND store_id=?");
            params.add(storeId);
        }
        return sumOrZero(sql.toString(), params.toArray());
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isEmpty()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (Exception e) {
            return YearMonth.now();
        }
    }

    private BigDecimal percent(BigDecimal part, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return part.divide(total, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal sumOrZero(String sql, Object... args) {
        try {
            BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, args);
            return v == null ? BigDecimal.ZERO : v;
        } catch (Exception e) {
            // 表或列不存在时返回 0，保证报表整体可用
            return BigDecimal.ZERO;
        }
    }
}
