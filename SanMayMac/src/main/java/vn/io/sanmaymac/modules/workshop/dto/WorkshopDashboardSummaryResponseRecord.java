package vn.io.sanmaymac.modules.workshop.dto;

import java.math.BigDecimal;

public record WorkshopDashboardSummaryResponseRecord(
        BigDecimal walletBalance,
        BigDecimal pendingBalance,
        Integer aiTokenBalance,
        BigDecimal revenueTotal,
        Long totalOrders,
        Long pendingOrders,
        Long pendingPayouts,
        double ratingAvg,
        long reviewCount,
        long unreadNotifications) {
}