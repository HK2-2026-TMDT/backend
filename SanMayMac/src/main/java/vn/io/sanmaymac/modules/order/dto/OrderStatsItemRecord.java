package vn.io.sanmaymac.modules.order.dto;

import java.math.BigDecimal;

public record OrderStatsItemRecord(String period, long totalOrders, BigDecimal totalRevenue) {
}
