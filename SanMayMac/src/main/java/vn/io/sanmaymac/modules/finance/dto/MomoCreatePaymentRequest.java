package vn.io.sanmaymac.modules.finance.dto;

import java.math.BigDecimal;

public record MomoCreatePaymentRequest(
        String phase,
        BigDecimal amount,
        String orderInfo) {
}
