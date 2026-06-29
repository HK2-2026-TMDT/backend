package vn.io.sanmaymac.modules.finance.dto;

import java.math.BigDecimal;

public record PaymentRequest(
        BigDecimal amount,
        String paymentMethod,
        String transactionCode) {
}
