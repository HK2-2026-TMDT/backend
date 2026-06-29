package vn.io.sanmaymac.modules.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PayoutResponseRecord(
        Long id,
        Long workshopId,
        BigDecimal amount,
        String status,
        String adminNote,
        Instant createdAt,
        Instant approvedAt) {
}
