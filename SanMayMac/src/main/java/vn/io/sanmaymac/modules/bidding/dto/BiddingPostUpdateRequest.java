package vn.io.sanmaymac.modules.bidding.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;

public record BiddingPostUpdateRequest(
        @NotBlank String title,
        @NotBlank String description,
        String aiImageUrl,
        String frontDesignUrl,
        String backDesignUrl,
        List<BiddingAttachmentRequestRecord> attachments) {
}
