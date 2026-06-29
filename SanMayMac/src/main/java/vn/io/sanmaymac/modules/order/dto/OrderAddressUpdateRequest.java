package vn.io.sanmaymac.modules.order.dto;

import jakarta.validation.constraints.NotNull;

public record OrderAddressUpdateRequest(@NotNull Long addressId) {
}
