package vn.io.sanmaymac.modules.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.sanmaymac.common.enums.OrderStatus;
import vn.io.sanmaymac.common.enums.OrderType;
import vn.io.sanmaymac.common.enums.PaymentStatus;
import vn.io.sanmaymac.common.enums.QuoteStatus;
import vn.io.sanmaymac.common.utils.SecurityUtils;
import vn.io.sanmaymac.modules.bidding.entity.QuoteEntity;
import vn.io.sanmaymac.modules.bidding.repository.QuoteRepository;
import vn.io.sanmaymac.modules.catalog.entity.ProductEntity;
import vn.io.sanmaymac.modules.catalog.entity.ProductVariantEntity;
import vn.io.sanmaymac.modules.catalog.repository.ProductVariantRepository;
import vn.io.sanmaymac.modules.coupon.service.CouponService;
import vn.io.sanmaymac.modules.order.dto.CartAddRequest;
import vn.io.sanmaymac.modules.order.dto.CartItemResponseRecord;
import vn.io.sanmaymac.modules.order.dto.CartResponseRecord;
import vn.io.sanmaymac.modules.order.dto.CartUpdateRequest;
import vn.io.sanmaymac.modules.order.dto.CheckoutCustomRequest;
import vn.io.sanmaymac.modules.order.dto.CheckoutReadyMadeRequest;
import vn.io.sanmaymac.modules.order.dto.CheckoutReadyMadeResponseRecord;
import vn.io.sanmaymac.modules.order.dto.OrderDetailItemResponseRecord;
import vn.io.sanmaymac.modules.order.dto.OrderDetailResponseRecord;
import vn.io.sanmaymac.modules.order.dto.OrderAddressUpdateRequest;
import vn.io.sanmaymac.modules.order.dto.OrderStatsItemRecord;
import vn.io.sanmaymac.modules.order.dto.OrderStatsResponseRecord;
import vn.io.sanmaymac.modules.order.dto.OrderStatusUpdateRequest;
import vn.io.sanmaymac.modules.order.dto.OrderTimelineItemResponseRecord;
import vn.io.sanmaymac.modules.order.dto.OrderTimelineResponseRecord;
import vn.io.sanmaymac.modules.order.dto.OrderSummaryResponseRecord;
import vn.io.sanmaymac.modules.order.dto.TrackingCodeRequest;
import vn.io.sanmaymac.modules.order.entity.CartEntity;
import vn.io.sanmaymac.modules.order.entity.CartItemEntity;
import vn.io.sanmaymac.modules.order.entity.OrderDetailEntity;
import vn.io.sanmaymac.modules.order.entity.OrderEntity;
import vn.io.sanmaymac.modules.order.repository.CartItemRepository;
import vn.io.sanmaymac.modules.order.repository.CartRepository;
import vn.io.sanmaymac.modules.order.repository.OrderDetailRepository;
import vn.io.sanmaymac.modules.order.repository.OrderRepository;
import vn.io.sanmaymac.modules.message.service.WorkshopMessageService;
import vn.io.sanmaymac.modules.notification.service.WorkshopNotificationService;
import vn.io.sanmaymac.modules.shipping.dto.ShippingQuoteResponseRecord;
import vn.io.sanmaymac.modules.shipping.service.GhnService;
import vn.io.sanmaymac.modules.user.entity.UserAddressEntity;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.repository.UserAddressRepository;
import vn.io.sanmaymac.modules.user.repository.UserRepository;

@Service
@Transactional
public class OrderService {
	private static final String GROUP_BY_DAY = "day";
	private static final String GROUP_BY_MONTH = "month";

	private final CartRepository cartRepository;
	private final CartItemRepository cartItemRepository;
	private final OrderRepository orderRepository;
	private final OrderDetailRepository orderDetailRepository;
	private final ProductVariantRepository productVariantRepository;
	private final QuoteRepository quoteRepository;
	private final UserRepository userRepository;
	private final UserAddressRepository userAddressRepository;
	private final CouponService couponService;
	private final WorkshopMessageService workshopMessageService;
	private final WorkshopNotificationService workshopNotificationService;
	private final GhnService ghnService;

