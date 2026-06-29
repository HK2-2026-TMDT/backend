package vn.io.sanmaymac.modules.user.dto;

import jakarta.validation.constraints.NotNull;

public record VettingRequest(@NotNull boolean approved, String adminNote) {
}
