package vn.io.sanmaymac.modules.complaint.dto;

import java.time.Instant;
import java.util.List;
import vn.io.sanmaymac.common.enums.ComplaintStatus;

public record ComplaintResponseRecord(
        Long id,
        Long orderId,
        Long customerId,
        String customerName,
        Long workshopId,
        String workshopName,
        String reason,
        ComplaintStatus status,
        List<String> imageUrls,
        String adminNote,
        Long disputeId,
        Instant createdAt,
        Instant updatedAt) {
}
