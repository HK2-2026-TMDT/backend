package vn.io.sanmaymac.modules.coupon.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import vn.io.sanmaymac.common.dto.ApiResponse;
import vn.io.sanmaymac.modules.coupon.dto.CouponResponseRecord;
import vn.io.sanmaymac.modules.coupon.dto.CouponUpsertRequest;
import vn.io.sanmaymac.modules.coupon.service.CouponService;

@RestController
@RequestMapping("/api/admin/coupons")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class CouponAdminController {
    private final CouponService couponService;

    public CouponAdminController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CouponResponseRecord>>> listCoupons() {
        return ResponseEntity.ok(ApiResponse.success("OK", couponService.listCoupons()));
    }

    @GetMapping("/{couponId}")
    public ResponseEntity<ApiResponse<CouponResponseRecord>> getCoupon(@PathVariable Long couponId) {
        return ResponseEntity.ok(ApiResponse.success("OK", couponService.getCoupon(couponId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CouponResponseRecord>> createCoupon(
            @Valid @RequestBody CouponUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Created", couponService.createCoupon(request)));
    }

    @PutMapping("/{couponId}")
    public ResponseEntity<ApiResponse<CouponResponseRecord>> updateCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Updated", couponService.updateCoupon(couponId, request)));
    }

    @DeleteMapping("/{couponId}")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Long couponId) {
        couponService.deleteCoupon(couponId);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}
