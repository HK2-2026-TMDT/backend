package vn.io.sanmaymac.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record PortfolioItemRequest(
        @NotBlank String title,
        @NotBlank String imageUrl,
        String description) {
}