	public OrderService(
			CartRepository cartRepository,
			CartItemRepository cartItemRepository,
			OrderRepository orderRepository,
			OrderDetailRepository orderDetailRepository,
			ProductVariantRepository productVariantRepository,
			QuoteRepository quoteRepository,
			UserRepository userRepository,
			UserAddressRepository userAddressRepository,
			CouponService couponService,
			WorkshopMessageService workshopMessageService,
			WorkshopNotificationService workshopNotificationService,
			GhnService ghnService) {
		this.cartRepository = cartRepository;
		this.cartItemRepository = cartItemRepository;
		this.orderRepository = orderRepository;
		this.orderDetailRepository = orderDetailRepository;
		this.productVariantRepository = productVariantRepository;
		this.quoteRepository = quoteRepository;
		this.userRepository = userRepository;
		this.userAddressRepository = userAddressRepository;
		this.couponService = couponService;
		this.workshopMessageService = workshopMessageService;
		this.workshopNotificationService = workshopNotificationService;
		this.ghnService = ghnService;
	}

	public CartResponseRecord getMyCart() {
		UserEntity customer = getCurrentUser();
		CartEntity cart = getOrCreateCart(customer);
		return buildCartResponse(cart);
	}

	public CartResponseRecord addToCart(CartAddRequest request) {
		UserEntity customer = getCurrentUser();
		CartEntity cart = getOrCreateCart(customer);
		ProductVariantEntity variant = productVariantRepository.findById(request.variantId())
				.orElseThrow(() -> new IllegalArgumentException("Variant not found"));

		CartItemEntity item = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variant.getId())
				.orElseGet(() -> CartItemEntity.builder()
						.cart(cart)
						.variant(variant)
						.quantity(0)
						.build());
		item.setQuantity(item.getQuantity() + request.quantity());
		cartItemRepository.save(item);
		return buildCartResponse(cart);
	}

	public CartResponseRecord updateCartItem(Long itemId, CartUpdateRequest request) {
		UserEntity customer = getCurrentUser();
		CartEntity cart = getOrCreateCart(customer);
		CartItemEntity item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
				.orElseThrow(() -> new IllegalArgumentException("Cart item not found"));

		if (request.quantity() <= 0) {
			cartItemRepository.delete(item);
		} else {
			item.setQuantity(request.quantity());
			cartItemRepository.save(item);
		}
		return buildCartResponse(cart);
	}

	public CartResponseRecord removeCartItem(Long itemId) {
		UserEntity customer = getCurrentUser();
		CartEntity cart = getOrCreateCart(customer);
		CartItemEntity item = cartItemRepository.findByIdAndCartId(itemId, cart.getId())
				.orElseThrow(() -> new IllegalArgumentException("Cart item not found"));
		cartItemRepository.delete(item);
		return buildCartResponse(cart);
	}

	public void clearCart() {
		UserEntity customer = getCurrentUser();
		CartEntity cart = getOrCreateCart(customer);
		cartItemRepository.deleteByCartId(cart.getId());
	}

	public CheckoutReadyMadeResponseRecord checkoutReadyMade(CheckoutReadyMadeRequest request) {
		UserEntity customer = getCurrentUser();
		CartEntity cart = getOrCreateCart(customer);
		List<CartItemEntity> items = cartItemRepository.findByCartId(cart.getId());
		if (items.isEmpty()) {
			throw new IllegalStateException("Cart is empty");
		}

		UserAddressEntity address = userAddressRepository.findByIdAndUserId(request.addressId(), customer.getId())
				.orElseThrow(() -> new IllegalArgumentException("Address not found"));

		Map<Long, List<CartItemEntity>> itemsByWorkshop = groupCartItemsByWorkshop(items);
		BigDecimal cartSubtotal = calculateCartTotal(items);
		String normalizedCoupon = request.couponCode() == null
				? null
				: request.couponCode().trim().toUpperCase(Locale.ROOT);
		BigDecimal totalDiscount = normalizedCoupon == null || normalizedCoupon.isBlank()
				? BigDecimal.ZERO
				: couponService.calculateDiscount(request.couponCode(), cartSubtotal);

		String checkoutBatchId = UUID.randomUUID().toString();
		List<OrderDetailResponseRecord> createdOrders = new ArrayList<>();
		BigDecimal grandTotal = BigDecimal.ZERO;
		int workshopIndex = 0;
		int workshopCount = itemsByWorkshop.size();
		BigDecimal allocatedDiscount = BigDecimal.ZERO;

		for (List<CartItemEntity> workshopItems : itemsByWorkshop.values()) {
			UserEntity workshop = resolveWorkshopForItems(workshopItems);
			BigDecimal workshopSubtotal = calculateCartTotal(workshopItems);
			BigDecimal workshopDiscount;
			if (totalDiscount.compareTo(BigDecimal.ZERO) == 0) {
				workshopDiscount = BigDecimal.ZERO;
			} else if (workshopIndex == workshopCount - 1) {
				workshopDiscount = totalDiscount.subtract(allocatedDiscount);
			} else {
				workshopDiscount = totalDiscount.multiply(workshopSubtotal)
						.divide(cartSubtotal, 2, RoundingMode.HALF_UP);
				allocatedDiscount = allocatedDiscount.add(workshopDiscount);
			}

			int workshopQuantity = workshopItems.stream()
					.mapToInt(item -> item.getQuantity() != null ? item.getQuantity() : 1)
					.sum();
			ShippingQuoteResponseRecord shippingQuote = ghnService.calculateFee(address, workshopQuantity);
			BigDecimal shippingFee = shippingQuote.available()
					? shippingQuote.fee()
					: BigDecimal.ZERO;
			BigDecimal finalAmount = workshopSubtotal.subtract(workshopDiscount).add(shippingFee);
			if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
				finalAmount = BigDecimal.ZERO;
			}

			OrderEntity order = OrderEntity.builder()
					.customer(customer)
					.workshop(workshop)
					.address(address)
					.checkoutBatchId(checkoutBatchId)
					.frontDesignUrl(null)
					.backDesignUrl(null)
					.orderType(OrderType.READY_MADE)
					.status(OrderStatus.PENDING)
					.paymentStatus(PaymentStatus.UNPAID)
					.couponCode(normalizedCoupon)
					.discountAmount(workshopDiscount)
					.shippingFee(shippingFee)
					.customerNote(request.customerNote())
					.totalAmount(finalAmount)
					.build();
			OrderEntity savedOrder = orderRepository.save(order);

			for (CartItemEntity item : workshopItems) {
				ProductVariantEntity variant = item.getVariant();
				ProductEntity product = variant != null ? variant.getProduct() : null;
				BigDecimal unitPrice = resolveUnitPrice(variant, product);
				OrderDetailEntity detail = OrderDetailEntity.builder()
						.order(savedOrder)
						.variant(variant)
						.productName(product != null ? product.getName() : null)
						.quantity(item.getQuantity())
						.unitPrice(unitPrice)
						.build();
				orderDetailRepository.save(detail);
			}

			workshopMessageService.createThreadForOrder(savedOrder);
			notifyOrderParticipants(savedOrder, "Đơn hàng mới", "Đơn hàng vừa được tạo");
			createdOrders.add(buildOrderDetail(savedOrder));
			grandTotal = grandTotal.add(finalAmount);
			workshopIndex++;
		}

		cartItemRepository.deleteByCartId(cart.getId());
		return new CheckoutReadyMadeResponseRecord(checkoutBatchId, grandTotal, createdOrders, createdOrders.size());
	}

	public OrderDetailResponseRecord checkoutCustom(CheckoutCustomRequest request) {
		UserEntity customer = getCurrentUser();
		QuoteEntity quote = quoteRepository.findById(request.quoteId())
				.orElseThrow(() -> new IllegalArgumentException("Quote not found"));
		if (!QuoteStatus.ACCEPTED.equals(quote.getStatus())) {
			throw new IllegalStateException("Quote not accepted");
		}
		if (quote.getPost() == null || quote.getPost().getCustomer() == null
				|| !quote.getPost().getCustomer().getId().equals(customer.getId())) {
			throw new IllegalStateException("Quote does not belong to customer");
		}

		UserAddressEntity address = userAddressRepository.findByIdAndUserId(request.addressId(), customer.getId())
				.orElseThrow(() -> new IllegalArgumentException("Address not found"));

		OrderEntity order = OrderEntity.builder()
				.customer(customer)
				.workshop(quote.getWorkshop())
				.quote(quote)
				.address(address)
				.frontDesignUrl(quote.getPost() != null ? quote.getPost().getFrontDesignUrl() : null)
				.backDesignUrl(quote.getPost() != null ? quote.getPost().getBackDesignUrl() : null)
				.orderType(OrderType.CUSTOM)
				.status(OrderStatus.PENDING)
				.paymentStatus(PaymentStatus.UNPAID)
				.discountAmount(BigDecimal.ZERO)
				.totalAmount(quote.getOfferedPrice() == null ? BigDecimal.ZERO : quote.getOfferedPrice())
				.build();
		OrderEntity savedOrder = orderRepository.save(order);
		workshopMessageService.createThreadForOrder(savedOrder);
		notifyOrderParticipants(savedOrder, "Đơn hàng mới", "Đơn hàng custom vừa được tạo");
		return buildOrderDetail(savedOrder);
	}

	public Page<OrderSummaryResponseRecord> getMyOrders(String status, Pageable pageable) {
		UserEntity customer = getCurrentUser();
		Page<OrderEntity> orders = status == null || status.isBlank()
				? orderRepository.findByCustomerId(customer.getId(), pageable)
				: orderRepository.findByCustomerIdAndStatus(customer.getId(), parseStatus(status), pageable);
		return orders.map(this::mapOrderSummary);
	}

	public OrderDetailResponseRecord getMyOrderDetail(Long orderId) {
		UserEntity customer = getCurrentUser();
		OrderEntity order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found"));
		if (order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId())) {
			throw new IllegalStateException("No permission to view order");
		}
		return buildOrderDetail(order);
	}

	public OrderTimelineResponseRecord getMyOrderTimeline(Long orderId) {
		UserEntity customer = getCurrentUser();
		OrderEntity order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found"));
		if (order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId())) {
			throw new IllegalStateException("No permission to view order timeline");
		}
		return buildOrderTimeline(order);
	}

	public OrderDetailResponseRecord getWorkshopOrderDetail(Long orderId) {
		return buildOrderDetail(getOrderForWorkshop(orderId));
	}

	public OrderTimelineResponseRecord getWorkshopOrderTimeline(Long orderId) {
		OrderEntity order = getOrderForWorkshop(orderId);
		return buildOrderTimeline(order);
	}

	private OrderTimelineResponseRecord buildOrderTimeline(OrderEntity order) {
		OrderStatus current = order.getStatus();
		List<OrderTimelineItemResponseRecord> items = List.of(
				new OrderTimelineItemResponseRecord("PENDING", "Chờ xác nhận", isCompleted(current, OrderStatus.PENDING), order.getCreatedAt(), "Đơn được tạo"),
				new OrderTimelineItemResponseRecord("DEPOSITED", "Đã tiếp nhận", isCompleted(current, OrderStatus.DEPOSITED), completedAt(order, OrderStatus.DEPOSITED), "Xưởng đã nhận đơn"),
				new OrderTimelineItemResponseRecord("PRODUCING", "Đang sản xuất", isCompleted(current, OrderStatus.PRODUCING), completedAt(order, OrderStatus.PRODUCING), "Đơn đang được sản xuất"),
				new OrderTimelineItemResponseRecord("SHIPPED", "Đang giao", isCompleted(current, OrderStatus.SHIPPED), completedAt(order, OrderStatus.SHIPPED), "Đơn đã được bàn giao vận chuyển"),
				new OrderTimelineItemResponseRecord("COMPLETED", "Hoàn tất", isCompleted(current, OrderStatus.COMPLETED), completedAt(order, OrderStatus.COMPLETED), "Đơn đã hoàn tất")
		);
		return new OrderTimelineResponseRecord(order.getId(), current != null ? current.name() : null, items);
	}

	public OrderDetailResponseRecord updateMyOrderAddress(Long orderId, OrderAddressUpdateRequest request) {
		UserEntity customer = getCurrentUser();
		OrderEntity order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found"));
		if (order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId())) {
			throw new IllegalStateException("No permission to update order");
		}
		if (!OrderStatus.PENDING.equals(order.getStatus())) {
			throw new IllegalStateException("Only pending orders can update address");
		}
		UserAddressEntity address = userAddressRepository.findByIdAndUserId(request.addressId(), customer.getId())
				.orElseThrow(() -> new IllegalArgumentException("Address not found"));
		order.setAddress(address);
		orderRepository.save(order);
		return buildOrderDetail(order);
	}

	public OrderDetailResponseRecord cancelOrder(Long orderId) {
		UserEntity customer = getCurrentUser();
		OrderEntity order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found"));
		if (order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId())) {
			throw new IllegalStateException("No permission to cancel order");
		}
		if (!OrderStatus.PENDING.equals(order.getStatus())) {
			throw new IllegalStateException("Order cannot be cancelled");
		}
		order.setStatus(OrderStatus.CANCELLED);
		orderRepository.save(order);
		return buildOrderDetail(order);
	}

	public OrderDetailResponseRecord confirmDelivery(Long orderId) {
		UserEntity customer = getCurrentUser();
		OrderEntity order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found"));
		if (order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId())) {
			throw new IllegalStateException("No permission to confirm order");
		}
		if (!OrderStatus.SHIPPED.equals(order.getStatus())) {
			throw new IllegalStateException("Order is not shipped");
		}
		order.setStatus(OrderStatus.COMPLETED);
		orderRepository.save(order);
		return buildOrderDetail(order);
	}

	public Page<OrderSummaryResponseRecord> getWorkshopOrders(String status, String orderType, Pageable pageable) {
		UserEntity workshop = getCurrentUser();
		OrderStatus parsedStatus = parseOrderStatus(status);
		OrderType parsedOrderType = parseOrderType(orderType);
		Page<OrderEntity> orders;
		if (parsedStatus != null && parsedOrderType != null) {
			orders = orderRepository.findByWorkshopIdAndStatusAndOrderType(
					workshop.getId(),
					parsedStatus,
					parsedOrderType,
					pageable);
		} else if (parsedStatus != null) {
			orders = orderRepository.findByWorkshopIdAndStatus(workshop.getId(), parsedStatus, pageable);
		} else if (parsedOrderType != null) {
			orders = orderRepository.findByWorkshopIdAndOrderType(workshop.getId(), parsedOrderType, pageable);
		} else {
			orders = orderRepository.findByWorkshopId(workshop.getId(), pageable);
		}
		return orders.map(this::mapOrderSummary);
	}

	public OrderDetailResponseRecord acceptOrder(Long orderId) {
		OrderEntity order = getOrderForWorkshop(orderId);
		if (!OrderStatus.PENDING.equals(order.getStatus())) {
			throw new IllegalStateException("Order is not pending");
		}
		order.setStatus(OrderStatus.DEPOSITED);
		orderRepository.save(order);
		notifyOrderParticipants(order, "Đơn hàng đã được tiếp nhận", "Xưởng đã tiếp nhận đơn hàng");
		return buildOrderDetail(order);
	}

	public OrderDetailResponseRecord rejectOrder(Long orderId) {
		OrderEntity order = getOrderForWorkshop(orderId);
		if (!OrderStatus.PENDING.equals(order.getStatus())) {
			throw new IllegalStateException("Order is not pending");
		}
		order.setStatus(OrderStatus.CANCELLED);
		orderRepository.save(order);
		notifyOrderParticipants(order, "Đơn hàng bị từ chối", "Xưởng đã từ chối đơn hàng");
		return buildOrderDetail(order);
	}

	public OrderDetailResponseRecord updateOrderStatus(Long orderId, OrderStatusUpdateRequest request) {
		OrderEntity order = getOrderForWorkshop(orderId);
		OrderStatus nextStatus = parseStatus(request.status());
		ensureWorkshopTransition(order.getStatus(), nextStatus);
		if (OrderStatus.SHIPPED.equals(nextStatus) && order.getGhnOrderCode() == null) {
			List<OrderDetailEntity> details = orderDetailRepository.findByOrderId(order.getId());
			String ghnOrderCode = ghnService.createShippingOrder(order, details);
			if (ghnOrderCode != null && !ghnOrderCode.isBlank()) {
				order.setGhnOrderCode(ghnOrderCode);
				if (order.getTrackingCode() == null || order.getTrackingCode().isBlank()) {
					order.setTrackingCode(ghnOrderCode);
				}
			}
		}
		order.setStatus(nextStatus);
		orderRepository.save(order);
		notifyOrderParticipants(order, "Trạng thái đơn hàng đã thay đổi", "Đơn hàng vừa được cập nhật tiến độ");
		return buildOrderDetail(order);
	}

	public OrderDetailResponseRecord updateTrackingCode(Long orderId, TrackingCodeRequest request) {
		OrderEntity order = getOrderForWorkshop(orderId);
		order.setTrackingCode(request.trackingCode());
		orderRepository.save(order);
		notifyOrderParticipants(order, "Đã cập nhật mã vận đơn", "Đơn hàng vừa có thông tin vận chuyển mới");
		return buildOrderDetail(order);
	}

	public Page<OrderSummaryResponseRecord> getAllOrders(String status, Pageable pageable) {
		Page<OrderEntity> orders = status == null || status.isBlank()
				? orderRepository.findAll(pageable)
				: orderRepository.findByStatus(parseStatus(status), pageable);
		return orders.map(this::mapOrderSummary);
	}

	public OrderDetailResponseRecord forceCancel(Long orderId) {
		OrderEntity order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found"));
		if (OrderStatus.COMPLETED.equals(order.getStatus())) {
			throw new IllegalStateException("Order already completed");
		}
		order.setStatus(OrderStatus.CANCELLED);
		orderRepository.save(order);
		return buildOrderDetail(order);
	}

	public OrderStatsResponseRecord getOrderStats(LocalDate from, LocalDate to, String groupBy) {
		ZoneId zoneId = ZoneId.systemDefault();
		LocalDate today = LocalDate.now(zoneId);
		LocalDate fromDate = from != null ? from : today.minusDays(30);
		LocalDate toDate = to != null ? to : today;

		String normalizedGroup = groupBy == null || groupBy.isBlank()
				? GROUP_BY_DAY
				: groupBy.toLowerCase(Locale.ROOT);
		if (!GROUP_BY_DAY.equals(normalizedGroup) && !GROUP_BY_MONTH.equals(normalizedGroup)) {
			throw new IllegalArgumentException("Invalid groupBy");
		}

		Instant fromInstant = fromDate.atStartOfDay(zoneId).toInstant();
		Instant toInstant = toDate.plusDays(1).atStartOfDay(zoneId).minusNanos(1).toInstant();
		List<OrderEntity> orders = orderRepository.findByCreatedAtBetween(fromInstant, toInstant);

		Map<String, OrderStatsAccumulator> buckets = new HashMap<>();
		for (OrderEntity order : orders) {
			if (OrderStatus.CANCELLED.equals(order.getStatus())) {
				continue;
			}
			Instant createdAt = order.getCreatedAt();
			if (createdAt == null) {
				continue;
			}
			String key = buildStatsKey(createdAt, zoneId, normalizedGroup);
			OrderStatsAccumulator accumulator = buckets.computeIfAbsent(key, k -> new OrderStatsAccumulator());
			accumulator.totalOrders++;
			accumulator.totalRevenue = accumulator.totalRevenue.add(
					order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount());
		}

		List<OrderStatsItemRecord> items = buckets.entrySet().stream()
				.sorted(Map.Entry.comparingByKey())
				.map(entry -> new OrderStatsItemRecord(
						entry.getKey(),
						entry.getValue().totalOrders,
						entry.getValue().totalRevenue))
				.toList();

		return new OrderStatsResponseRecord(normalizedGroup, items);
	}

	private UserEntity getCurrentUser() {
		String email = SecurityUtils.getCurrentUserEmail();
		if (email == null || email.isBlank()) {
			throw new IllegalStateException("Unauthenticated");
		}
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
	}

	private CartEntity getOrCreateCart(UserEntity customer) {
		return cartRepository.findByCustomerId(customer.getId())
				.orElseGet(() -> cartRepository.save(CartEntity.builder().customer(customer).build()));
	}

	private CartResponseRecord buildCartResponse(CartEntity cart) {
		List<CartItemEntity> items = cartItemRepository.findByCartId(cart.getId());
		List<CartItemResponseRecord> responseItems = new ArrayList<>();
		BigDecimal subTotal = BigDecimal.ZERO;
		for (CartItemEntity item : items) {
			ProductVariantEntity variant = item.getVariant();
			ProductEntity product = variant != null ? variant.getProduct() : null;
			BigDecimal unitPrice = resolveUnitPrice(variant, product);
			BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
			responseItems.add(new CartItemResponseRecord(
					item.getId(),
					variant != null ? variant.getId() : null,
					product != null ? product.getId() : null,
					product != null ? product.getName() : null,
					variant != null ? variant.getColor() : null,
					variant != null ? variant.getSize() : null,
					item.getQuantity(),
					unitPrice,
					totalPrice));
			subTotal = subTotal.add(totalPrice);
		}
		return new CartResponseRecord(responseItems, subTotal);
	}

	private Map<Long, List<CartItemEntity>> groupCartItemsByWorkshop(List<CartItemEntity> items) {
		Map<Long, List<CartItemEntity>> grouped = new LinkedHashMap<>();
		for (CartItemEntity item : items) {
			UserEntity workshop = resolveWorkshopForItem(item);
			grouped.computeIfAbsent(workshop.getId(), ignored -> new ArrayList<>()).add(item);
		}
		return grouped;
	}

	private UserEntity resolveWorkshopForItem(CartItemEntity item) {
		ProductVariantEntity variant = item.getVariant();
		ProductEntity product = variant != null ? variant.getProduct() : null;
		UserEntity workshop = product != null ? product.getWorkshop() : null;
		if (workshop == null) {
			throw new IllegalStateException("Workshop not found for cart item");
		}
		return workshop;
	}

	private UserEntity resolveWorkshopForItems(List<CartItemEntity> items) {
		if (items.isEmpty()) {
			throw new IllegalStateException("Workshop not found for cart item");
		}
		return resolveWorkshopForItem(items.get(0));
	}

	private BigDecimal calculateCartTotal(List<CartItemEntity> items) {
		BigDecimal total = BigDecimal.ZERO;
		for (CartItemEntity item : items) {
			ProductVariantEntity variant = item.getVariant();
			ProductEntity product = variant != null ? variant.getProduct() : null;
			BigDecimal unitPrice = resolveUnitPrice(variant, product);
			total = total.add(unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
		}
		return total;
	}

	private BigDecimal resolveUnitPrice(ProductVariantEntity variant, ProductEntity product) {
		if (variant != null && variant.getPrice() != null) {
			return variant.getPrice();
		}
		if (product != null && product.getBasePrice() != null) {
			return product.getBasePrice();
		}
		return BigDecimal.ZERO;
	}

	    private OrderSummaryResponseRecord mapOrderSummary(OrderEntity order) {
		return new OrderSummaryResponseRecord(
			order.getId(),
			order.getOrderType() != null ? order.getOrderType().name() : null,
			order.getStatus() != null ? order.getStatus().name() : null,
			order.getTotalAmount(),
			order.getCheckoutBatchId(),
			order.getTrackingCode(),
			order.getCreatedAt());
	    }

	private OrderDetailResponseRecord buildOrderDetail(OrderEntity order) {
		List<OrderDetailEntity> details = orderDetailRepository.findByOrderId(order.getId());
		List<OrderDetailItemResponseRecord> items = details.stream()
				.map(detail -> new OrderDetailItemResponseRecord(
						detail.getId(),
						detail.getVariant() != null ? detail.getVariant().getId() : null,
						detail.getProductName(),
						detail.getQuantity(),
						detail.getUnitPrice(),
						detail.getUnitPrice() != null
								? detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity()))
								: BigDecimal.ZERO))
				.toList();

		return new OrderDetailResponseRecord(
				order.getId(),
				order.getOrderType() != null ? order.getOrderType().name() : null,
				order.getStatus() != null ? order.getStatus().name() : null,
				order.getTotalAmount(),
				order.getShippingFee(),
				order.getCustomerNote(),
				order.getCheckoutBatchId(),
				order.getTrackingCode(),
				order.getGhnOrderCode(),
				order.getFrontDesignUrl(),
				order.getBackDesignUrl(),
				order.getWorkshop() != null ? order.getWorkshop().getId() : null,
				order.getWorkshop() != null ? order.getWorkshop().getFullName() : null,
				order.getAddress() != null ? order.getAddress().getId() : null,
				items,
				order.getCreatedAt());
	}

	private OrderEntity getOrderForWorkshop(Long orderId) {
		UserEntity workshop = getCurrentUser();
		OrderEntity order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found"));
		if (order.getWorkshop() == null || !order.getWorkshop().getId().equals(workshop.getId())) {
			throw new IllegalStateException("No permission to update order");
		}
		return order;
	}

	private OrderStatus parseStatus(String status) {
		return OrderStatus.valueOf(status.toUpperCase(Locale.ROOT));
	}

	private OrderStatus parseOrderStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		return parseStatus(status);
	}

	private OrderType parseOrderType(String orderType) {
		if (orderType == null || orderType.isBlank()) {
			return null;
		}
		return OrderType.valueOf(orderType.toUpperCase(Locale.ROOT));
	}

	private void ensureWorkshopTransition(OrderStatus current, OrderStatus next) {
		if (next == OrderStatus.DEPOSITED && current == OrderStatus.PENDING) {
			return;
		}
		if (next == OrderStatus.PRODUCING && current == OrderStatus.DEPOSITED) {
			return;
		}
		if (next == OrderStatus.SHIPPED && current == OrderStatus.PRODUCING) {
			return;
		}
		throw new IllegalStateException("Invalid order status transition");
	}

	private String buildStatsKey(Instant createdAt, ZoneId zoneId, String groupBy) {
		if (GROUP_BY_MONTH.equals(groupBy)) {
			YearMonth month = YearMonth.from(createdAt.atZone(zoneId));
			return month.toString();
		}
		LocalDate date = createdAt.atZone(zoneId).toLocalDate();
		return date.toString();
	}

	private boolean isCompleted(OrderStatus current, OrderStatus target) {
		if (current == null || target == null || current == OrderStatus.CANCELLED) {
			return false;
		}
		return current.ordinal() >= target.ordinal();
	}

	private Instant completedAt(OrderEntity order, OrderStatus target) {
		if (order.getStatus() == null || order.getStatus().ordinal() < target.ordinal()) {
			return null;
		}
		return order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt();
	}

	private void notifyOrderParticipants(OrderEntity order, String title, String body) {
		if (order.getCustomer() != null) {
			workshopNotificationService.createNotification(
					order.getCustomer().getId(),
					"ORDER",
					title,
					body,
					"/api/workshop/orders/" + order.getId(),
					order.getId());
		}
		if (order.getWorkshop() != null) {
			workshopNotificationService.createNotification(
					order.getWorkshop().getId(),
					"ORDER",
					title,
					body,
					"/api/workshop/orders/" + order.getId(),
					order.getId());
		}
	}

	private static class OrderStatsAccumulator {
		private long totalOrders;
		private BigDecimal totalRevenue = BigDecimal.ZERO;
	}
}
