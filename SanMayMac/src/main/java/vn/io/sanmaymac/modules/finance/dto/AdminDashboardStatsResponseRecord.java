package vn.io.sanmaymac.modules.finance.dto;

import java.util.List;
import vn.io.sanmaymac.modules.order.dto.OrderStatsResponseRecord;

public record AdminDashboardStatsResponseRecord(
        OrderStatsResponseRecord trend,
        List<WorkshopRevenueShareRecord> revenueByWorkshop) {
}
