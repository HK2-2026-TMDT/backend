package vn.io.sanmaymac.modules.review.dto;

import java.time.Instant;

public record ReviewReportResponseRecord(
        Long id,
        Long reviewId,
        Long workshopId,
        String reason,
        String status,
        String resolution,
        String adminNote,
        Instant resolvedAt,
        Instant createdAt) {
}
