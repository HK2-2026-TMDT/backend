package vn.io.sanmaymac.modules.finance.dto;

import java.math.BigDecimal;

public record CheckoutBatchOrderItemRecord(
        Long orderId,
        String workshopName,
        BigDecimal totalAmount,
        BigDecimal remainingAmount,
        String paymentStatus) {
}
