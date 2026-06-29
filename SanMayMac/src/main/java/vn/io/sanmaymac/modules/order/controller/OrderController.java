package vn.io.sanmaymac.modules.order.controller;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import vn.io.sanmaymac.common.dto.ApiResponse;
import vn.io.sanmaymac.modules.order.dto.CartAddRequest;
import vn.io.sanmaymac.modules.order.dto.CartResponseRecord;
import vn.io.sanmaymac.modules.order.dto.CartUpdateRequest;
import vn.io.sanmaymac.modules.order.dto.CheckoutCustomRequest;
import vn.io.sanmaymac.modules.order.dto.CheckoutReadyMadeRequest;
import vn.io.sanmaymac.modules.order.dto.CheckoutReadyMadeResponseRecord;
import vn.io.sanmaymac.modules.order.dto.OrderDetailResponseRecord;
import vn.io.sanmaymac.modules.order.dto.OrderAddressUpdateRequest;
import vn.io.sanmaymac.modules.order.dto.OrderStatsResponseRecord;
import vn.io.sanmaymac.modules.order.dto.OrderStatusUpdateRequest;
import vn.io.sanmaymac.modules.order.dto.OrderTimelineResponseRecord;
import vn.io.sanmaymac.modules.order.dto.OrderSummaryResponseRecord;
import vn.io.sanmaymac.modules.order.dto.TrackingCodeRequest;
import vn.io.sanmaymac.modules.order.service.OrderService;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {
	private final OrderService orderService;

	public OrderController(OrderService orderService) {
		this.orderService = orderService;
	}

	@GetMapping("/cart")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<CartResponseRecord>> getMyCart() {
		return ResponseEntity.ok(ApiResponse.success("OK", orderService.getMyCart()));
	}

	@PostMapping("/cart")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<CartResponseRecord>> addToCart(
			@Valid @RequestBody CartAddRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Added", orderService.addToCart(request)));
	}

	@PutMapping("/cart/items/{itemId}")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<CartResponseRecord>> updateCartItem(
			@PathVariable Long itemId,
			@Valid @RequestBody CartUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", orderService.updateCartItem(itemId, request)));
	}

	@DeleteMapping("/cart/items/{itemId}")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<CartResponseRecord>> removeCartItem(@PathVariable Long itemId) {
		return ResponseEntity.ok(ApiResponse.success("Removed", orderService.removeCartItem(itemId)));
	}

	@DeleteMapping("/cart")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<Void>> clearCart() {
		orderService.clearCart();
		return ResponseEntity.ok(ApiResponse.success("Cleared", null));
	}

	@PostMapping("/checkout/ready-made")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<CheckoutReadyMadeResponseRecord>> checkoutReadyMade(
			@Valid @RequestBody CheckoutReadyMadeRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Created", orderService.checkoutReadyMade(request)));
	}

	@PostMapping("/checkout/custom")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<OrderDetailResponseRecord>> checkoutCustom(
			@Valid @RequestBody CheckoutCustomRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Created", orderService.checkoutCustom(request)));
	}

	@GetMapping("/me")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<Page<OrderSummaryResponseRecord>>> getMyOrders(
			@RequestParam(required = false) String status,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("OK", orderService.getMyOrders(status, pageable)));
	}

	@GetMapping("/me/{orderId}")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<OrderDetailResponseRecord>> getMyOrderDetail(
			@PathVariable Long orderId) {
		return ResponseEntity.ok(ApiResponse.success("OK", orderService.getMyOrderDetail(orderId)));
	}

	@GetMapping("/me/{orderId}/timeline")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<OrderTimelineResponseRecord>> getMyOrderTimeline(
			@PathVariable Long orderId) {
		return ResponseEntity.ok(ApiResponse.success("OK", orderService.getMyOrderTimeline(orderId)));
	}

	@PostMapping("/me/{orderId}/cancel")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<OrderDetailResponseRecord>> cancelOrder(
			@PathVariable Long orderId) {
		return ResponseEntity.ok(ApiResponse.success("Cancelled", orderService.cancelOrder(orderId)));
	}

	@PutMapping("/me/{orderId}/address")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<OrderDetailResponseRecord>> updateMyOrderAddress(
			@PathVariable Long orderId,
			@Valid @RequestBody OrderAddressUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", orderService.updateMyOrderAddress(orderId, request)));
	}

	@PostMapping("/me/{orderId}/confirm-delivery")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<OrderDetailResponseRecord>> confirmDelivery(
			@PathVariable Long orderId) {
		return ResponseEntity.ok(ApiResponse.success("Completed", orderService.confirmDelivery(orderId)));
	}

	@GetMapping("/workshop")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<Page<OrderSummaryResponseRecord>>> getWorkshopOrders(
			@RequestParam(required = false) String status,
			@RequestParam(required = false) String orderType,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("OK", orderService.getWorkshopOrders(status, orderType, pageable)));
	}

	@PostMapping("/workshop/{orderId}/accept")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<OrderDetailResponseRecord>> acceptOrder(
			@PathVariable Long orderId) {
		return ResponseEntity.ok(ApiResponse.success("Accepted", orderService.acceptOrder(orderId)));
	}

	@PostMapping("/workshop/{orderId}/reject")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<OrderDetailResponseRecord>> rejectOrder(
			@PathVariable Long orderId) {
		return ResponseEntity.ok(ApiResponse.success("Rejected", orderService.rejectOrder(orderId)));
	}

	@PostMapping("/workshop/{orderId}/status")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<OrderDetailResponseRecord>> updateOrderStatus(
			@PathVariable Long orderId,
			@Valid @RequestBody OrderStatusUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", orderService.updateOrderStatus(orderId, request)));
	}

	@PostMapping("/workshop/{orderId}/tracking")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<OrderDetailResponseRecord>> updateTrackingCode(
			@PathVariable Long orderId,
			@Valid @RequestBody TrackingCodeRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", orderService.updateTrackingCode(orderId, request)));
	}

	@GetMapping("/workshop/{orderId}")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<OrderDetailResponseRecord>> getWorkshopOrderDetail(
			@PathVariable Long orderId) {
		return ResponseEntity.ok(ApiResponse.success("OK", orderService.getWorkshopOrderDetail(orderId)));
	}

	@GetMapping("/workshop/{orderId}/timeline")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<OrderTimelineResponseRecord>> getWorkshopOrderTimeline(
			@PathVariable Long orderId) {
		return ResponseEntity.ok(ApiResponse.success("OK", orderService.getWorkshopOrderTimeline(orderId)));
	}

	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<OrderSummaryResponseRecord>>> getAllOrders(
			@RequestParam(required = false) String status,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("OK", orderService.getAllOrders(status, pageable)));
	}

	@PostMapping("/admin/{orderId}/force-cancel")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<OrderDetailResponseRecord>> forceCancel(
			@PathVariable Long orderId) {
		return ResponseEntity.ok(ApiResponse.success("Cancelled", orderService.forceCancel(orderId)));
	}

	@GetMapping("/admin/stats")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<OrderStatsResponseRecord>> getOrderStats(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) String groupBy) {
		return ResponseEntity.ok(ApiResponse.success("OK", orderService.getOrderStats(from, to, groupBy)));
	}
}
