package vn.io.sanmaymac.modules.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartAddRequest(
        @NotNull Long variantId,
        @NotNull @Min(1) Integer quantity) {
}
