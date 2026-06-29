package vn.io.sanmaymac.modules.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import vn.io.sanmaymac.common.enums.ProductApprovalStatus;

public record ProductSummaryResponseRecord(
        Long id,
        String name,
        BigDecimal basePrice,
        String thumbnailUrl,
        Long workshopId,
        Boolean isFavorite,
        Boolean isVisible,
        ProductApprovalStatus approvalStatus,
        String adminNote,
        String categoryName,
        String description,
        Instant createdAt) {
}
