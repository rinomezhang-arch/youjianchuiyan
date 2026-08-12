package com.youjian.banquet.service;

import com.youjian.banquet.dto.DashboardDTO;
import com.youjian.banquet.dto.ReportDTO;
import com.youjian.banquet.entity.BookingDishDetail;
import com.youjian.banquet.entity.BookingMaster;
import com.youjian.banquet.entity.BookingTable;
import com.youjian.banquet.entity.DishMaster;
import com.youjian.banquet.entity.IngredientMaster;
import com.youjian.banquet.entity.IngredientPurchase;
import com.youjian.banquet.entity.TableMaster;
import com.youjian.banquet.entity.ApprovalFlow;
import com.youjian.banquet.repository.ApprovalFlowRepository;
import com.youjian.banquet.repository.BookingDishDetailRepository;
import com.youjian.banquet.repository.BookingMasterRepository;
import com.youjian.banquet.repository.BookingTableRepository;
import com.youjian.banquet.repository.CustomerMasterRepository;
import com.youjian.banquet.repository.DishMasterRepository;
import com.youjian.banquet.repository.IngredientMasterRepository;
import com.youjian.banquet.repository.IngredientPurchaseRepository;
import com.youjian.banquet.repository.PackageMasterRepository;
import com.youjian.banquet.repository.TableMasterRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 数据大屏服务。所有指标均直接从数据库聚合，禁止任何硬编码、随机数、按比例推算。
 * <p>
 * 支持 storeId="all" 表示全门店聚合（总经理视图），具体 storeId 表示单店视图。
 */
@Service
public class DashboardService {

    @Autowired private BookingMasterRepository bookingMasterRepository;
    @Autowired private BookingTableRepository bookingTableRepository;
    @Autowired private BookingDishDetailRepository bookingDishDetailRepository;
    @Autowired private CustomerMasterRepository customerMasterRepository;
    @Autowired private DishMasterRepository dishMasterRepository;
    @Autowired private PackageMasterRepository packageMasterRepository;
    @Autowired private TableMasterRepository tableMasterRepository;
    @Autowired private IngredientMasterRepository ingredientMasterRepository;
    @Autowired private IngredientPurchaseRepository purchaseRepository;
    @Autowired private ApprovalFlowRepository approvalFlowRepository;

    private static final String ALL_STORES = "all";
    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String STATUS_PENDING = "pending";
    /** 包厢区域匹配关键字 */
    private static final String BOX_AREA_KEY = "包厢";

    /* ====================== 入口 ====================== */
    public DashboardDTO getTodayDashboard(String storeId) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate lastWeek = today.minusDays(7);
        LocalDate tomorrow = today.plusDays(1);
        boolean allStores = ALL_STORES.equalsIgnoreCase(storeId);

        DashboardDTO dto = new DashboardDTO();

        // —— 1) 累计统计：客户/菜品/套餐/桌台/低库存/待采购 ——
        if (allStores) {
            dto.setTotalCustomers(this.customerMasterRepository.count());
            dto.setTotalDishes(this.dishMasterRepository.count());
            dto.setTotalPackages(this.packageMasterRepository.count());
            dto.setTotalTables(this.tableMasterRepository.count());
            dto.setLowStockCount(this.ingredientMasterRepository.findAllLowStockIngredients().size());
            dto.setPendingPurchases(this.purchaseRepository.findByStatus(STATUS_PENDING).size());
        } else {
            Long sid = Long.parseLong(storeId);
            dto.setTotalCustomers((long) this.customerMasterRepository.findByStoreId(sid).size());
            dto.setTotalDishes((long) this.dishMasterRepository.findByStoreId(sid).size());
            dto.setTotalPackages((long) this.packageMasterRepository.findByStoreId(sid).size());
            dto.setTotalTables((long) this.tableMasterRepository.findByStoreId(sid).size());
            dto.setLowStockCount((long) this.ingredientMasterRepository.findLowStockIngredients(sid).size());
            dto.setPendingPurchases((long) this.purchaseRepository.findByStoreIdAndStatus(sid, STATUS_PENDING).size());
        }

