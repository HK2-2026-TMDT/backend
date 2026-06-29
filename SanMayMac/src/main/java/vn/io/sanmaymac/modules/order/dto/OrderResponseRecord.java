package vn.io.sanmaymac.modules.order.dto;

import java.math.BigDecimal;

public record OrderResponseRecord(Long id, BigDecimal totalAmount, String status) {
}
