package vn.io.sanmaymac.modules.review.dto;

import java.math.BigDecimal;

public record UnreviewedOrderResponseRecord(Long orderId, Long workshopId, BigDecimal totalAmount) {
}
