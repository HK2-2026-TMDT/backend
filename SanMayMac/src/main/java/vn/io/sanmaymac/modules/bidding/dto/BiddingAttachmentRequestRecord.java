package vn.io.sanmaymac.modules.bidding.dto;

import jakarta.validation.constraints.NotBlank;

public record BiddingAttachmentRequestRecord(
        @NotBlank String fileUrl,
        String fileType) {
}
