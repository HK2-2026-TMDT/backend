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
        BigDecimal discountAmount,
        String customerNote,
        String checkoutBatchId,
        String trackingCode,
        String ghnOrderCode,
        String frontDesignUrl,
        String backDesignUrl,
        Long workshopId,
        String workshopName,
        Long customerId,
        String customerName,
        String customerPhone,
        String customerEmail,
        String receiverName,
        String receiverPhone,
        String shippingAddress,
        Long addressId,
        List<OrderDetailItemResponseRecord> items,
        Instant createdAt) {
}
