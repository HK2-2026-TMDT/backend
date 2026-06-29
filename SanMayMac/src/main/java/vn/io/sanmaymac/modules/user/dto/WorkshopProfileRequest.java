package vn.io.sanmaymac.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkshopProfileRequest(
        @NotBlank String shopName,
        @NotBlank String workshopAddress,
        Integer productionCapacity,
        String description) {
}
