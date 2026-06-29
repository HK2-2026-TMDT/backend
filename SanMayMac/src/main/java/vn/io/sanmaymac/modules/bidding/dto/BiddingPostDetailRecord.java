package vn.io.sanmaymac.modules.bidding.dto;

import java.time.Instant;
import java.util.List;

public record BiddingPostDetailRecord(
        Long id,
        String title,
        String description,
        String aiImageUrl,
        String frontDesignUrl,
        String backDesignUrl,
        String status,
        Long customerId,
        String customerName,
        long quoteCount,
        List<BiddingAttachmentResponseRecord> attachments,
        Instant createdAt) {
}
