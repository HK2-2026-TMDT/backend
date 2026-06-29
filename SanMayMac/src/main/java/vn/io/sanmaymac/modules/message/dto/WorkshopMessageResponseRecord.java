package vn.io.sanmaymac.modules.message.dto;

import java.time.Instant;

public record WorkshopMessageResponseRecord(
        Long id,
        Long senderId,
        String content,
        Instant createdAt) {
}