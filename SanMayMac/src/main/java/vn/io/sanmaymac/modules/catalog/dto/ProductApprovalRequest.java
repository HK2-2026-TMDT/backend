package vn.io.sanmaymac.modules.catalog.dto;

import jakarta.validation.constraints.NotNull;

public record ProductApprovalRequest(@NotNull boolean approved, String adminNote) {
}
