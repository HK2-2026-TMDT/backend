package vn.io.sanmaymac.modules.message.dto;

import java.time.Instant;

public record WorkshopMessageResponseRecord(
        Long id,
        Long threadId,
        Long senderId,
        String senderName,
        String senderRole,
        String senderAvatarUrl,
        String content,
        java.time.Instant createdAt) {
}