package vn.io.sanmaymac.modules.order.dto;

import java.time.Instant;

public record OrderTimelineItemResponseRecord(
        String code,
        String label,
        boolean completed,
        Instant updatedAt,
        String note) {
}