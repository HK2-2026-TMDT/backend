package vn.io.sanmaymac.modules.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProductDetailCacheRecord(
        Long id,
        String name,
        BigDecimal basePrice,
        String description,
        Long categoryId,
        Long workshopId,
        List<ProductImageResponseRecord> images,
        List<ProductVariantResponseRecord> variants,
        Instant createdAt) {
}