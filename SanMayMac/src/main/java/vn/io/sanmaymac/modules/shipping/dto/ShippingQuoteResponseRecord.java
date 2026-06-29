package vn.io.sanmaymac.modules.shipping.dto;

import java.math.BigDecimal;

public record ShippingQuoteResponseRecord(
        BigDecimal fee,
        String serviceName,
        boolean available,
        String message) {
}
