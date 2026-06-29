package vn.io.sanmaymac.modules.order.dto;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutReadyMadeResponseRecord(
        String checkoutBatchId,
        BigDecimal grandTotal,
        List<OrderDetailResponseRecord> orders,
        int orderCount) {
}
