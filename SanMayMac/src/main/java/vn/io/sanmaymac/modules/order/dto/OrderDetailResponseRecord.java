package vn.io.sanmaymac.modules.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetailResponseRecord(
        Long id,
        String orderType,
        String status,
        BigDecimal totalAmount,
        BigDecimal shippingFee,
        String customerNote,
        String checkoutBatchId,
        String trackingCode,
        String ghnOrderCode,
        String frontDesignUrl,
        String backDesignUrl,
        Long workshopId,
        String workshopName,
        Long addressId,
        List<OrderDetailItemResponseRecord> items,
        Instant createdAt) {
}
