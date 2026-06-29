package vn.io.sanmaymac.modules.catalog.dto;

import jakarta.validation.constraints.NotNull;

public record ProductVisibilityUpdateRequest(@NotNull Boolean isVisible) {
}
