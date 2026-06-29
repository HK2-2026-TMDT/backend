package vn.io.sanmaymac.modules.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponseRecord(List<CartItemResponseRecord> items, BigDecimal subTotal) {
}
