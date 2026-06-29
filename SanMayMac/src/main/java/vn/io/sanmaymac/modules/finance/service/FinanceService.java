package vn.io.sanmaymac.modules.finance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.io.sanmaymac.common.enums.OrderStatus;
import vn.io.sanmaymac.common.enums.PaymentStatus;
import vn.io.sanmaymac.common.enums.PayoutStatus;
import vn.io.sanmaymac.common.enums.Role;
import vn.io.sanmaymac.common.enums.TransactionDirection;
import vn.io.sanmaymac.common.enums.TransactionStatus;
import vn.io.sanmaymac.common.enums.TransactionType;
import vn.io.sanmaymac.common.utils.SecurityUtils;
import vn.io.sanmaymac.modules.finance.dto.AiTokenPurchaseRequest;
import vn.io.sanmaymac.modules.finance.dto.BankAccountRequest;
import vn.io.sanmaymac.modules.finance.dto.BankAccountResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.CashflowResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.CheckoutBatchOrderItemRecord;
import vn.io.sanmaymac.modules.finance.dto.CheckoutBatchSummaryResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.CommissionConfigRequest;
import vn.io.sanmaymac.modules.finance.dto.CommissionConfigResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.MomoCreatePaymentRequest;
import vn.io.sanmaymac.modules.finance.dto.MomoCreatePaymentResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.PayoutApprovalRequest;
import vn.io.sanmaymac.modules.finance.dto.PayoutRequestCreate;
import vn.io.sanmaymac.modules.finance.dto.PayoutResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.RevenueSummaryItemRecord;
import vn.io.sanmaymac.modules.finance.dto.RevenueSummaryRecord;
import vn.io.sanmaymac.modules.finance.dto.TransactionResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.WalletResponseRecord;
import vn.io.sanmaymac.modules.finance.entity.BankAccountEntity;
import vn.io.sanmaymac.modules.finance.entity.CommissionConfigEntity;
import vn.io.sanmaymac.modules.finance.entity.PayoutRequestEntity;
import vn.io.sanmaymac.modules.finance.entity.TransactionEntity;
import vn.io.sanmaymac.modules.finance.entity.WalletEntity;
import vn.io.sanmaymac.modules.finance.repository.BankAccountRepository;
import vn.io.sanmaymac.modules.finance.repository.CommissionConfigRepository;
import vn.io.sanmaymac.modules.finance.repository.PayoutRequestRepository;
import vn.io.sanmaymac.modules.finance.repository.TransactionRepository;
import vn.io.sanmaymac.modules.finance.repository.WalletRepository;
import vn.io.sanmaymac.modules.notification.service.WorkshopNotificationService;
import vn.io.sanmaymac.modules.order.entity.OrderEntity;
import vn.io.sanmaymac.modules.order.repository.OrderRepository;
import vn.io.sanmaymac.modules.coupon.service.CouponService;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.repository.UserRepository;

@Service
@Transactional
public class FinanceService {
	private static final BigDecimal DEFAULT_COMMISSION_RATE = BigDecimal.ZERO;
	private static final BigDecimal DEFAULT_DEPOSIT_RATE = new BigDecimal("0.30");
	private static final String GROUP_BY_DAY = "day";
	private static final String GROUP_BY_MONTH = "month";

	private final TransactionRepository transactionRepository;
	private final WalletRepository walletRepository;
	private final BankAccountRepository bankAccountRepository;
	private final PayoutRequestRepository payoutRequestRepository;
	private final CommissionConfigRepository commissionConfigRepository;
	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final CouponService couponService;
	private final WorkshopNotificationService workshopNotificationService;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Value("${app.momo.endpoint:https://test-payment.momo.vn/v2/gateway/api/create}")
	private String momoEndpoint;
	@Value("${app.momo.partner-code:MOMO}")
	private String momoPartnerCode;
	@Value("${app.momo.access-key:}")
	private String momoAccessKey;
	@Value("${app.momo.secret-key:}")
	private String momoSecretKey;
	@Value("${app.momo.redirect-url:http://localhost:3000/payment/momo-return}")
	private String momoRedirectUrl;
	@Value("${app.momo.ipn-url:http://localhost:8080/api/finance/momo/ipn}")
	private String momoIpnUrl;
	@Value("${app.momo.request-type:captureWallet}")
	private String momoRequestType;

	public FinanceService(
			TransactionRepository transactionRepository,
			WalletRepository walletRepository,
			BankAccountRepository bankAccountRepository,
			PayoutRequestRepository payoutRequestRepository,
			CommissionConfigRepository commissionConfigRepository,
			OrderRepository orderRepository,
			UserRepository userRepository,
			CouponService couponService,
			WorkshopNotificationService workshopNotificationService) {
		this.transactionRepository = transactionRepository;
		this.walletRepository = walletRepository;
		this.bankAccountRepository = bankAccountRepository;
		this.payoutRequestRepository = payoutRequestRepository;
		this.commissionConfigRepository = commissionConfigRepository;
		this.orderRepository = orderRepository;
		this.userRepository = userRepository;
		this.couponService = couponService;
		this.workshopNotificationService = workshopNotificationService;
	}

	public WalletResponseRecord getMyWallet() {
		UserEntity user = getCurrentUser();
		WalletEntity wallet = getOrCreateWallet(user);
		return toWalletResponse(wallet);
	}

