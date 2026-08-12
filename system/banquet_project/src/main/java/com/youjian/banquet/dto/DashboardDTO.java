package com.youjian.banquet.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 数据大屏 DTO。所有字段均由 DashboardService 实时从数据库聚合而来，不允许前端再做任何硬编码、随机数或按比例推导。
 */
public class DashboardDTO {
    /* ====================== KPI 经营指标 6 张卡片 ====================== */
    /** 今日已确认预订总营收（多店可空，单店可空） */
    private BigDecimal todayRevenue = BigDecimal.ZERO;
    /** 昨日已确认预订总营收（用于趋势计算） */
    private BigDecimal yesterdayRevenue = BigDecimal.ZERO;
    /** 上周同日已确认预订总营收（用于趋势计算） */
    private BigDecimal lastWeekRevenue = BigDecimal.ZERO;
    /** 各门店营收明细 key=storeId(Long.toString), value=营收 */
    private Map<String, BigDecimal> revenueByStore = Map.of();
    /** 今日客流（已确认预订 guestCount 之和） */
    private long todayTraffic = 0L;
    /** 翻台率：当日 booking_table 行数 / table_master 总行数 × 100，保留 1 位小数 */
    private double turnoverRate = 0.0;
    /** 综合毛利率：基于 BookingDishDetail × (DishMaster.salePrice - DishMaster.costPrice)，加权平均，0~100 */
    private double grossMarginRate = 0.0;
    /** 预估净利：营收 - 食材成本 - 人工 - 能耗 */
    private BigDecimal netProfitEstimate = BigDecimal.ZERO;
    /** 成本拆分：key=food/labor/energy, value=占营收百分比（0~100） */
    private Map<String, Double> costBreakdown = Map.of();
    /** 今日订单数（BookingMaster，状态 confirmed） */
    private long orderCount = 0L;
    /** 订单按渠道（occasionType 分类）key=banquet/dine_in/... value=订单数 */
    private Map<String, Long> orderByChannel = Map.of();
    /** 营收同比百分比（今日 vs 昨日） */
    private double revenueTrendPct = 0.0;
    /** 客流同比百分比 */
    private double trafficTrendPct = 0.0;
    /** 翻台率同比绝对值差 */
    private double turnoverTrendPct = 0.0;
    /** 毛利率同比绝对值差 */
    private double grossMarginTrendPct = 0.0;
    /** 净利同比百分比 */
    private double netProfitTrendPct = 0.0;
    /** 订单数同比百分比 */
    private double orderTrendPct = 0.0;

    /* ====================== 累计统计（用于顶栏辅助展示） ====================== */
    private long totalCustomers = 0L;
    private long totalDishes = 0L;
    private long totalPackages = 0L;
    private long totalTables = 0L;
    private long lowStockCount = 0L;
    private long pendingPurchases = 0L;

    /* ====================== 最近预订列表 ====================== */
    private List<Map<String, Object>> recentBookings = List.of();
    /** 状态分布 key=bookingStatus, value=金额合计 */
    private Map<String, BigDecimal> statusBreakdown = Map.of();

    /* ====================== 预定看板 ====================== */
    private long todayBoxBookings = 0L;
    private long todayBanquetBookings = 0L;
    private long emptyBoxWarningCount = 0L;
    private long tomorrowTotal = 0L;
    private long tomorrowLunch = 0L;
    private long tomorrowDinner = 0L;
    /** 今日包厢预定明细（最多 4 条） */
    private List<Map<String, Object>> todayBoxList = List.of();
    /** 今日宴席预定明细（最多 3 条） */
    private List<Map<String, Object>> todayBanquetList = List.of();
    /** 空闲包厢明细（最多 4 条） */
    private List<Map<String, Object>> emptyBoxList = List.of();

    /* ====================== 待办审批 ====================== */
    /** 待审批总数 */
    private long pendingApprovalCount = 0L;
    /** 各类型待审批数 key=flowType, value=count */
    private Map<String, Long> approvalByType = Map.of();
    /** 待审批列表（最多 10 条） */
    private List<Map<String, Object>> pendingApprovalList = List.of();

