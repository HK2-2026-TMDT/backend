package vn.io.sanmaymac.modules.finance.dto;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutBatchSummaryResponseRecord(
        String checkoutBatchId,
        BigDecimal grandTotal,
        BigDecimal remainingTotal,
        int orderCount,
        List<CheckoutBatchOrderItemRecord> orders) {
}
