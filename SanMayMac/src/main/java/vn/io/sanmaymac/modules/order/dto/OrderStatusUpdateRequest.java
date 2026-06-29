package vn.io.sanmaymac.modules.order.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderStatusUpdateRequest(@NotBlank String status) {
}
