package vn.io.sanmaymac.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record WorkshopKycRequest(
        @NotBlank String taxCode,
        @NotBlank String licenseUrl) {
}
