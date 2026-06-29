package vn.io.sanmaymac.modules.bidding.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePostStatusRequest(@NotBlank String status) {
}
