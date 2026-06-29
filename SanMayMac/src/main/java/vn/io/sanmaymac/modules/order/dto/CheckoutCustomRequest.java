package vn.io.sanmaymac.modules.order.dto;

import jakarta.validation.constraints.NotNull;

public record CheckoutCustomRequest(
        @NotNull Long quoteId,
        @NotNull Long addressId) {
}