	public List<TransactionResponseRecord> listMyTransactions(String type, String status) {
		UserEntity user = getCurrentUser();
		List<TransactionEntity> transactions;
		if (type != null && !type.isBlank()) {
			transactions = transactionRepository.findByUserIdAndType(user.getId(), TransactionType.valueOf(type.toUpperCase(Locale.ROOT)));
		} else if (status != null && !status.isBlank()) {
			transactions = transactionRepository.findByUserIdAndStatus(user.getId(), TransactionStatus.valueOf(status.toUpperCase(Locale.ROOT)));
		} else {
			transactions = transactionRepository.findByUserId(user.getId());
		}
		return transactions.stream()
				.sorted(Comparator.comparing(TransactionEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
				.map(this::toTransactionResponse)
				.toList();
	}

	public TransactionResponseRecord payOrder(Long orderId, String paymentMethod, String transactionCode, BigDecimal amount) {
		UserEntity customer = getCurrentUser();
		OrderEntity order = getPayableOrderForCustomer(orderId, customer);
		BigDecimal remainingAmount = getRemainingAmount(order);
		BigDecimal payable = amount != null ? amount : remainingAmount;
		ensureValidPayAmount(payable, remainingAmount);
		return createAndApplyPayment(order, customer, payable, paymentMethod, transactionCode, "Order payment");
	}

	public TransactionResponseRecord payOrderDeposit(Long orderId, BigDecimal amount) {
		UserEntity customer = getCurrentUser();
		OrderEntity order = getPayableOrderForCustomer(orderId, customer);
		if (PaymentStatus.PAID.equals(order.getPaymentStatus())) {
			throw new IllegalStateException("Order is already fully paid");
		}
		BigDecimal remaining = getRemainingAmount(order);
		BigDecimal defaultDeposit = normalize(order.getTotalAmount())
				.multiply(DEFAULT_DEPOSIT_RATE)
				.setScale(2, RoundingMode.HALF_UP);
		BigDecimal payable = amount != null ? amount : defaultDeposit;
		if (payable.compareTo(remaining) > 0) {
			payable = remaining;
		}
		ensureValidPayAmount(payable, remaining);
		return createAndApplyPayment(order, customer, payable, "DEPOSIT", generateInternalCode(order.getId()), "Order deposit");
	}

	public TransactionResponseRecord payOrderBalance(Long orderId) {
		UserEntity customer = getCurrentUser();
		OrderEntity order = getPayableOrderForCustomer(orderId, customer);
		if (order.getCheckoutBatchId() != null && !order.getCheckoutBatchId().isBlank()) {
			List<OrderEntity> batchOrders = getPayableBatchOrders(order.getCheckoutBatchId(), customer);
			if (batchOrders.size() > 1) {
				throw new IllegalStateException("Use checkout batch payment for multi-workshop orders");
			}
		}
		BigDecimal remaining = getRemainingAmount(order);
		if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalStateException("Order balance already paid");
		}
		return createAndApplyPayment(order, customer, remaining, "BALANCE", generateInternalCode(order.getId()), "Order balance payment");
	}

	public CheckoutBatchSummaryResponseRecord getCheckoutBatchSummary(String checkoutBatchId) {
		UserEntity customer = getCurrentUser();
		List<OrderEntity> orders = getPayableBatchOrders(checkoutBatchId, customer);
		BigDecimal grandTotal = BigDecimal.ZERO;
		BigDecimal remainingTotal = BigDecimal.ZERO;
		List<CheckoutBatchOrderItemRecord> items = orders.stream()
				.map(order -> {
					BigDecimal total = normalize(order.getTotalAmount());
					BigDecimal remaining = getRemainingAmount(order);
					return new CheckoutBatchOrderItemRecord(
							order.getId(),
							order.getWorkshop() != null ? order.getWorkshop().getFullName() : null,
							total,
							remaining,
							order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null);
				})
				.toList();
		for (OrderEntity order : orders) {
			grandTotal = grandTotal.add(normalize(order.getTotalAmount()));
			remainingTotal = remainingTotal.add(getRemainingAmount(order));
		}
		return new CheckoutBatchSummaryResponseRecord(
				checkoutBatchId,
				grandTotal,
				remainingTotal,
				orders.size(),
				items);
	}

	public MomoCreatePaymentResponseRecord createMomoPaymentForCheckoutBatch(
			String checkoutBatchId,
			MomoCreatePaymentRequest request) {
		UserEntity customer = getCurrentUser();
		List<OrderEntity> orders = getPayableBatchOrders(checkoutBatchId, customer);
		String phase = request != null && request.phase() != null ? request.phase().trim().toLowerCase(Locale.ROOT) : "full";
		BigDecimal batchRemaining = getBatchRemainingAmount(orders);
		BigDecimal amount = resolveBatchMomoAmount(orders, request, phase, batchRemaining);
		ensureValidPayAmount(amount, batchRemaining);
		ensureMomoConfig();
		ensureNoPendingBatchPayment(checkoutBatchId);

		String requestId = UUID.randomUUID().toString();
		String momoOrderId = "SMM-BATCH-" + checkoutBatchId.substring(0, 8) + "-" + System.currentTimeMillis();
		String orderInfo = request != null && request.orderInfo() != null && !request.orderInfo().isBlank()
				? request.orderInfo()
				: "Thanh toan " + orders.size() + " don hang";
		return createMomoSession(customer, orders.get(0), checkoutBatchId, amount, phase, requestId, momoOrderId, orderInfo);
	}

	public MomoCreatePaymentResponseRecord createMomoPayment(Long orderId, MomoCreatePaymentRequest request) {
		UserEntity customer = getCurrentUser();
		OrderEntity order = getPayableOrderForCustomer(orderId, customer);
		if (order.getCheckoutBatchId() != null && !order.getCheckoutBatchId().isBlank()) {
			List<OrderEntity> batchOrders = getPayableBatchOrders(order.getCheckoutBatchId(), customer);
			if (batchOrders.size() > 1) {
				return createMomoPaymentForCheckoutBatch(order.getCheckoutBatchId(), request);
			}
		}
		String phase = request != null && request.phase() != null ? request.phase().trim().toLowerCase(Locale.ROOT) : "full";
		BigDecimal amount = resolveMomoAmount(order, request, phase);
		BigDecimal remaining = getRemainingAmount(order);
		ensureValidPayAmount(amount, remaining);
		ensureMomoConfig();

		String requestId = UUID.randomUUID().toString();
		String momoOrderId = "SMM-" + order.getId() + "-" + System.currentTimeMillis();
		String orderInfo = request != null && request.orderInfo() != null && !request.orderInfo().isBlank()
				? request.orderInfo()
				: "Thanh toan don hang " + order.getId();
		return createMomoSession(customer, order, null, amount, phase, requestId, momoOrderId, orderInfo);
	}

	private MomoCreatePaymentResponseRecord createMomoSession(
			UserEntity customer,
			OrderEntity primaryOrder,
			String checkoutBatchId,
			BigDecimal amount,
			String phase,
			String requestId,
			String momoOrderId,
			String orderInfo) {
		String extraData = "";

		String rawSignature = "accessKey=" + momoAccessKey
				+ "&amount=" + amount.toBigInteger().toString()
				+ "&extraData=" + extraData
				+ "&ipnUrl=" + momoIpnUrl
				+ "&orderId=" + momoOrderId
				+ "&orderInfo=" + orderInfo
				+ "&partnerCode=" + momoPartnerCode
				+ "&redirectUrl=" + momoRedirectUrl
				+ "&requestId=" + requestId
				+ "&requestType=" + momoRequestType;
		String signature = hmacSha256(momoSecretKey, rawSignature);

		Map<String, Object> payload = new HashMap<>();
		payload.put("partnerCode", momoPartnerCode);
		payload.put("partnerName", "SanMayMac");
		payload.put("storeId", "SanMayMac");
		payload.put("requestId", requestId);
		payload.put("amount", amount.toBigInteger().toString());
		payload.put("orderId", momoOrderId);
		payload.put("orderInfo", orderInfo);
		payload.put("redirectUrl", momoRedirectUrl);
		payload.put("ipnUrl", momoIpnUrl);
		payload.put("lang", "vi");
		payload.put("extraData", extraData);
		payload.put("requestType", momoRequestType);
		payload.put("signature", signature);

		try {
			String body = objectMapper.writeValueAsString(payload);
			HttpRequest httpRequest = HttpRequest.newBuilder()
					.uri(URI.create(momoEndpoint))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(body))
					.build();
			HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
			Map<String, Object> responseMap = objectMapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {
			});
			int resultCode = parseInt(responseMap.get("resultCode"));
			if (resultCode != 0) {
				throw new IllegalStateException("MoMo create payment failed: " + responseMap.get("message"));
			}
			TransactionEntity pending = TransactionEntity.builder()
					.order(primaryOrder)
					.checkoutBatchId(checkoutBatchId)
					.user(customer)
					.amount(amount)
					.paymentMethod("MOMO")
					.transactionCode(momoOrderId)
					.type(TransactionType.ORDER_PAYMENT)
					.direction(TransactionDirection.OUT)
					.description(checkoutBatchId != null ? "MoMo batch " + phase + " payment" : "MoMo " + phase + " payment")
					.status(TransactionStatus.PENDING)
					.build();
			transactionRepository.save(pending);
			return new MomoCreatePaymentResponseRecord(
					stringValue(responseMap.get("payUrl")),
					stringValue(responseMap.get("requestId")),
					stringValue(responseMap.get("orderId")),
					amount,
					stringValue(responseMap.get("message")));
		} catch (IllegalStateException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to create MoMo payment", ex);
		}
	}

	public Map<String, Object> handleMomoIpn(Map<String, Object> payload) {
		String signature = stringValue(payload.get("signature"));
		if (signature == null || signature.isBlank()) {
			return Map.of("resultCode", 1, "message", "Missing signature");
		}
		if (!verifyMomoIpnSignature(payload, signature)) {
			return Map.of("resultCode", 1, "message", "Invalid signature");
		}
		String momoOrderId = stringValue(payload.get("orderId"));
		TransactionEntity payment = transactionRepository.findByTransactionCode(momoOrderId)
				.orElse(null);
		if (payment == null) {
			return Map.of("resultCode", 1, "message", "Transaction not found");
		}
		if (TransactionStatus.SUCCESS.equals(payment.getStatus())) {
			return Map.of("resultCode", 0, "message", "Processed");
		}
		int resultCode = parseInt(payload.get("resultCode"));
		if (resultCode != 0) {
			payment.setStatus(TransactionStatus.FAILED);
			transactionRepository.save(payment);
			return Map.of("resultCode", 0, "message", "Payment failed acknowledged");
		}
		applySuccessfulPayment(payment);
		return Map.of("resultCode", 0, "message", "Success");
	}

	public TransactionResponseRecord purchaseAiTokens(AiTokenPurchaseRequest request) {
		UserEntity user = getCurrentUser();
		WalletEntity wallet = getOrCreateWallet(user);
		wallet.setAiTokenBalance(safeInt(wallet.getAiTokenBalance()) + request.tokenCount());
		walletRepository.save(wallet);

		TransactionEntity transaction = TransactionEntity.builder()
				.user(user)
				.amount(request.amount())
				.type(TransactionType.AI_TOKEN_PURCHASE)
				.direction(TransactionDirection.OUT)
				.description("AI token purchase")
				.status(TransactionStatus.SUCCESS)
				.build();
		transactionRepository.save(transaction);
		return toTransactionResponse(transaction);
	}

	public BankAccountResponseRecord upsertBankAccount(BankAccountRequest request) {
		UserEntity workshop = getWorkshopUser();
		BankAccountEntity account = bankAccountRepository.findByUserId(workshop.getId())
				.orElseGet(() -> BankAccountEntity.builder().user(workshop).build());
		account.setBankName(request.bankName());
		account.setAccountNo(request.accountNo());
		account.setAccountName(request.accountName());
		account.setIsVerified(Boolean.FALSE);
		bankAccountRepository.save(account);
		return toBankAccountResponse(account);
	}

	public BankAccountResponseRecord getMyBankAccount() {
		UserEntity workshop = getWorkshopUser();
		BankAccountEntity account = bankAccountRepository.findByUserId(workshop.getId())
				.orElseThrow(() -> new IllegalArgumentException("Bank account not found"));
		return toBankAccountResponse(account);
	}

	public PayoutResponseRecord createPayoutRequest(PayoutRequestCreate request) {
		UserEntity workshop = getWorkshopUser();
		BankAccountEntity account = bankAccountRepository.findByUserId(workshop.getId())
				.orElseThrow(() -> new IllegalStateException("Bank account not configured"));
		if (!Boolean.TRUE.equals(account.getIsVerified())) {
			throw new IllegalStateException("Bank account not verified");
		}

		WalletEntity wallet = getOrCreateWallet(workshop);
		if (wallet.getAvailableBalance().compareTo(request.amount()) < 0) {
			throw new IllegalStateException("Insufficient balance");
		}

		wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(request.amount()));
		walletRepository.save(wallet);

		PayoutRequestEntity payout = PayoutRequestEntity.builder()
				.workshop(workshop)
				.amount(request.amount())
				.status(PayoutStatus.PENDING)
				.build();
		payoutRequestRepository.save(payout);
		workshopNotificationService.createNotification(
				workshop.getId(),
				"PAYOUT",
				"Yêu cầu rút tiền mới",
				"Yêu cầu rút tiền của bạn đã được tạo",
				null,
				payout.getId());

		TransactionEntity transaction = TransactionEntity.builder()
				.user(workshop)
				.amount(request.amount())
				.type(TransactionType.PAYOUT)
				.direction(TransactionDirection.OUT)
				.description("Payout request")
				.status(TransactionStatus.PENDING)
				.build();
		transactionRepository.save(transaction);

		return toPayoutResponse(payout);
	}

	public List<PayoutResponseRecord> listMyPayouts() {
		UserEntity workshop = getWorkshopUser();
		return payoutRequestRepository.findByWorkshopId(workshop.getId())
				.stream()
				.map(this::toPayoutResponse)
				.toList();
	}

	public PayoutResponseRecord getMyPayout(Long payoutId) {
		UserEntity workshop = getWorkshopUser();
		PayoutRequestEntity payout = payoutRequestRepository.findByIdAndWorkshopId(payoutId, workshop.getId())
				.orElseThrow(() -> new IllegalArgumentException("Payout not found"));
		return toPayoutResponse(payout);
	}

	public PayoutResponseRecord cancelMyPayout(Long payoutId) {
		UserEntity workshop = getWorkshopUser();
		PayoutRequestEntity payout = payoutRequestRepository.findByIdAndWorkshopId(payoutId, workshop.getId())
				.orElseThrow(() -> new IllegalArgumentException("Payout not found"));
		if (!PayoutStatus.PENDING.equals(payout.getStatus())) {
			throw new IllegalStateException("Payout cannot be cancelled");
		}
		payout.setStatus(PayoutStatus.REJECTED);
		payout.setAdminNote("Cancelled by workshop");
		payout.setApprovedAt(Instant.now());
		payoutRequestRepository.save(payout);

		WalletEntity wallet = getOrCreateWallet(workshop);
		wallet.setAvailableBalance(wallet.getAvailableBalance().add(payout.getAmount()));
		walletRepository.save(wallet);

		workshopNotificationService.createNotification(
				workshop.getId(),
				"PAYOUT",
				"Yêu cầu rút tiền đã bị hủy",
				"Yêu cầu rút tiền của bạn đã được hủy",
				null,
				payout.getId());

		return toPayoutResponse(payout);
	}

	public RevenueSummaryRecord getWorkshopRevenue(LocalDate from, LocalDate to, String groupBy) {
		UserEntity workshop = getWorkshopUser();
		List<TransactionEntity> transactions = transactionRepository.findByUserIdAndType(workshop.getId(), TransactionType.ESCROW_RELEASE);
		return buildRevenueSummary(transactions, from, to, groupBy);
	}

	public CashflowResponseRecord getCashflow(LocalDate from, LocalDate to) {
		List<TransactionEntity> transactions = transactionRepository.findByCreatedAtBetween(
				startOfDay(from), endOfDay(to));

		BigDecimal escrowBalance = BigDecimal.ZERO;
		BigDecimal platformRevenue = BigDecimal.ZERO;
		for (TransactionEntity transaction : transactions) {
			if (TransactionType.ESCROW_HOLD.equals(transaction.getType())) {
				escrowBalance = escrowBalance.add(normalize(transaction.getAmount()));
			}
			if (TransactionType.COMMISSION_FEE.equals(transaction.getType())) {
				platformRevenue = platformRevenue.add(normalize(transaction.getAmount()));
			}
		}

		BigDecimal workshopAvailable = walletRepository.findAll().stream()
				.map(WalletEntity::getAvailableBalance)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		return new CashflowResponseRecord(escrowBalance, workshopAvailable, platformRevenue);
	}

	public List<PayoutResponseRecord> listAllPayouts(String status) {
		if (status == null || status.isBlank()) {
			return payoutRequestRepository.findAll().stream().map(this::toPayoutResponse).toList();
		}
		PayoutStatus payoutStatus = PayoutStatus.valueOf(status.toUpperCase(Locale.ROOT));
		return payoutRequestRepository.findByStatus(payoutStatus).stream().map(this::toPayoutResponse).toList();
	}

	public PayoutResponseRecord approvePayout(Long payoutId, PayoutApprovalRequest request) {
		PayoutRequestEntity payout = payoutRequestRepository.findById(payoutId)
				.orElseThrow(() -> new IllegalArgumentException("Payout not found"));
		if (!PayoutStatus.PENDING.equals(payout.getStatus())) {
			throw new IllegalStateException("Payout is not pending");
		}
		payout.setStatus(PayoutStatus.APPROVED);
		payout.setAdminNote(request.adminNote());
		payout.setApprovedAt(Instant.now());
		payoutRequestRepository.save(payout);
		workshopNotificationService.createNotification(
				payout.getWorkshop().getId(),
				"PAYOUT",
				"Yêu cầu rút tiền đã được duyệt",
				"Yêu cầu rút tiền của bạn đã được duyệt",
				null,
				payout.getId());
		return toPayoutResponse(payout);
	}

	public PayoutResponseRecord rejectPayout(Long payoutId, PayoutApprovalRequest request) {
		PayoutRequestEntity payout = payoutRequestRepository.findById(payoutId)
				.orElseThrow(() -> new IllegalArgumentException("Payout not found"));
		if (!PayoutStatus.PENDING.equals(payout.getStatus())) {
			throw new IllegalStateException("Payout is not pending");
		}
		payout.setStatus(PayoutStatus.REJECTED);
		payout.setAdminNote(request.adminNote());
		payout.setApprovedAt(Instant.now());
		payoutRequestRepository.save(payout);

		WalletEntity wallet = getOrCreateWallet(payout.getWorkshop());
		wallet.setAvailableBalance(wallet.getAvailableBalance().add(payout.getAmount()));
		walletRepository.save(wallet);
		workshopNotificationService.createNotification(
				payout.getWorkshop().getId(),
				"PAYOUT",
				"Yêu cầu rút tiền bị từ chối",
				"Yêu cầu rút tiền của bạn đã bị từ chối",
				null,
				payout.getId());
		return toPayoutResponse(payout);
	}

	public CommissionConfigResponseRecord getCommissionConfig() {
		CommissionConfigEntity config = getOrCreateCommissionConfig();
		return new CommissionConfigResponseRecord(config.getId(), config.getCommissionRate());
	}

	public CommissionConfigResponseRecord updateCommissionConfig(CommissionConfigRequest request) {
		CommissionConfigEntity config = getOrCreateCommissionConfig();
		config.setCommissionRate(request.commissionRate());
		commissionConfigRepository.save(config);
		return new CommissionConfigResponseRecord(config.getId(), config.getCommissionRate());
	}

	public void releaseEscrowForOrder(OrderEntity order) {
		if (order.getWorkshop() == null || order.getTotalAmount() == null) {
			return;
		}
		WalletEntity wallet = getOrCreateWallet(order.getWorkshop());
		BigDecimal total = order.getTotalAmount();
		BigDecimal commission = calculateCommission(total);
		BigDecimal net = total.subtract(commission);

		wallet.setPendingBalance(wallet.getPendingBalance().subtract(total));
		wallet.setAvailableBalance(wallet.getAvailableBalance().add(net));
		walletRepository.save(wallet);

		TransactionEntity release = TransactionEntity.builder()
				.order(order)
				.user(order.getWorkshop())
				.amount(net)
				.type(TransactionType.ESCROW_RELEASE)
				.direction(TransactionDirection.IN)
				.description("Escrow released")
				.status(TransactionStatus.SUCCESS)
				.build();
		transactionRepository.save(release);

		if (commission.compareTo(BigDecimal.ZERO) > 0) {
			TransactionEntity fee = TransactionEntity.builder()
					.order(order)
					.user(order.getWorkshop())
					.amount(commission)
					.type(TransactionType.COMMISSION_FEE)
					.direction(TransactionDirection.OUT)
					.description("Commission fee")
					.status(TransactionStatus.SUCCESS)
					.build();
			transactionRepository.save(fee);
		}
	}

	public void refundOrder(OrderEntity order) {
		if (order.getCustomer() == null || order.getTotalAmount() == null) {
			return;
		}
		WalletEntity wallet = getOrCreateWallet(order.getCustomer());
		wallet.setAvailableBalance(wallet.getAvailableBalance().add(order.getTotalAmount()));
		walletRepository.save(wallet);

		TransactionEntity refund = TransactionEntity.builder()
				.order(order)
				.user(order.getCustomer())
				.amount(order.getTotalAmount())
				.type(TransactionType.REFUND)
				.direction(TransactionDirection.IN)
				.description("Order refund")
				.status(TransactionStatus.SUCCESS)
				.build();
		transactionRepository.save(refund);
	}

	private OrderEntity getPayableOrderForCustomer(Long orderId, UserEntity customer) {
		OrderEntity order = orderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found"));
		if (order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId())) {
			throw new IllegalStateException("No permission to pay order");
		}
		if (OrderStatus.CANCELLED.equals(order.getStatus())) {
			throw new IllegalStateException("Order is cancelled");
		}
		return order;
	}

	private BigDecimal getPaidAmount(OrderEntity order) {
		return transactionRepository.findByOrderIdAndTypeAndStatus(
						order.getId(),
						TransactionType.ORDER_PAYMENT,
						TransactionStatus.SUCCESS)
				.stream()
				.map(TransactionEntity::getAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal getRemainingAmount(OrderEntity order) {
		BigDecimal remaining = normalize(order.getTotalAmount()).subtract(getPaidAmount(order));
		if (remaining.compareTo(BigDecimal.ZERO) < 0) {
			return BigDecimal.ZERO;
		}
		return remaining;
	}

	private void ensureValidPayAmount(BigDecimal amount, BigDecimal remainingAmount) {
		if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalStateException("Invalid payment amount");
		}
		if (amount.compareTo(remainingAmount) > 0) {
			throw new IllegalStateException("Payment amount exceeds remaining amount");
		}
	}

	private TransactionResponseRecord createAndApplyPayment(
			OrderEntity order,
			UserEntity customer,
			BigDecimal amount,
			String paymentMethod,
			String transactionCode,
			String description) {
		TransactionEntity payment = TransactionEntity.builder()
				.order(order)
				.user(customer)
				.amount(amount)
				.paymentMethod(paymentMethod)
				.transactionCode(transactionCode == null || transactionCode.isBlank()
						? generateInternalCode(order.getId())
						: transactionCode)
				.type(TransactionType.ORDER_PAYMENT)
				.direction(TransactionDirection.OUT)
				.description(description)
				.status(TransactionStatus.PENDING)
				.build();
		transactionRepository.save(payment);
		applySuccessfulPayment(payment);
		return toTransactionResponse(payment);
	}

	private void applySuccessfulPayment(TransactionEntity payment) {
		if (payment.getCheckoutBatchId() != null && !payment.getCheckoutBatchId().isBlank()) {
			applySuccessfulBatchPayment(payment);
			return;
		}
		OrderEntity order = payment.getOrder();
		if (order == null) {
			throw new IllegalStateException("Payment has no order");
		}
		if (TransactionStatus.SUCCESS.equals(payment.getStatus())) {
			return;
		}
		payment.setStatus(TransactionStatus.SUCCESS);
		transactionRepository.save(payment);

		BigDecimal payable = normalize(payment.getAmount());
			creditWorkshopEscrow(order, payable);
		updateOrderPaymentStatus(order, true);
	}

	private void applySuccessfulBatchPayment(TransactionEntity payment) {
		if (TransactionStatus.SUCCESS.equals(payment.getStatus())) {
			return;
		}
		payment.setStatus(TransactionStatus.SUCCESS);
		transactionRepository.save(payment);

		List<OrderEntity> orders = orderRepository.findByCheckoutBatchIdAndCustomerId(
				payment.getCheckoutBatchId(),
				payment.getUser().getId());
		if (orders.isEmpty()) {
			throw new IllegalStateException("Checkout batch not found for payment");
		}

		List<OrderEntity> unpaidOrders = orders.stream()
				.filter(order -> getRemainingAmount(order).compareTo(BigDecimal.ZERO) > 0)
				.toList();
		if (unpaidOrders.isEmpty()) {
			return;
		}

		BigDecimal totalPaid = normalize(payment.getAmount());
		BigDecimal totalRemainingBefore = getBatchRemainingAmount(unpaidOrders);
		BigDecimal allocated = BigDecimal.ZERO;

		for (int i = 0; i < unpaidOrders.size(); i++) {
			OrderEntity order = unpaidOrders.get(i);
			BigDecimal orderRemaining = getRemainingAmount(order);
			BigDecimal portion;
			if (i == unpaidOrders.size() - 1) {
				portion = totalPaid.subtract(allocated);
			} else if (totalRemainingBefore.compareTo(BigDecimal.ZERO) > 0) {
				portion = totalPaid.multiply(orderRemaining)
						.divide(totalRemainingBefore, 2, RoundingMode.HALF_UP);
				allocated = allocated.add(portion);
			} else {
				portion = BigDecimal.ZERO;
			}
			if (portion.compareTo(BigDecimal.ZERO) <= 0) {
				continue;
			}
			if (portion.compareTo(orderRemaining) > 0) {
				portion = orderRemaining;
			}

			TransactionEntity allocation = TransactionEntity.builder()
					.order(order)
					.checkoutBatchId(payment.getCheckoutBatchId())
					.user(payment.getUser())
					.amount(portion)
					.paymentMethod(payment.getPaymentMethod())
					.transactionCode(payment.getTransactionCode() + "-O" + order.getId())
					.type(TransactionType.ORDER_PAYMENT)
					.direction(TransactionDirection.OUT)
					.description("Batch payment allocation")
					.status(TransactionStatus.SUCCESS)
					.build();
			transactionRepository.save(allocation);
			creditWorkshopEscrow(order, portion);
			updateOrderPaymentStatus(order, false);
		}

		boolean batchFullyPaid = orders.stream()
				.allMatch(order -> getRemainingAmount(order).compareTo(BigDecimal.ZERO) <= 0);
		if (batchFullyPaid) {
			orders.stream()
					.map(OrderEntity::getCouponCode)
					.filter(code -> code != null && !code.isBlank())
					.findFirst()
					.ifPresent(couponService::markCouponUsed);
		}
	}

	private void creditWorkshopEscrow(OrderEntity order, BigDecimal payable) {
		if (order.getWorkshop() == null) {
			return;
		}
		WalletEntity workshopWallet = getOrCreateWallet(order.getWorkshop());
		workshopWallet.setPendingBalance(add(workshopWallet.getPendingBalance(), payable));
		walletRepository.save(workshopWallet);

		TransactionEntity escrow = TransactionEntity.builder()
				.order(order)
				.user(order.getWorkshop())
				.amount(payable)
				.type(TransactionType.ESCROW_HOLD)
				.direction(TransactionDirection.IN)
				.description("Escrow hold")
				.status(TransactionStatus.PENDING)
				.build();
		transactionRepository.save(escrow);
	}

	private void updateOrderPaymentStatus(OrderEntity order, boolean markCoupon) {
		BigDecimal remainingAmount = getRemainingAmount(order);
		if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
			order.setPaymentStatus(PaymentStatus.PAID);
			if (markCoupon && order.getCouponCode() != null && !order.getCouponCode().isBlank()) {
				couponService.markCouponUsed(order.getCouponCode());
			}
		} else {
			order.setPaymentStatus(PaymentStatus.PARTIAL_PAID);
		}
		orderRepository.save(order);
	}

	private List<OrderEntity> getPayableBatchOrders(String checkoutBatchId, UserEntity customer) {
		List<OrderEntity> orders = orderRepository.findByCheckoutBatchIdAndCustomerId(checkoutBatchId, customer.getId());
		if (orders.isEmpty()) {
			throw new IllegalArgumentException("Checkout batch not found");
		}
		return orders.stream()
				.filter(order -> !OrderStatus.CANCELLED.equals(order.getStatus()))
				.toList();
	}

	private BigDecimal getBatchRemainingAmount(List<OrderEntity> orders) {
		return orders.stream()
				.map(this::getRemainingAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private BigDecimal resolveBatchMomoAmount(
			List<OrderEntity> orders,
			MomoCreatePaymentRequest request,
			String phase,
			BigDecimal batchRemaining) {
		if (request != null && request.amount() != null) {
			return request.amount();
		}
		if ("deposit".equals(phase)) {
			BigDecimal batchTotal = orders.stream()
					.map(order -> normalize(order.getTotalAmount()))
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal deposit = batchTotal.multiply(DEFAULT_DEPOSIT_RATE).setScale(2, RoundingMode.HALF_UP);
			if (deposit.compareTo(batchRemaining) > 0) {
				return batchRemaining;
			}
			return deposit;
		}
		return batchRemaining;
	}

	private void ensureNoPendingBatchPayment(String checkoutBatchId) {
		List<TransactionEntity> pending = transactionRepository.findByCheckoutBatchIdAndTypeAndStatus(
				checkoutBatchId,
				TransactionType.ORDER_PAYMENT,
				TransactionStatus.PENDING);
		if (!pending.isEmpty()) {
			throw new IllegalStateException("A MoMo payment session is already pending for this checkout");
		}
	}

	private String generateInternalCode(Long orderId) {
		return "PAY-" + orderId + "-" + System.currentTimeMillis();
	}

	private BigDecimal resolveMomoAmount(OrderEntity order, MomoCreatePaymentRequest request, String phase) {
		BigDecimal remaining = getRemainingAmount(order);
		if (request != null && request.amount() != null) {
			return request.amount();
		}
		if ("deposit".equals(phase)) {
			BigDecimal deposit = normalize(order.getTotalAmount())
					.multiply(DEFAULT_DEPOSIT_RATE)
					.setScale(2, RoundingMode.HALF_UP);
			if (deposit.compareTo(remaining) > 0) {
				return remaining;
			}
			return deposit;
		}
		return remaining;
	}

	private void ensureMomoConfig() {
		if (momoAccessKey == null || momoAccessKey.isBlank()
				|| momoSecretKey == null || momoSecretKey.isBlank()) {
			throw new IllegalStateException("MoMo config is missing");
		}
	}

	private boolean verifyMomoIpnSignature(Map<String, Object> payload, String signature) {
		String raw = "accessKey=" + momoAccessKey
				+ "&amount=" + stringValue(payload.get("amount"))
				+ "&extraData=" + stringValue(payload.get("extraData"))
				+ "&message=" + stringValue(payload.get("message"))
				+ "&orderId=" + stringValue(payload.get("orderId"))
				+ "&orderInfo=" + stringValue(payload.get("orderInfo"))
				+ "&orderType=" + stringValue(payload.get("orderType"))
				+ "&partnerCode=" + stringValue(payload.get("partnerCode"))
				+ "&payType=" + stringValue(payload.get("payType"))
				+ "&requestId=" + stringValue(payload.get("requestId"))
				+ "&responseTime=" + stringValue(payload.get("responseTime"))
				+ "&resultCode=" + stringValue(payload.get("resultCode"))
				+ "&transId=" + stringValue(payload.get("transId"));
		return hmacSha256(momoSecretKey, raw).equals(signature);
	}

	private String hmacSha256(String secretKey, String data) {
		try {
			Mac hmacSha256 = Mac.getInstance("HmacSHA256");
			SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
			hmacSha256.init(secretKeySpec);
			byte[] hash = hmacSha256.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (Exception ex) {
			throw new IllegalStateException("Cannot create MoMo signature", ex);
		}
	}

	private String stringValue(Object value) {
		return value == null ? "" : String.valueOf(value);
	}

	private int parseInt(Object value) {
		if (value == null) {
			return 0;
		}
		try {
			return Integer.parseInt(String.valueOf(value));
		} catch (NumberFormatException ex) {
			return 0;
		}
	}

	private WalletEntity getOrCreateWallet(UserEntity user) {
		return walletRepository.findByUserId(user.getId()).orElseGet(() -> {
			WalletEntity wallet = WalletEntity.builder()
					.user(user)
					.availableBalance(BigDecimal.ZERO)
					.pendingBalance(BigDecimal.ZERO)
					.aiTokenBalance(0)
					.build();
			return walletRepository.save(wallet);
		});
	}

	private UserEntity getCurrentUser() {
		String email = SecurityUtils.getCurrentUserEmail();
		if (email == null || email.isBlank()) {
			throw new IllegalStateException("Unauthenticated");
		}
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
	}

	private UserEntity getWorkshopUser() {
		UserEntity user = getCurrentUser();
		if (!Role.WORKSHOP.equals(user.getRole())) {
			throw new IllegalStateException("User is not a workshop");
		}
		return user;
	}

	private BankAccountResponseRecord toBankAccountResponse(BankAccountEntity entity) {
		return new BankAccountResponseRecord(
				entity.getId(),
				entity.getBankName(),
				entity.getAccountNo(),
				entity.getAccountName(),
				entity.getIsVerified());
	}

	private PayoutResponseRecord toPayoutResponse(PayoutRequestEntity entity) {
		return new PayoutResponseRecord(
				entity.getId(),
				entity.getWorkshop() != null ? entity.getWorkshop().getId() : null,
				entity.getAmount(),
				entity.getStatus() != null ? entity.getStatus().name() : null,
				entity.getAdminNote(),
				entity.getCreatedAt(),
				entity.getApprovedAt());
	}

	private WalletResponseRecord toWalletResponse(WalletEntity wallet) {
		return new WalletResponseRecord(
				wallet.getUser() != null ? wallet.getUser().getId() : null,
				wallet.getAvailableBalance(),
				wallet.getPendingBalance(),
				wallet.getAiTokenBalance());
	}

	private TransactionResponseRecord toTransactionResponse(TransactionEntity entity) {
		return new TransactionResponseRecord(
				entity.getId(),
				entity.getOrder() != null ? entity.getOrder().getId() : null,
				entity.getAmount(),
				entity.getType() != null ? entity.getType().name() : null,
				entity.getDirection() != null ? entity.getDirection().name() : null,
				entity.getStatus() != null ? entity.getStatus().name() : null,
				entity.getDescription(),
				entity.getCreatedAt());
	}

	private CommissionConfigEntity getOrCreateCommissionConfig() {
		return commissionConfigRepository.findAll().stream().findFirst()
				.orElseGet(() -> commissionConfigRepository.save(CommissionConfigEntity.builder()
						.commissionRate(DEFAULT_COMMISSION_RATE)
						.build()));
	}

	private BigDecimal calculateCommission(BigDecimal total) {
		CommissionConfigEntity config = getOrCreateCommissionConfig();
		BigDecimal rate = config.getCommissionRate() == null ? DEFAULT_COMMISSION_RATE : config.getCommissionRate();
		return total.multiply(rate);
	}

	private int safeInt(Integer value) {
		return value == null ? 0 : value;
	}

	private BigDecimal add(BigDecimal left, BigDecimal right) {
		return normalize(left).add(normalize(right));
	}

	private BigDecimal normalize(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private RevenueSummaryRecord buildRevenueSummary(
			List<TransactionEntity> transactions,
			LocalDate from,
			LocalDate to,
			String groupBy) {
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

		List<RevenueSummaryItemRecord> items = transactions.stream()
				.filter(tx -> tx.getCreatedAt() != null
						&& !tx.getCreatedAt().isBefore(fromInstant)
						&& !tx.getCreatedAt().isAfter(toInstant))
				.collect(java.util.stream.Collectors.groupingBy(tx -> buildRevenueKey(tx.getCreatedAt(), zoneId, normalizedGroup),
						java.util.stream.Collectors.reducing(
								BigDecimal.ZERO,
								tx -> normalize(tx.getAmount()),
								BigDecimal::add)))
				.entrySet().stream()
				.sorted(java.util.Map.Entry.comparingByKey())
				.map(entry -> new RevenueSummaryItemRecord(entry.getKey(), entry.getValue()))
				.toList();

		return new RevenueSummaryRecord(normalizedGroup, items);
	}

	private String buildRevenueKey(Instant createdAt, ZoneId zoneId, String groupBy) {
		if (GROUP_BY_MONTH.equals(groupBy)) {
			return YearMonth.from(createdAt.atZone(zoneId)).toString();
		}
		return createdAt.atZone(zoneId).toLocalDate().toString();
	}

	private Instant startOfDay(LocalDate date) {
		LocalDate value = date != null ? date : LocalDate.now();
		return value.atStartOfDay(ZoneId.systemDefault()).toInstant();
	}

	private Instant endOfDay(LocalDate date) {
		LocalDate value = date != null ? date : LocalDate.now();
		return value.plusDays(1).atStartOfDay(ZoneId.systemDefault()).minusNanos(1).toInstant();
	}
}
