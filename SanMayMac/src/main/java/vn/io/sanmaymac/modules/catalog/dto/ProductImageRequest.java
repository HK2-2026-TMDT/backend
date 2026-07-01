package vn.io.sanmaymac.modules.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductImageRequest(
        @NotBlank String imageUrl,
        Boolean isThumbnail,
        Integer sortOrder) {
}