    /* ====================== 风险预警 ====================== */
    private List<Map<String, Object>> riskWarnings = List.of();
    /** 风险计数 key=lowStock/pendingPurchase/pendingApproval/emptyBox, value=数量 */
    private Map<String, Long> riskCounts = Map.of();

    /* ====================== 业务看板 badge ====================== */
    private Map<String, Long> navBadges = Map.of();

    /* ====================== Accessors ====================== */
    public BigDecimal getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(BigDecimal todayRevenue) { this.todayRevenue = todayRevenue == null ? BigDecimal.ZERO : todayRevenue; }
    public BigDecimal getYesterdayRevenue() { return yesterdayRevenue; }
    public void setYesterdayRevenue(BigDecimal yesterdayRevenue) { this.yesterdayRevenue = yesterdayRevenue == null ? BigDecimal.ZERO : yesterdayRevenue; }
    public BigDecimal getLastWeekRevenue() { return lastWeekRevenue; }
    public void setLastWeekRevenue(BigDecimal lastWeekRevenue) { this.lastWeekRevenue = lastWeekRevenue == null ? BigDecimal.ZERO : lastWeekRevenue; }
    public Map<String, BigDecimal> getRevenueByStore() { return revenueByStore; }
    public void setRevenueByStore(Map<String, BigDecimal> revenueByStore) { this.revenueByStore = revenueByStore == null ? Map.of() : revenueByStore; }
    public long getTodayTraffic() { return todayTraffic; }
    public void setTodayTraffic(long todayTraffic) { this.todayTraffic = todayTraffic; }
    public double getTurnoverRate() { return turnoverRate; }
    public void setTurnoverRate(double turnoverRate) { this.turnoverRate = turnoverRate; }
    public double getGrossMarginRate() { return grossMarginRate; }
    public void setGrossMarginRate(double grossMarginRate) { this.grossMarginRate = grossMarginRate; }
    public BigDecimal getNetProfitEstimate() { return netProfitEstimate; }
    public void setNetProfitEstimate(BigDecimal netProfitEstimate) { this.netProfitEstimate = netProfitEstimate == null ? BigDecimal.ZERO : netProfitEstimate; }
    public Map<String, Double> getCostBreakdown() { return costBreakdown; }
    public void setCostBreakdown(Map<String, Double> costBreakdown) { this.costBreakdown = costBreakdown == null ? Map.of() : costBreakdown; }
    public long getOrderCount() { return orderCount; }
    public void setOrderCount(long orderCount) { this.orderCount = orderCount; }
    public Map<String, Long> getOrderByChannel() { return orderByChannel; }
    public void setOrderByChannel(Map<String, Long> orderByChannel) { this.orderByChannel = orderByChannel == null ? Map.of() : orderByChannel; }
    public double getRevenueTrendPct() { return revenueTrendPct; }
    public void setRevenueTrendPct(double revenueTrendPct) { this.revenueTrendPct = revenueTrendPct; }
    public double getTrafficTrendPct() { return trafficTrendPct; }
    public void setTrafficTrendPct(double trafficTrendPct) { this.trafficTrendPct = trafficTrendPct; }
    public double getTurnoverTrendPct() { return turnoverTrendPct; }
    public void setTurnoverTrendPct(double turnoverTrendPct) { this.turnoverTrendPct = turnoverTrendPct; }
    public double getGrossMarginTrendPct() { return grossMarginTrendPct; }
    public void setGrossMarginTrendPct(double grossMarginTrendPct) { this.grossMarginTrendPct = grossMarginTrendPct; }
    public double getNetProfitTrendPct() { return netProfitTrendPct; }
    public void setNetProfitTrendPct(double netProfitTrendPct) { this.netProfitTrendPct = netProfitTrendPct; }
    public double getOrderTrendPct() { return orderTrendPct; }
    public void setOrderTrendPct(double orderTrendPct) { this.orderTrendPct = orderTrendPct; }
    public long getTotalCustomers() { return totalCustomers; }
    public void setTotalCustomers(long totalCustomers) { this.totalCustomers = totalCustomers; }
    public long getTotalDishes() { return totalDishes; }
    public void setTotalDishes(long totalDishes) { this.totalDishes = totalDishes; }
    public long getTotalPackages() { return totalPackages; }
    public void setTotalPackages(long totalPackages) { this.totalPackages = totalPackages; }
    public long getTotalTables() { return totalTables; }
    public void setTotalTables(long totalTables) { this.totalTables = totalTables; }
    public long getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(long lowStockCount) { this.lowStockCount = lowStockCount; }
    public long getPendingPurchases() { return pendingPurchases; }
    public void setPendingPurchases(long pendingPurchases) { this.pendingPurchases = pendingPurchases; }
    public List<Map<String, Object>> getRecentBookings() { return recentBookings; }
    public void setRecentBookings(List<Map<String, Object>> recentBookings) { this.recentBookings = recentBookings == null ? List.of() : recentBookings; }
    public Map<String, BigDecimal> getStatusBreakdown() { return statusBreakdown; }
    public void setStatusBreakdown(Map<String, BigDecimal> statusBreakdown) { this.statusBreakdown = statusBreakdown == null ? Map.of() : statusBreakdown; }
    public long getTodayBoxBookings() { return todayBoxBookings; }
    public void setTodayBoxBookings(long todayBoxBookings) { this.todayBoxBookings = todayBoxBookings; }
    public long getTodayBanquetBookings() { return todayBanquetBookings; }
    public void setTodayBanquetBookings(long todayBanquetBookings) { this.todayBanquetBookings = todayBanquetBookings; }
    public long getEmptyBoxWarningCount() { return emptyBoxWarningCount; }
    public void setEmptyBoxWarningCount(long emptyBoxWarningCount) { this.emptyBoxWarningCount = emptyBoxWarningCount; }
    public long getTomorrowTotal() { return tomorrowTotal; }
    public void setTomorrowTotal(long tomorrowTotal) { this.tomorrowTotal = tomorrowTotal; }
    public long getTomorrowLunch() { return tomorrowLunch; }
    public void setTomorrowLunch(long tomorrowLunch) { this.tomorrowLunch = tomorrowLunch; }
    public long getTomorrowDinner() { return tomorrowDinner; }
    public void setTomorrowDinner(long tomorrowDinner) { this.tomorrowDinner = tomorrowDinner; }
    public List<Map<String, Object>> getTodayBoxList() { return todayBoxList; }
    public void setTodayBoxList(List<Map<String, Object>> todayBoxList) { this.todayBoxList = todayBoxList == null ? List.of() : todayBoxList; }
    public List<Map<String, Object>> getTodayBanquetList() { return todayBanquetList; }
    public void setTodayBanquetList(List<Map<String, Object>> todayBanquetList) { this.todayBanquetList = todayBanquetList == null ? List.of() : todayBanquetList; }
    public List<Map<String, Object>> getEmptyBoxList() { return emptyBoxList; }
    public void setEmptyBoxList(List<Map<String, Object>> emptyBoxList) { this.emptyBoxList = emptyBoxList == null ? List.of() : emptyBoxList; }
    public long getPendingApprovalCount() { return pendingApprovalCount; }
    public void setPendingApprovalCount(long pendingApprovalCount) { this.pendingApprovalCount = pendingApprovalCount; }
    public Map<String, Long> getApprovalByType() { return approvalByType; }
    public void setApprovalByType(Map<String, Long> approvalByType) { this.approvalByType = approvalByType == null ? Map.of() : approvalByType; }
    public List<Map<String, Object>> getPendingApprovalList() { return pendingApprovalList; }
    public void setPendingApprovalList(List<Map<String, Object>> pendingApprovalList) { this.pendingApprovalList = pendingApprovalList == null ? List.of() : pendingApprovalList; }
    public List<Map<String, Object>> getRiskWarnings() { return riskWarnings; }
    public void setRiskWarnings(List<Map<String, Object>> riskWarnings) { this.riskWarnings = riskWarnings == null ? List.of() : riskWarnings; }
    public Map<String, Long> getRiskCounts() { return riskCounts; }
    public void setRiskCounts(Map<String, Long> riskCounts) { this.riskCounts = riskCounts == null ? Map.of() : riskCounts; }
    public Map<String, Long> getNavBadges() { return navBadges; }
    public void setNavBadges(Map<String, Long> navBadges) { this.navBadges = navBadges == null ? Map.of() : navBadges; }
}
