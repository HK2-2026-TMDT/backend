package vn.io.sanmaymac.modules.review.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewReportRequest(@NotBlank String reason) {
}
