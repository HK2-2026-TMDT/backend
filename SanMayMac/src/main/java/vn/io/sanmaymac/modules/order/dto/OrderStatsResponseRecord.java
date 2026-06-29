package vn.io.sanmaymac.modules.order.dto;

import java.util.List;

public record OrderStatsResponseRecord(String groupBy, List<OrderStatsItemRecord> items) {
}
