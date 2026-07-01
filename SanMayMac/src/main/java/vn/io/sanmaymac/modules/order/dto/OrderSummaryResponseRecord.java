package vn.io.sanmaymac.modules.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryResponseRecord(
        Long id,
        String orderType,
        String status,
        BigDecimal totalAmount,
        String checkoutBatchId,
        String trackingCode,
        String customerName,
        Instant createdAt) {
}
