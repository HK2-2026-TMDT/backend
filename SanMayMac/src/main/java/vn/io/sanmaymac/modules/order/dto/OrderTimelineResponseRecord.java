package vn.io.sanmaymac.modules.order.dto;

import java.util.List;

public record OrderTimelineResponseRecord(
        Long orderId,
        String currentStatus,
        List<OrderTimelineItemResponseRecord> items) {
}