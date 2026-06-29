package vn.io.sanmaymac.modules.catalog.dto;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductUpdateRequest(
        @NotBlank String name,
        @NotNull Long categoryId,
        BigDecimal basePrice,
        String description,
        List<ProductVariantRequest> variants,
        List<ProductImageRequest> images) {
}
