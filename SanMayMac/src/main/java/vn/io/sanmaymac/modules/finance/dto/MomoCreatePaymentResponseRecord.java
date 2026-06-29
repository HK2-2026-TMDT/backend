package vn.io.sanmaymac.modules.finance.dto;

import java.math.BigDecimal;

public record MomoCreatePaymentResponseRecord(
        String payUrl,
        String requestId,
        String momoOrderId,
        BigDecimal amount,
        String message) {
}
