package vn.io.sanmaymac.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserProfileRequest(
        @NotBlank String fullName,
        @NotBlank String phoneNumber,
        String avatarUrl) {
}
