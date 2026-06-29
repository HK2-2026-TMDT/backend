package vn.io.sanmaymac.modules.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import vn.io.sanmaymac.common.enums.ProductApprovalStatus;

public record ProductDetailResponseRecord(
        Long id,
        String name,
        BigDecimal basePrice,
        String description,
        Long categoryId,
        Long workshopId,
        List<ProductImageResponseRecord> images,
        List<ProductVariantResponseRecord> variants,
        Boolean isFavorite,
        Instant createdAt,
        Boolean isVisible,
        ProductApprovalStatus approvalStatus,
        String adminNote) {
}
