package vn.io.sanmaymac.modules.complaint.dto;

import java.time.Instant;
import java.util.List;
import vn.io.sanmaymac.common.enums.ComplaintStatus;
import vn.io.sanmaymac.common.enums.DisputeStatus;

public record DisputeResponseRecord(
        Long id,
        Long complaintId,
        Long orderId,
        Long customerId,
        String customerName,
        Long workshopId,
        String workshopName,
        String reason,
        List<String> imageUrls,
        ComplaintStatus complaintStatus,
        DisputeStatus status,
        String adminRequestInfo,
        String customerSupplement,
        String ruling,
        Boolean refundProcessed,
        Boolean escrowReleased,
        Boolean violationRecorded,
        String violationNote,
        Instant resolvedAt,
        Instant createdAt,
        Instant updatedAt) {
}
