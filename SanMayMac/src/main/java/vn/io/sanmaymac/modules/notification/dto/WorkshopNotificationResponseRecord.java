package vn.io.sanmaymac.modules.notification.dto;

import java.time.Instant;

public record WorkshopNotificationResponseRecord(
        Long id,
        String type,
        String title,
        String body,
        String referenceUrl,
        Long referenceId,
        Instant readAt,
        Instant createdAt) {
}