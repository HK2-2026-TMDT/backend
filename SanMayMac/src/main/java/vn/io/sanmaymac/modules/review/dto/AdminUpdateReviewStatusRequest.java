package vn.io.sanmaymac.modules.review.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUpdateReviewStatusRequest(@NotBlank String status) {
}
