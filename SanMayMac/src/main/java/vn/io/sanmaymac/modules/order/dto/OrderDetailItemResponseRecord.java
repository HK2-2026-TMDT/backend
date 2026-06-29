package vn.io.sanmaymac.modules.order.dto;

import java.math.BigDecimal;

public record OrderDetailItemResponseRecord(
        Long id,
        Long variantId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice) {
}
