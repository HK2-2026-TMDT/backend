package vn.io.sanmaymac.modules.shipping.dto;

import jakarta.validation.constraints.NotNull;

public record ShippingQuoteRequest(
        @NotNull Long addressId) {
}