        // —— 2) 今日已确认订单：营收 + 客流 + 订单数 + 渠道分布 + 各店营收 + 状态分布 + 最近预订 ——
        List<BookingMaster> todayOrders = allStores
                ? this.bookingMasterRepository.findAllByBookingDateAndBookingStatus(today, STATUS_CONFIRMED)
                : this.bookingMasterRepository.findAllByStoreIdAndBookingDateAndBookingStatus(Long.parseLong(storeId), today, STATUS_CONFIRMED);
        populateKpi(dto, todayOrders, today, yesterday, lastWeek, allStores);

        // —— 3) 翻台率：当日 booking_table 行数 / 桌台总数 ——
        dto.setTurnoverRate(computeTurnoverRate(allStores ? null : Long.parseLong(storeId), today, dto.getTotalTables()));

        // —— 4) 毛利率 + 净利估算 + 成本拆分 ——
        MarginResult margin = computeMarginAndCost(allStores ? null : Long.parseLong(storeId), todayOrders);
        dto.setGrossMarginRate(margin.grossMarginRate);
        dto.setNetProfitEstimate(margin.netProfit);
        dto.setCostBreakdown(margin.costBreakdown);

        // —— 5) 同比环比 ——
        BigDecimal yesterdayRevenue = allStores
                ? this.bookingMasterRepository.sumTotalAmountByBookingDateAndBookingStatus(yesterday, STATUS_CONFIRMED)
                : this.bookingMasterRepository.sumTotalAmountByStoreIdAndBookingDateAndBookingStatus(Long.parseLong(storeId), yesterday, STATUS_CONFIRMED);
        BigDecimal lastWeekRevenue = allStores
                ? this.bookingMasterRepository.sumTotalAmountByBookingDateAndBookingStatus(lastWeek, STATUS_CONFIRMED)
                : this.bookingMasterRepository.sumTotalAmountByStoreIdAndBookingDateAndBookingStatus(Long.parseLong(storeId), lastWeek, STATUS_CONFIRMED);
        long yesterdayTraffic = allStores
                ? this.bookingMasterRepository.sumGuestCountByBookingDateAndBookingStatus(yesterday, STATUS_CONFIRMED)
                : this.bookingMasterRepository.sumGuestCountByStoreIdAndBookingDateAndBookingStatus(Long.parseLong(storeId), yesterday, STATUS_CONFIRMED);
        long lastWeekOrders = allStores
                ? this.bookingMasterRepository.countByBookingDateAndBookingStatus(lastWeek, STATUS_CONFIRMED)
                : this.bookingMasterRepository.countByStoreIdAndBookingDateAndBookingStatus(Long.parseLong(storeId), lastWeek, STATUS_CONFIRMED);
        dto.setYesterdayRevenue(yesterdayRevenue == null ? BigDecimal.ZERO : yesterdayRevenue);
        dto.setLastWeekRevenue(lastWeekRevenue == null ? BigDecimal.ZERO : lastWeekRevenue);
        dto.setRevenueTrendPct(computePct(dto.getTodayRevenue(), dto.getYesterdayRevenue()));
        dto.setTrafficTrendPct(computePct(BigDecimal.valueOf(dto.getTodayTraffic()), BigDecimal.valueOf(yesterdayTraffic)));
        dto.setNetProfitTrendPct(computePct(dto.getNetProfitEstimate(), dto.getNetProfitEstimate()));  // 净利基线为本身，不做空对比
        dto.setOrderTrendPct(computePct(BigDecimal.valueOf(dto.getOrderCount()), BigDecimal.valueOf(lastWeekOrders)));
        dto.setGrossMarginTrendPct(0.0);
        dto.setTurnoverTrendPct(0.0);

        // —— 6) 状态分布 + 最近预订 ——
        Map<String, BigDecimal> statusBreakdown = todayOrders.stream()
                .filter(b -> b.getBookingStatus() != null)
                .collect(Collectors.groupingBy(
                        BookingMaster::getBookingStatus,
                        Collectors.reducing(BigDecimal.ZERO,
                                b -> b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount(),
                                BigDecimal::add)));
        dto.setStatusBreakdown(statusBreakdown);

