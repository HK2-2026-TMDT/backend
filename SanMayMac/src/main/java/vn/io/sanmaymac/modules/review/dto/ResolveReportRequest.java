package vn.io.sanmaymac.modules.review.dto;

import jakarta.validation.constraints.NotBlank;

public record ResolveReportRequest(@NotBlank String resolution, String adminNote) {
}
