/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.youjian.banquet.dto.ReportDTO
 */
package com.youjian.banquet.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ReportDTO {
    private String period;
    private String startDate;
    private String endDate;
    private BigDecimal totalRevenue;
    private long totalBookings;
    private long totalGuests;
    private BigDecimal averagePerBooking;
    private Map<String, BigDecimal> revenueByOccasion;
    private Map<String, Long> bookingsByStatus;
    private List<Map<String, Object>> dailyTrend;
    private List<Map<String, Object>> topDishes;

    public String getPeriod() {
        return this.period;
    }

    public String getStartDate() {
        return this.startDate;
    }

    public String getEndDate() {
        return this.endDate;
    }

    public BigDecimal getTotalRevenue() {
        return this.totalRevenue;
    }

    public long getTotalBookings() {
        return this.totalBookings;
    }

    public long getTotalGuests() {
        return this.totalGuests;
    }

    public BigDecimal getAveragePerBooking() {
        return this.averagePerBooking;
    }

    public Map<String, BigDecimal> getRevenueByOccasion() {
        return this.revenueByOccasion;
    }

    public Map<String, Long> getBookingsByStatus() {
        return this.bookingsByStatus;
    }

    public List<Map<String, Object>> getDailyTrend() {
        return this.dailyTrend;
    }

    public List<Map<String, Object>> getTopDishes() {
        return this.topDishes;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public void setTotalBookings(long totalBookings) {
        this.totalBookings = totalBookings;
    }

    public void setTotalGuests(long totalGuests) {
        this.totalGuests = totalGuests;
    }

    public void setAveragePerBooking(BigDecimal averagePerBooking) {
        this.averagePerBooking = averagePerBooking;
    }

    public void setRevenueByOccasion(Map<String, BigDecimal> revenueByOccasion) {
        this.revenueByOccasion = revenueByOccasion;
    }

    public void setBookingsByStatus(Map<String, Long> bookingsByStatus) {
        this.bookingsByStatus = bookingsByStatus;
    }

    public void setDailyTrend(List<Map<String, Object>> dailyTrend) {
        this.dailyTrend = dailyTrend;
    }

    public void setTopDishes(List<Map<String, Object>> topDishes) {
        this.topDishes = topDishes;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ReportDTO)) {
            return false;
        }
        ReportDTO other = (ReportDTO)o;
        if (!other.canEqual((Object)this)) {
            return false;
        }
        if (this.getTotalBookings() != other.getTotalBookings()) {
            return false;
        }
        if (this.getTotalGuests() != other.getTotalGuests()) {
            return false;
        }
        String this$period = this.getPeriod();
        String other$period = other.getPeriod();
        if (this$period == null ? other$period != null : !this$period.equals(other$period)) {
            return false;
        }
        String this$startDate = this.getStartDate();
        String other$startDate = other.getStartDate();
        if (this$startDate == null ? other$startDate != null : !this$startDate.equals(other$startDate)) {
            return false;
        }
        String this$endDate = this.getEndDate();
        String other$endDate = other.getEndDate();
        if (this$endDate == null ? other$endDate != null : !this$endDate.equals(other$endDate)) {
            return false;
        }
        BigDecimal this$totalRevenue = this.getTotalRevenue();
        BigDecimal other$totalRevenue = other.getTotalRevenue();
        if (this$totalRevenue == null ? other$totalRevenue != null : !((Object)this$totalRevenue).equals(other$totalRevenue)) {
            return false;
        }
        BigDecimal this$averagePerBooking = this.getAveragePerBooking();
        BigDecimal other$averagePerBooking = other.getAveragePerBooking();
        if (this$averagePerBooking == null ? other$averagePerBooking != null : !((Object)this$averagePerBooking).equals(other$averagePerBooking)) {
            return false;
        }
        Map this$revenueByOccasion = this.getRevenueByOccasion();
        Map other$revenueByOccasion = other.getRevenueByOccasion();
        if (this$revenueByOccasion == null ? other$revenueByOccasion != null : !((Object)this$revenueByOccasion).equals(other$revenueByOccasion)) {
            return false;
        }
        Map this$bookingsByStatus = this.getBookingsByStatus();
        Map other$bookingsByStatus = other.getBookingsByStatus();
        if (this$bookingsByStatus == null ? other$bookingsByStatus != null : !((Object)this$bookingsByStatus).equals(other$bookingsByStatus)) {
            return false;
        }
        List this$dailyTrend = this.getDailyTrend();
        List other$dailyTrend = other.getDailyTrend();
        if (this$dailyTrend == null ? other$dailyTrend != null : !((Object)this$dailyTrend).equals(other$dailyTrend)) {
            return false;
        }
        List this$topDishes = this.getTopDishes();
        List other$topDishes = other.getTopDishes();
        return !(this$topDishes == null ? other$topDishes != null : !((Object)this$topDishes).equals(other$topDishes));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ReportDTO;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        long $totalBookings = this.getTotalBookings();
        result = result * 59 + (int)($totalBookings >>> 32 ^ $totalBookings);
        long $totalGuests = this.getTotalGuests();
        result = result * 59 + (int)($totalGuests >>> 32 ^ $totalGuests);
        String $period = this.getPeriod();
        result = result * 59 + ($period == null ? 43 : $period.hashCode());
        String $startDate = this.getStartDate();
        result = result * 59 + ($startDate == null ? 43 : $startDate.hashCode());
        String $endDate = this.getEndDate();
        result = result * 59 + ($endDate == null ? 43 : $endDate.hashCode());
        BigDecimal $totalRevenue = this.getTotalRevenue();
        result = result * 59 + ($totalRevenue == null ? 43 : ((Object)$totalRevenue).hashCode());
        BigDecimal $averagePerBooking = this.getAveragePerBooking();
        result = result * 59 + ($averagePerBooking == null ? 43 : ((Object)$averagePerBooking).hashCode());
        Map $revenueByOccasion = this.getRevenueByOccasion();
        result = result * 59 + ($revenueByOccasion == null ? 43 : ((Object)$revenueByOccasion).hashCode());
        Map $bookingsByStatus = this.getBookingsByStatus();
        result = result * 59 + ($bookingsByStatus == null ? 43 : ((Object)$bookingsByStatus).hashCode());
        List $dailyTrend = this.getDailyTrend();
        result = result * 59 + ($dailyTrend == null ? 43 : ((Object)$dailyTrend).hashCode());
        List $topDishes = this.getTopDishes();
        result = result * 59 + ($topDishes == null ? 43 : ((Object)$topDishes).hashCode());
        return result;
    }

    public String toString() {
        return "ReportDTO(period=" + this.getPeriod() + ", startDate=" + this.getStartDate() + ", endDate=" + this.getEndDate() + ", totalRevenue=" + String.valueOf(this.getTotalRevenue()) + ", totalBookings=" + this.getTotalBookings() + ", totalGuests=" + this.getTotalGuests() + ", averagePerBooking=" + String.valueOf(this.getAveragePerBooking()) + ", revenueByOccasion=" + String.valueOf(this.getRevenueByOccasion()) + ", bookingsByStatus=" + String.valueOf(this.getBookingsByStatus()) + ", dailyTrend=" + String.valueOf(this.getDailyTrend()) + ", topDishes=" + String.valueOf(this.getTopDishes()) + ")";
    }

    public ReportDTO() {
    }

    public ReportDTO(String period, String startDate, String endDate, BigDecimal totalRevenue, long totalBookings, long totalGuests, BigDecimal averagePerBooking, Map<String, BigDecimal> revenueByOccasion, Map<String, Long> bookingsByStatus, List<Map<String, Object>> dailyTrend, List<Map<String, Object>> topDishes) {
        this.period = period;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalRevenue = totalRevenue;
        this.totalBookings = totalBookings;
        this.totalGuests = totalGuests;
        this.averagePerBooking = averagePerBooking;
        this.revenueByOccasion = revenueByOccasion;
        this.bookingsByStatus = bookingsByStatus;
        this.dailyTrend = dailyTrend;
        this.topDishes = topDishes;
    }
}

