package vn.io.sanmaymac.modules.workshop.dto;

import vn.io.sanmaymac.modules.finance.dto.RevenueSummaryRecord;
import vn.io.sanmaymac.modules.order.dto.OrderStatsResponseRecord;

public record WorkshopStatsResponseRecord(
        OrderStatsResponseRecord orderStats,
        RevenueSummaryRecord revenue) {
}