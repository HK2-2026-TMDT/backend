package vn.io.sanmaymac.modules.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import vn.io.sanmaymac.common.enums.ProductApprovalStatus;

public record AdminProductReviewRecord(
        Long id,
        String name,
        String description,
        BigDecimal basePrice,
        String thumbnailUrl,
        Long categoryId,
        String categoryName,
        Long workshopId,
        String workshopName,
        ProductApprovalStatus approvalStatus,
        Boolean isVisible,
        String adminNote,
        Instant createdAt) {
}
