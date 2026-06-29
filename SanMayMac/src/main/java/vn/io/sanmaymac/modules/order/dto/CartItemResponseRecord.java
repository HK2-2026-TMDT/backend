package vn.io.sanmaymac.modules.order.dto;

import java.math.BigDecimal;

public record CartItemResponseRecord(
        Long id,
        Long variantId,
        Long productId,
        String productName,
        String color,
        String size,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice) {
}
