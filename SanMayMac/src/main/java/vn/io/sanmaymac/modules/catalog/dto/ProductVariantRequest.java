package vn.io.sanmaymac.modules.catalog.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductVariantRequest(
        String skuCode,
        String color,
        String size,
        BigDecimal price,
        @NotNull @Min(0) Integer stockQuantity) {
}
