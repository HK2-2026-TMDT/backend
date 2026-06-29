package vn.io.sanmaymac.modules.catalog.dto;

import java.math.BigDecimal;

public record ProductSummaryCacheRecord(
        Long id,
        String name,
        BigDecimal basePrice,
        String thumbnailUrl,
        Long workshopId) {
}