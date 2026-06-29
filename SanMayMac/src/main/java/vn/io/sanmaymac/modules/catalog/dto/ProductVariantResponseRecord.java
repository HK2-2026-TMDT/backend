package vn.io.sanmaymac.modules.catalog.dto;

import java.math.BigDecimal;

public record ProductVariantResponseRecord(
        Long id,
        String skuCode,
        String color,
        String size,
        BigDecimal price,
        Integer stockQuantity) {
}