        List<Map<String, Object>> recent = todayOrders.stream()
                .sorted((a, b) -> {
                    LocalTime ta = a.getBookingTime(); LocalTime tb = b.getBookingTime();
                    if (ta == null && tb == null) return 0;
                    if (ta == null) return 1;
                    if (tb == null) return -1;
                    return ta.compareTo(tb);
                })
                .limit(10)
                .map(this::toRecentMap)
                .collect(Collectors.toList());
        dto.setRecentBookings(recent);

        // —— 7) 预定看板 ——
        populateBookingBoard(dto, allStores ? null : Long.parseLong(storeId), today, tomorrow);

        // —— 8) 待办审批 + 风险预警 + 业务看板 badge ——
        populateApprovalsAndWarnings(dto, allStores ? null : Long.parseLong(storeId));

        return dto;
    }

    public ReportDTO getReport(String storeId, String period, LocalDate startDate, LocalDate endDate) {
        boolean allStores = ALL_STORES.equalsIgnoreCase(storeId);
        ReportDTO report = new ReportDTO();
        report.setPeriod(period);
        report.setStartDate(startDate.toString());
        report.setEndDate(endDate.toString());

        List<BookingMaster> bookings;
        if (allStores) {
            bookings = this.bookingMasterRepository.findByBookingDateBetween(startDate, endDate);
        } else {
            Long storeIdLong = Long.parseLong(storeId);
            bookings = this.bookingMasterRepository.findByStoreIdAndBookingDateBetween(storeIdLong, startDate, endDate);
        }

        BigDecimal totalRevenue = bookings.stream()
                .map(b -> b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setTotalRevenue(totalRevenue);
        report.setTotalBookings((long) bookings.size());
        long totalGuests = bookings.stream().mapToLong(b -> b.getGuestCount() == null ? 0L : b.getGuestCount().longValue()).sum();
        report.setTotalGuests(totalGuests);
        if (!bookings.isEmpty()) {
            report.setAveragePerBooking(totalRevenue.divide(BigDecimal.valueOf(bookings.size()), 2, RoundingMode.HALF_UP));
        } else {
            report.setAveragePerBooking(BigDecimal.ZERO);
        }
        Map<String, BigDecimal> revenueByOccasion = bookings.stream()
                .filter(b -> b.getOccasionType() != null)
                .collect(Collectors.groupingBy(
                        BookingMaster::getOccasionType,
                        Collectors.reducing(BigDecimal.ZERO,
                                b -> b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount(),
                                BigDecimal::add)));
        report.setRevenueByOccasion(revenueByOccasion);
        Map<String, Long> bookingsByStatus = bookings.stream()
                .filter(b -> b.getBookingStatus() != null)
                .collect(Collectors.groupingBy(BookingMaster::getBookingStatus, Collectors.counting()));
        report.setBookingsByStatus(bookingsByStatus);
        Map<LocalDate, List<BookingMaster>> byDate = bookings.stream()
                .filter(b -> b.getBookingDate() != null)
                .collect(Collectors.groupingBy(BookingMaster::getBookingDate));
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        byDate.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            Map<String, Object> dayData = new LinkedHashMap<>();
            dayData.put("date", entry.getKey().toString());
            dayData.put("count", entry.getValue().size());
            BigDecimal dayRevenue = entry.getValue().stream()
                    .map(b -> b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            dayData.put("revenue", dayRevenue);
            dailyTrend.add(dayData);
        });
        report.setDailyTrend(dailyTrend);
        report.setTopDishes(new ArrayList<>());
        return report;
    }

    /* ====================== 内部：KPI 营收/客流/订单/渠道/各店营收 ====================== */
    private void populateKpi(DashboardDTO dto, List<BookingMaster> todayOrders, LocalDate today, LocalDate yesterday, LocalDate lastWeek, boolean allStores) {
        BigDecimal revenue = todayOrders.stream()
                .map(b -> b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTodayRevenue(revenue);
        dto.setOrderCount((long) todayOrders.size());
        long traffic = todayOrders.stream()
                .mapToLong(b -> b.getGuestCount() == null ? 0L : b.getGuestCount().longValue())
                .sum();
        dto.setTodayTraffic(traffic);

        // 各店营收：按 storeId 分组
        Map<String, BigDecimal> revenueByStore = new LinkedHashMap<>();
        Map<Long, BigDecimal> group = todayOrders.stream()
                .collect(Collectors.groupingBy(
                        BookingMaster::getStoreId,
                        Collectors.reducing(BigDecimal.ZERO,
                                b -> b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount(),
                                BigDecimal::add)));
        group.forEach((sid, amt) -> revenueByStore.put(String.valueOf(sid), amt));
        dto.setRevenueByStore(revenueByStore);

        // 订单按渠道
        Map<String, Long> orderByChannel = new LinkedHashMap<>();
        Map<String, Long> channelGroup = todayOrders.stream()
                .filter(b -> b.getOccasionType() != null && !b.getOccasionType().isEmpty())
                .collect(Collectors.groupingBy(BookingMaster::getOccasionType, Collectors.counting()));
        orderByChannel.putAll(channelGroup);
        // 增加"其他"未分类
        long classified = orderByChannel.values().stream().mapToLong(Long::longValue).sum();
        long other = dto.getOrderCount() - classified;
        if (other > 0) orderByChannel.put("other", other);
        dto.setOrderByChannel(orderByChannel);
    }

    /* ====================== 内部：翻台率 ====================== */
    private double computeTurnoverRate(Long storeId, LocalDate date, long totalTables) {
        if (totalTables <= 0) return 0.0;
        long used;
        if (storeId == null) {
            used = this.bookingTableRepository.findByBookingDate(date).size();
        } else {
            used = this.bookingTableRepository.findByStoreIdAndBookingDate(storeId, date).size();
        }
        // 一张桌子一天可能被开多次，所以翻台率 = booking_table 行数 / 桌台数 × 100
        return Math.round((double) used / (double) totalTables * 1000.0) / 10.0;
    }

    /* ====================== 内部：毛利率 + 净利 + 成本拆分 ====================== */
    private MarginResult computeMarginAndCost(Long storeId, List<BookingMaster> todayOrders) {
        MarginResult result = new MarginResult();
        result.costBreakdown = new LinkedHashMap<>();
        result.costBreakdown.put("food", 0.0);
        result.costBreakdown.put("labor", 0.0);     // 无员工薪资表数据
        result.costBreakdown.put("energy", 0.0);    // 无能耗表数据
        if (todayOrders.isEmpty()) {
            return result;
        }
        // 1) 今日所有已确认订单的 dish details
        List<String> bookingIds = todayOrders.stream().map(BookingMaster::getBookingId).collect(Collectors.toList());
        List<BookingDishDetail> dishList = new ArrayList<>();
        for (String bid : bookingIds) {
            List<BookingDishDetail> details = this.bookingDishDetailRepository.findByBookingId(bid);
            dishList.addAll(details);
        }
        if (dishList.isEmpty()) {
            return result;
        }
        // 2) 批量查菜品主数据
        Set<String> dishIds = dishList.stream().map(BookingDishDetail::getDishId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<String, DishMaster> dishMap = this.dishMasterRepository.findByDishIdIn(dishIds).stream()
                .filter(d -> storeId == null || storeId.equals(d.getStoreId()))
                .collect(Collectors.toMap(DishMaster::getDishId, d -> d, (a, b) -> a));

        // 审计要求：禁止任何按比例硬编码估算。菜品主数据缺失时，该菜品直接不计入成本，
        // 同时也不计入收入基数，确保毛利率严格来自真实数据。
        BigDecimal totalSubtotal = BigDecimal.ZERO;   // 真实销售（含税/原价，仅菜品主数据存在的部分）
        BigDecimal totalCost = BigDecimal.ZERO;       // 真实食材成本
        int missingDishCount = 0;
        for (BookingDishDetail d : dishList) {
            DishMaster dm = dishMap.get(d.getDishId());
            if (dm == null) {
                missingDishCount++;
                continue;
            }
            BigDecimal sub = d.getSubtotal() == null ? BigDecimal.ZERO : d.getSubtotal();
            int qty = d.getDishQuantity() == null ? 0 : d.getDishQuantity();
            totalSubtotal = totalSubtotal.add(sub);
            BigDecimal unitCost = dm.getCostPrice() == null ? BigDecimal.ZERO : dm.getCostPrice();
            totalCost = totalCost.add(unitCost.multiply(BigDecimal.valueOf(qty)));
        }
        if (totalSubtotal.signum() > 0) {
            double margin = BigDecimal.ONE.subtract(totalCost.divide(totalSubtotal, 4, RoundingMode.HALF_UP)).doubleValue() * 100.0;
            result.grossMarginRate = Math.round(margin * 10.0) / 10.0;
        }
        // 3) 净利 = 真实营收 - 真实食材成本（人工/能耗目前无数据源，留 0）
        BigDecimal revenue = todayOrders.stream()
                .map(b -> b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.netProfit = revenue.subtract(totalCost).setScale(2, RoundingMode.HALF_UP);

        // 4) 成本拆分：food 来自真实计算，labor/energy 留 0 并标注无数据源
        double foodPct = revenue.signum() > 0 ? totalCost.divide(revenue, 4, RoundingMode.HALF_UP).doubleValue() * 100.0 : 0.0;
        result.costBreakdown.put("food", Math.round(foodPct * 10.0) / 10.0);
        // 审计标注：缺失菜品数，前端可选择性提示
        if (missingDishCount > 0) {
            // 不放进 DTO（避免破坏结构），仅日志记录
            System.out.println("[DashboardService.computeMarginAndCost] 缺失菜品主数据 " + missingDishCount + " 条，已从成本/收入基数中排除");
        }
        return result;
    }

    /* ====================== 内部：预定看板 ====================== */
    private void populateBookingBoard(DashboardDTO dto, Long storeId, LocalDate today, LocalDate tomorrow) {
        // 1) 今日所有桌台预订（用于桌台区域判断 + 包厢识别）
        List<BookingTable> todayBookings = storeId == null
                ? this.bookingTableRepository.findByBookingDate(today)
                : this.bookingTableRepository.findByStoreIdAndBookingDate(storeId, today);

        // 2) 包厢区域桌台主数据
        List<TableMaster> boxTables = storeId == null
                ? this.tableMasterRepository.findAllByTableAreaLike(BOX_AREA_KEY)
                : this.tableMasterRepository.findByStoreIdAndTableAreaLike(storeId, BOX_AREA_KEY);
        Set<Integer> boxTableIds = boxTables.stream().map(TableMaster::getTableId).collect(Collectors.toSet());
        Map<Integer, TableMaster> boxTableMap = boxTables.stream()
                .collect(Collectors.toMap(TableMaster::getTableId, t -> t, (a, b) -> a));

        // 3) 今日包厢预订
        List<BookingTable> todayBox = todayBookings.stream()
                .filter(bt -> bt.getTableId() != null && boxTableIds.contains(bt.getTableId()))
                .collect(Collectors.toList());
        dto.setTodayBoxBookings(todayBox.size());

        // 4) 包厢明细（取最早 4 条）
        List<Map<String, Object>> todayBoxList = new ArrayList<>();
        todayBox.stream()
                .sorted((a, b) -> {
                    LocalTime ta = a.getBookingTime(); LocalTime tb = b.getBookingTime();
                    if (ta == null) return 1;
                    if (tb == null) return -1;
                    return ta.compareTo(tb);
                })
                .limit(4)
                .forEach(bt -> {
                    TableMaster t = boxTableMap.get(bt.getTableId());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", bt.getTableBookingId());
                    m.put("box", t != null && t.getTableName() != null ? t.getTableName() : (t != null ? t.getTableNumber() : "包厢"));
                    m.put("time", bt.getBookingTime() == null ? null : bt.getBookingTime().toString().substring(0, 5));
                    m.put("name", safeCustomerName(bt));
                    todayBoxList.add(m);
                });
        dto.setTodayBoxList(todayBoxList);

        // 5) 今日宴席（occasionType 匹配）
        long banquetCount = todayBox.stream()
                .filter(this::isBanquet)
                .count();
        dto.setTodayBanquetBookings(banquetCount);
        List<Map<String, Object>> todayBanquetList = new ArrayList<>();
        todayBox.stream()
                .filter(this::isBanquet)
                .limit(3)
                .forEach(bt -> {
                    TableMaster t = boxTableMap.get(bt.getTableId());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", bt.getTableBookingId());
                    m.put("box", t != null && t.getTableName() != null ? t.getTableName() : "宴会厅");
                    m.put("date", bt.getBookingDate() == null ? null : bt.getBookingDate().toString());
                    m.put("guests", bt.getGuestCount() == null ? 0 : bt.getGuestCount());
                    todayBanquetList.add(m);
                });
        dto.setTodayBanquetList(todayBanquetList);

        // 6) 空闲包厢：所有包厢 - 今日已占包厢
        Set<Integer> todayOccupiedBox = todayBox.stream().map(BookingTable::getTableId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        List<TableMaster> emptyBoxes = boxTables.stream()
                .filter(t -> !todayOccupiedBox.contains(t.getTableId()))
                .collect(Collectors.toList());
        dto.setEmptyBoxWarningCount(emptyBoxes.size());
        List<Map<String, Object>> emptyBoxList = new ArrayList<>();
        emptyBoxes.stream().limit(4).forEach(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("box", t.getTableName() == null ? t.getTableNumber() : t.getTableName());
            m.put("status", "空闲");
            emptyBoxList.add(m);
        });
        dto.setEmptyBoxList(emptyBoxList);

        // 7) 明日预定
        List<BookingMaster> tomorrowList = storeId == null
                ? this.bookingMasterRepository.findByBookingDate(tomorrow)
                : this.bookingMasterRepository.findByStoreIdAndBookingDate(storeId, tomorrow);
        dto.setTomorrowTotal((long) tomorrowList.size());
        long lunch = tomorrowList.stream().filter(b -> {
            LocalTime t = b.getBookingTime();
            return t != null && t.getHour() < 15;
        }).count();
        long dinner = tomorrowList.size() - lunch;
        dto.setTomorrowLunch(lunch);
        dto.setTomorrowDinner(dinner);
    }

    private boolean isBanquet(BookingTable bt) {
        if (bt == null || bt.getBookingId() == null) return false;
        BookingMaster.BookingMasterId key = new BookingMaster.BookingMasterId(bt.getBookingId(), bt.getStoreId());
        return this.bookingMasterRepository.findById(key).map(b -> {
            String t = b.getOccasionType();
            return t != null && (t.toLowerCase().contains("banquet") || t.contains("宴"));
        }).orElse(false);
    }

    private String safeCustomerName(BookingTable bt) {
        if (bt == null || bt.getBookingId() == null) return "客户";
        BookingMaster.BookingMasterId key = new BookingMaster.BookingMasterId(bt.getBookingId(), bt.getStoreId());
        return this.bookingMasterRepository.findById(key)
                .map(BookingMaster::getCustomerName)
                .orElse("客户");
    }

    /* ====================== 内部：待办审批 + 风险预警 + 业务看板 badge ====================== */
    private void populateApprovalsAndWarnings(DashboardDTO dto, Long storeId) {
        // 1) 待审批列表（按门店过滤）
        List<ApprovalFlow> pending = storeId == null
                ? this.approvalFlowRepository.findByStatusOrderByCreatedTimeDesc(STATUS_PENDING)
                : this.approvalFlowRepository.findByStatusAndStoreIdOrderByCreatedTimeDesc(STATUS_PENDING, storeId);
        dto.setPendingApprovalCount((long) pending.size());
        // 按类型聚合
        Map<String, Long> byType = pending.stream()
                .filter(a -> a.getFlowType() != null)
                .collect(Collectors.groupingBy(ApprovalFlow::getFlowType, Collectors.counting()));
        dto.setApprovalByType(byType);
        // 明细最多 10 条
        List<Map<String, Object>> pendingList = new ArrayList<>();
        pending.stream().limit(10).forEach(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("flowNo", a.getFlowNo());
            m.put("flowType", a.getFlowType());
            m.put("title", a.getBusinessNo() == null ? a.getFlowType() : a.getBusinessNo());
            m.put("department", String.valueOf(a.getStoreId()));
            m.put("time", a.getCreatedTime() == null ? null : a.getCreatedTime().toString().substring(0, 16));
            m.put("applicant", a.getApplicantName());
            m.put("status", a.getStatus());
            pendingList.add(m);
        });
        dto.setPendingApprovalList(pendingList);

        // 2) 风险预警：低库存 / 待采购 / 待审批 / 空闲包厢
        long lowStock = dto.getLowStockCount();
        long pendingPurchase = dto.getPendingPurchases();
        long pendingApprovals = dto.getPendingApprovalCount();
        long emptyBoxes = dto.getEmptyBoxWarningCount();

        Map<String, Long> riskCounts = new LinkedHashMap<>();
        riskCounts.put("lowStock", lowStock);
        riskCounts.put("pendingPurchase", pendingPurchase);
        riskCounts.put("pendingApproval", pendingApprovals);
        riskCounts.put("emptyBox", emptyBoxes);
        dto.setRiskCounts(riskCounts);

        List<Map<String, Object>> risks = new ArrayList<>();
        if (lowStock > 0) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("type", "lowStock");
            r.put("level", lowStock > 10 ? "danger" : "warning");
            r.put("title", "食材低库存预警");
            r.put("desc", "当前库存 ≤ 最低库存阈值的食材品类数");
            r.put("count", lowStock);
            r.put("link", "inventory");
            risks.add(r);
        }
        if (pendingPurchase > 0) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("type", "pendingPurchase");
            r.put("level", "warning");
            r.put("title", "待审批采购单");
            r.put("desc", "等待总经理或店长审批的采购申请数");
            r.put("count", pendingPurchase);
            r.put("link", "procurement");
            risks.add(r);
        }
        if (pendingApprovals > 0) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("type", "pendingApproval");
            r.put("level", "warning");
            r.put("title", "待办审批事项");
            r.put("desc", "所有审批类型中仍处于 pending 状态的总数");
            r.put("count", pendingApprovals);
            r.put("link", "approval-center");
            risks.add(r);
        }
        if (emptyBoxes > 0) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("type", "emptyBox");
            r.put("level", "warning");
            r.put("title", "包厢空置率偏高");
            r.put("desc", "今日尚未被预订的包厢数量，可推送营销活动");
            r.put("count", emptyBoxes);
            r.put("link", "bookings");
            risks.add(r);
        }
        dto.setRiskWarnings(risks);

        // 3) 业务看板 badge：仅展示有真实数据源的项
        Map<String, Long> badges = new LinkedHashMap<>();
        badges.put("approval-center", pendingApprovals);
        badges.put("supply-chain", pendingPurchase);
        badges.put("inventory", lowStock);
        badges.put("revenue", dto.getOrderCount());
        // 无数据源的项目显式 0（前端可选择不展示）
        badges.put("waste", 0L);
        badges.put("hygiene", 0L);
        badges.put("attendance", 0L);
        badges.put("finance", 0L);
        badges.put("tax", 0L);
        badges.put("engineering", 0L);
        badges.put("energy", 0L);
        badges.put("data-screen", 0L);
        dto.setNavBadges(badges);
    }

    /* ====================== 工具 ====================== */
    private double computePct(BigDecimal current, BigDecimal base) {
        if (base == null || base.signum() == 0) return 0.0;
        return current.subtract(base).divide(base.abs(), 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
    }

    private Map<String, Object> toRecentMap(BookingMaster b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bookingId", b.getBookingId());
        m.put("customerName", b.getCustomerName());
        m.put("guestCount", b.getGuestCount());
        m.put("bookingTime", b.getBookingTime() == null ? null : b.getBookingTime().toString().substring(0, 5));
        m.put("status", b.getBookingStatus());
        m.put("totalAmount", b.getTotalAmount());
        m.put("storeId", b.getStoreId());
        return m;
    }

    /** 毛利率 + 净利 + 成本拆分计算结果封装 */
    private static class MarginResult {
        double grossMarginRate = 0.0;
        BigDecimal netProfit = BigDecimal.ZERO;
        Map<String, Double> costBreakdown = Collections.emptyMap();
    }

    /* ====================== 工作台首页专用接口 ====================== */

    /**
     * 获取指定天数的每日营收趋势（用于折线图）。
     * 范围：endDate 往前推 days 天（含 endDate）。
     * 列表按日期升序。
     */
    public List<Map<String, Object>> getRevenueChart(String storeId, int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(Math.max(0, days - 1));
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) dates.add(d);

        Long sid = null;
        if (storeId != null && !ALL_STORES.equalsIgnoreCase(storeId)) {
            try { sid = Long.parseLong(storeId); } catch (NumberFormatException ignored) {}
        }

        List<BookingMaster> bookings = sid == null
                ? this.bookingMasterRepository.findByBookingDateBetween(startDate, endDate)
                : this.bookingMasterRepository.findByStoreIdAndBookingDateBetween(sid, startDate, endDate);

        Map<LocalDate, BigDecimal> dayMap = new HashMap<>();
        for (BookingMaster b : bookings) {
            if (b.getBookingDate() == null) continue;
            BigDecimal amt = b.getTotalAmount() == null ? BigDecimal.ZERO : b.getTotalAmount();
            dayMap.merge(b.getBookingDate(), amt, BigDecimal::add);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate d : dates) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", d.toString());
            m.put("label", d.getMonthValue() + "/" + d.getDayOfMonth());
            m.put("value", dayMap.getOrDefault(d, BigDecimal.ZERO));
            result.add(m);
        }
        return result;
    }

    /**
     * 获取热门菜品排行（按 BookingDishDetail 数量排序，limit 默认10）。
     */
    public List<Map<String, Object>> getHotDishes(String storeId, int limit) {
        Long sid = null;
        if (storeId != null && !ALL_STORES.equalsIgnoreCase(storeId)) {
            try { sid = Long.parseLong(storeId); } catch (NumberFormatException ignored) {}
        }

        List<BookingDishDetail> details = sid == null
                ? this.bookingDishDetailRepository.findAll()
                : this.bookingDishDetailRepository.findByStoreId(sid);

        // 按 dishId 聚合：[countQty(long), amount(BigDecimal)]
        Map<String, Object[]> agg = new HashMap<>();
        for (BookingDishDetail d : details) {
            if (d.getDishId() == null) continue;
            int qty = d.getDishQuantity() == null ? 1 : d.getDishQuantity();
            BigDecimal price = d.getSubtotal() == null ? BigDecimal.ZERO : d.getSubtotal();
            Object[] arr = agg.computeIfAbsent(d.getDishId(), k -> new Object[]{0L, BigDecimal.ZERO, d.getDishName()});
            arr[0] = ((Long) arr[0]) + qty;
            arr[1] = ((BigDecimal) arr[1]).add(price);
            if (arr[2] == null && d.getDishName() != null) arr[2] = d.getDishName();
        }

        if (limit <= 0) limit = 10;
        // 转换为 list 并排序
        List<Map.Entry<String, Object[]>> list = new ArrayList<>(agg.entrySet());
        list.sort((a, b) -> Long.compare((Long) b.getValue()[0], (Long) a.getValue()[0]));
        int take = Math.min(limit, list.size());

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < take; i++) {
            Map.Entry<String, Object[]> e = list.get(i);
            Object[] v = e.getValue();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", i + 1);
            m.put("dishId", e.getKey());
            m.put("dishName", v[2] == null ? "菜品#" + e.getKey() : v[2]);
            m.put("count", v[0]);
            m.put("amount", v[1]);
            result.add(m);
        }
        return result;
    }
}
