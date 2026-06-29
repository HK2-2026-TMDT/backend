package vn.io.sanmaymac.modules.message.dto;

import java.time.Instant;

public record WorkshopMessageThreadResponseRecord(
        Long id,
        Long orderId,
        Long customerId,
        Long workshopId,
        String lastMessage,
        Instant lastMessageAt) {
}