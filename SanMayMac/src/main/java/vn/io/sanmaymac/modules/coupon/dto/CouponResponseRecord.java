package vn.io.sanmaymac.modules.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CouponResponseRecord(
        Long id,
        String code,
        String discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderAmount,
        Integer usageLimit,
        Integer usedCount,
        Instant startsAt,
        Instant expiresAt,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt) {
}
