package vn.io.sanmaymac.modules.order.dto;

import jakarta.validation.constraints.NotNull;

public record CheckoutReadyMadeRequest(
        @NotNull Long addressId,
        String couponCode,
        String customerNote) {
}
