package vn.io.sanmaymac.modules.coupon.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.sanmaymac.common.enums.CouponDiscountType;
import vn.io.sanmaymac.modules.coupon.dto.CouponResponseRecord;
import vn.io.sanmaymac.modules.coupon.dto.CouponUpsertRequest;
import vn.io.sanmaymac.modules.coupon.entity.CouponEntity;
import vn.io.sanmaymac.modules.coupon.repository.CouponRepository;

@Service
@Transactional
public class CouponService {
    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public java.util.List<CouponResponseRecord> listCoupons() {
        return couponRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(CouponEntity::getId))
                .map(this::toResponse)
                .toList();
    }

    public CouponResponseRecord getCoupon(Long couponId) {
        CouponEntity coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        return toResponse(coupon);
    }

    public CouponResponseRecord createCoupon(CouponUpsertRequest request) {
        if (couponRepository.existsByCodeIgnoreCase(request.code())) {
            throw new IllegalStateException("Coupon code already exists");
        }
        CouponEntity entity = CouponEntity.builder()
                .code(normalizeCode(request.code()))
                .usedCount(0)
                .build();
        mapUpsert(entity, request);
        return toResponse(couponRepository.save(entity));
    }

    public CouponResponseRecord updateCoupon(Long couponId, CouponUpsertRequest request) {
        CouponEntity entity = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        String normalizedCode = normalizeCode(request.code());
        couponRepository.findByCodeIgnoreCase(normalizedCode)
                .filter(existing -> !existing.getId().equals(couponId))
                .ifPresent(existing -> {
                    throw new IllegalStateException("Coupon code already exists");
                });
        mapUpsert(entity, request);
        return toResponse(couponRepository.save(entity));
    }

    public void deleteCoupon(Long couponId) {
        CouponEntity entity = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        couponRepository.delete(entity);
    }

    public BigDecimal calculateDiscount(String code, BigDecimal orderAmount) {
        if (code == null || code.isBlank()) {
            return BigDecimal.ZERO;
        }
        CouponEntity coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        validateAvailability(coupon, orderAmount);
        if (coupon.getDiscountType() == CouponDiscountType.FIXED) {
            return clampDiscount(orderAmount, normalize(coupon.getDiscountValue()));
        }
        BigDecimal discount = normalize(orderAmount)
                .multiply(normalize(coupon.getDiscountValue()))
                .divide(BigDecimal.valueOf(100));
        if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
            discount = coupon.getMaxDiscountAmount();
        }
        return clampDiscount(orderAmount, discount);
    }

    public void markCouponUsed(String code) {
        if (code == null || code.isBlank()) {
            return;
        }
        CouponEntity coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));
        int usedCount = safeInt(coupon.getUsedCount());
        if (coupon.getUsageLimit() != null && usedCount >= coupon.getUsageLimit()) {
            throw new IllegalStateException("Coupon usage limit exceeded");
        }
        coupon.setUsedCount(usedCount + 1);
        couponRepository.save(coupon);
    }

    private void mapUpsert(CouponEntity entity, CouponUpsertRequest request) {
        CouponDiscountType discountType = CouponDiscountType.valueOf(request.discountType().toUpperCase(Locale.ROOT));
        if (request.expiresAt() != null && request.startsAt() != null && request.expiresAt().isBefore(request.startsAt())) {
            throw new IllegalArgumentException("expiresAt must be after startsAt");
        }
        entity.setCode(normalizeCode(request.code()));
        entity.setDiscountType(discountType);
        entity.setDiscountValue(normalize(request.discountValue()));
        entity.setMaxDiscountAmount(request.maxDiscountAmount());
        entity.setMinOrderAmount(request.minOrderAmount());
        entity.setUsageLimit(request.usageLimit());
        entity.setStartsAt(request.startsAt());
        entity.setExpiresAt(request.expiresAt());
        entity.setIsActive(Boolean.TRUE.equals(request.isActive()));
        if (entity.getUsedCount() == null) {
            entity.setUsedCount(0);
        }
    }

    private void validateAvailability(CouponEntity coupon, BigDecimal orderAmount) {
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new IllegalStateException("Coupon is inactive");
        }
        Instant now = Instant.now();
        if (coupon.getStartsAt() != null && now.isBefore(coupon.getStartsAt())) {
            throw new IllegalStateException("Coupon is not started");
        }
        if (coupon.getExpiresAt() != null && now.isAfter(coupon.getExpiresAt())) {
            throw new IllegalStateException("Coupon is expired");
        }
        if (coupon.getMinOrderAmount() != null && normalize(orderAmount).compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new IllegalStateException("Order amount is below minimum for coupon");
        }
        if (coupon.getUsageLimit() != null && safeInt(coupon.getUsedCount()) >= coupon.getUsageLimit()) {
            throw new IllegalStateException("Coupon usage limit reached");
        }
    }

    private CouponResponseRecord toResponse(CouponEntity coupon) {
        return new CouponResponseRecord(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDiscountType() != null ? coupon.getDiscountType().name() : null,
                coupon.getDiscountValue(),
                coupon.getMaxDiscountAmount(),
                coupon.getMinOrderAmount(),
                coupon.getUsageLimit(),
                coupon.getUsedCount(),
                coupon.getStartsAt(),
                coupon.getExpiresAt(),
                coupon.getIsActive(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt());
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private BigDecimal clampDiscount(BigDecimal total, BigDecimal discount) {
        BigDecimal normalizedTotal = normalize(total);
        BigDecimal normalizedDiscount = normalize(discount);
        if (normalizedDiscount.compareTo(normalizedTotal) > 0) {
            return normalizedTotal;
        }
        return normalizedDiscount;
    }
}
