package vn.io.sanmaymac.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddressRequest(
        @NotBlank String receiverName,
        @NotBlank String phone,
        @NotBlank String detailedAddress,
        @NotNull Integer provinceId,
        @NotNull Integer districtId,
        @NotBlank String wardCode,
        String provinceName,
        String districtName,
        String wardName,
        boolean isDefault) {
}
