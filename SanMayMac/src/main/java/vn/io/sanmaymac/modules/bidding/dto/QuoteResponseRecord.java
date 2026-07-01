package vn.io.sanmaymac.modules.bidding.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record QuoteResponseRecord(
        Long id,
        Long postId,
        String postTitle,
        String customerName,
        Long workshopId,
        String workshopName,
        String workshopAvatar,
        BigDecimal offeredPrice,
        Integer estimateDays,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
