package vn.io.sanmaymac.modules.coupon.dto;

import java.math.BigDecimal;
import java.time.Instant;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CouponUpsertRequest(
        @NotBlank String code,
        @NotBlank String discountType,
        @NotNull @DecimalMin("0.0") BigDecimal discountValue,
        BigDecimal maxDiscountAmount,
        BigDecimal minOrderAmount,
        Integer usageLimit,
        Instant startsAt,
        Instant expiresAt,
        @NotNull Boolean isActive) {
}
