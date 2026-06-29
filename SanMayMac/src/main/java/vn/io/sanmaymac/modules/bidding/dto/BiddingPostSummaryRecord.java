package vn.io.sanmaymac.modules.bidding.dto;

import java.time.Instant;

public record BiddingPostSummaryRecord(
        Long id,
        String title,
        String status,
        long quoteCount,
        Instant createdAt) {
}
