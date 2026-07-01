package vn.io.sanmaymac.modules.message.dto;

import java.time.Instant;

public record WorkshopMessageThreadResponseRecord(
        Long id,
        Long orderId,
        Long customerId,
        String customerName,
        Long workshopId,
        String workshopName,
        String participantName,
        String participantAvatarUrl,
        String subject,
        String lastMessage,
        Instant lastMessageAt) {
}