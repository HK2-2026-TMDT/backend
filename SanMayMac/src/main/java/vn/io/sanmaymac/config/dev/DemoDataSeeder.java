package vn.io.sanmaymac.config.dev;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.io.sanmaymac.common.enums.OrderStatus;
import vn.io.sanmaymac.common.enums.OrderType;
import vn.io.sanmaymac.common.enums.PaymentStatus;
import vn.io.sanmaymac.common.enums.PayoutStatus;
import vn.io.sanmaymac.common.enums.PostStatus;
import vn.io.sanmaymac.common.enums.ProductApprovalStatus;
import vn.io.sanmaymac.common.enums.QuoteStatus;
import vn.io.sanmaymac.common.enums.ReviewStatus;
import vn.io.sanmaymac.common.enums.TransactionDirection;
import vn.io.sanmaymac.common.enums.TransactionStatus;
import vn.io.sanmaymac.common.enums.TransactionType;
import vn.io.sanmaymac.modules.bidding.entity.BiddingPostEntity;
import vn.io.sanmaymac.modules.bidding.entity.QuoteEntity;
import vn.io.sanmaymac.modules.bidding.repository.BiddingPostRepository;
import vn.io.sanmaymac.modules.bidding.repository.QuoteRepository;
import vn.io.sanmaymac.modules.catalog.entity.CategoryEntity;
import vn.io.sanmaymac.modules.catalog.entity.ProductEntity;
import vn.io.sanmaymac.modules.catalog.entity.ProductImageEntity;
import vn.io.sanmaymac.modules.catalog.entity.ProductVariantEntity;
import vn.io.sanmaymac.modules.catalog.repository.CategoryRepository;
import vn.io.sanmaymac.modules.catalog.repository.ProductImageRepository;
import vn.io.sanmaymac.modules.catalog.repository.ProductRepository;
import vn.io.sanmaymac.modules.catalog.repository.ProductVariantRepository;
import vn.io.sanmaymac.modules.finance.entity.BankAccountEntity;
import vn.io.sanmaymac.modules.finance.entity.PayoutRequestEntity;
import vn.io.sanmaymac.modules.finance.entity.TransactionEntity;
import vn.io.sanmaymac.modules.finance.entity.WalletEntity;
import vn.io.sanmaymac.modules.finance.repository.BankAccountRepository;
import vn.io.sanmaymac.modules.finance.repository.PayoutRequestRepository;
import vn.io.sanmaymac.modules.finance.repository.TransactionRepository;
import vn.io.sanmaymac.modules.finance.repository.WalletRepository;
import vn.io.sanmaymac.modules.order.entity.OrderDetailEntity;
import vn.io.sanmaymac.modules.order.entity.OrderEntity;
import vn.io.sanmaymac.modules.order.repository.OrderDetailRepository;
import vn.io.sanmaymac.modules.order.repository.OrderRepository;
import vn.io.sanmaymac.modules.review.entity.ReviewEntity;
import vn.io.sanmaymac.modules.review.entity.ReviewImageEntity;
import vn.io.sanmaymac.modules.review.entity.ReviewReplyEntity;
import vn.io.sanmaymac.modules.review.repository.ReviewImageRepository;
import vn.io.sanmaymac.modules.review.repository.ReviewReplyRepository;
import vn.io.sanmaymac.modules.review.repository.ReviewRepository;
import vn.io.sanmaymac.modules.user.entity.UserAddressEntity;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.entity.WorkshopProfileEntity;
import vn.io.sanmaymac.modules.user.repository.UserAddressRepository;
import vn.io.sanmaymac.modules.user.repository.UserRepository;
import vn.io.sanmaymac.modules.user.repository.WorkshopProfileRepository;

@Slf4j
@Component
@Profile({"docker", "local"})
@ConditionalOnProperty(name = "app.dev.seed-demo-data", havingValue = "true", matchIfMissing = true)
@Order(2)
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {
    private static final String DEMO_PREFIX = "[DEMO] ";
    private static final String DEMO_MARKER = "DEMO_SEED_V2";

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final WorkshopProfileRepository workshopProfileRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final BiddingPostRepository biddingPostRepository;
    private final QuoteRepository quoteRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final WalletRepository walletRepository;
    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final PayoutRequestRepository payoutRequestRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewReplyRepository reviewReplyRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        UserEntity workshop = userRepository.findByEmail("workshop@sanmaymac.vn").orElse(null);
        UserEntity workshop2 = userRepository.findByEmail("workshop2@sanmaymac.vn").orElse(null);
        UserEntity customer = userRepository.findByEmail("customer@sanmaymac.vn").orElse(null);

        if (workshop == null || customer == null) {
            log.warn("DemoDataSeeder: thiếu tài khoản workshop@ hoặc customer@ — bỏ qua seed demo.");
            return;
        }

        if (alreadySeeded(workshop.getId())) {
            log.info("DemoDataSeeder: dữ liệu demo đã tồn tại — bỏ qua.");
            return;
        }

        UserAddressEntity address = ensureCustomerAddress(customer);
        CategoryEntity category = categoryRepository.findAll().stream().findFirst().orElse(null);
        if (category == null) {
            log.warn("DemoDataSeeder: không có danh mục — bỏ qua.");
            return;
        }

        boolean hasDemoProducts = hasDemoProducts(workshop.getId());
        ProductBundle catalog;
        List<OrderEntity> completedOrders;

        if (!hasDemoProducts) {
            catalog = seedCatalog(workshop, category);
            seedBidding(customer, workshop, workshop2);
            completedOrders = seedOrders(customer, workshop, address, catalog);
        } else {
            log.info("DemoDataSeeder: bổ sung dữ liệu demo còn thiếu…");
            catalog = loadExistingCatalog(workshop);
            completedOrders = orderRepository.findByWorkshopId(workshop.getId()).stream()
                    .filter(order -> order.getCustomerNote() != null && order.getCustomerNote().contains(DEMO_MARKER))
                    .filter(order -> OrderStatus.COMPLETED.equals(order.getStatus()))
                    .toList();
        }

        if (!hasDemoFinance(workshop.getId())) {
            seedFinance(workshop, completedOrders);
        }
        if (!hasDemoReviews(workshop.getId())) {
            seedReviews(customer, workshop, catalog, completedOrders);
            updateWorkshopRating(workshop);
        }

        log.info("""
                ============================================================
                DỮ LIỆU DEMO ĐÃ SẴN SÀNG
                  Workshop  | workshop@sanmaymac.vn  | sản phẩm, đơn hàng, tài chính, đánh giá
                  Customer  | customer@sanmaymac.vn  | đơn hàng, đấu thầu, đánh giá
                  Workshop2 | workshop2@sanmaymac.vn | báo giá cạnh tranh
                ============================================================""");
    }

    private boolean alreadySeeded(Long workshopId) {
        return hasDemoProducts(workshopId) && hasDemoFinance(workshopId) && hasDemoReviews(workshopId);
    }

    private boolean hasDemoProducts(Long workshopId) {
        return productRepository.findByWorkshopId(workshopId, Pageable.ofSize(50)).getContent().stream()
                .anyMatch(product -> product.getName() != null && product.getName().startsWith(DEMO_PREFIX));
    }

    private boolean hasDemoFinance(Long workshopId) {
        return transactionRepository.findByUserId(workshopId).stream()
                .anyMatch(tx -> tx.getDescription() != null && tx.getDescription().contains(DEMO_MARKER));
    }

    private boolean hasDemoReviews(Long workshopId) {
        return reviewRepository.findAll().stream()
                .anyMatch(review -> review.getWorkshop() != null
                        && review.getWorkshop().getId().equals(workshopId)
                        && review.getComment() != null
                        && review.getComment().contains("hài lòng"));
    }

    private ProductBundle loadExistingCatalog(UserEntity workshop) {
        List<ProductEntity> products = productRepository.findByWorkshopId(workshop.getId(), Pageable.ofSize(20))
                .getContent()
                .stream()
                .filter(product -> product.getName() != null && product.getName().startsWith(DEMO_PREFIX))
                .toList();
        ProductEntity shirt = products.stream().filter(p -> p.getName().contains("thun")).findFirst().orElse(products.get(0));
        ProductEntity polo = products.stream().filter(p -> p.getName().contains("Polo")).findFirst().orElse(shirt);
        ProductEntity hoodie = products.stream().filter(p -> p.getName().contains("Hoodie")).findFirst().orElse(shirt);
        return new ProductBundle(
                shirt,
                productVariantRepository.findAll().stream()
                        .filter(v -> v.getProduct().getId().equals(shirt.getId()))
                        .findFirst()
                        .orElseThrow(),
                polo,
                productVariantRepository.findAll().stream()
                        .filter(v -> v.getProduct().getId().equals(polo.getId()))
                        .findFirst()
                        .orElseThrow(),
                hoodie,
                productVariantRepository.findAll().stream()
                        .filter(v -> v.getProduct().getId().equals(hoodie.getId()))
                        .findFirst()
                        .orElseThrow());
    }

    private UserAddressEntity ensureCustomerAddress(UserEntity customer) {
        return userAddressRepository.findByUserId(customer.getId()).stream()
                .findFirst()
                .orElseGet(() -> userAddressRepository.save(UserAddressEntity.builder()
                        .user(customer)
                        .receiverName(customer.getFullName())
                        .phone(customer.getPhoneNumber())
                        .detailedAddress("123 Nguyễn Huệ")
                        .provinceName("TP. Hồ Chí Minh")
                        .districtName("Quận 1")
                        .wardName("Phường Bến Nghé")
                        .provinceId(79)
                        .districtId(760)
                        .wardCode("26734")
                        .isDefault(true)
                        .build()));
    }

    private ProductBundle seedCatalog(UserEntity workshop, CategoryEntity category) {
        ProductEntity shirt = createProduct(workshop, category, "Áo thun Basic Cotton", "250000",
                "Áo thun cotton 100%, form regular fit.");
        ProductVariantEntity shirtWhiteM = createVariant(shirt, "DEMO-SHIRT-W-M", "Trắng", "M", "250000", 120);
        ProductVariantEntity shirtBlackL = createVariant(shirt, "DEMO-SHIRT-B-L", "Đen", "L", "260000", 80);
        ProductVariantEntity shirtNavyS = createVariant(shirt, "DEMO-SHIRT-N-S", "Navy", "S", "255000", 60);
        createImage(shirt, "https://picsum.photos/seed/demo-shirt-1/600/600", true, 0);
        createImage(shirt, "https://picsum.photos/seed/demo-shirt-2/600/600", false, 1);

        ProductEntity polo = createProduct(workshop, category, "Polo Premium", "320000",
                "Áo polo co giãn, logo thêu tùy chọn.");
        ProductVariantEntity poloRedL = createVariant(polo, "DEMO-POLO-R-L", "Đỏ", "L", "320000", 50);
        ProductVariantEntity poloWhiteM = createVariant(polo, "DEMO-POLO-W-M", "Trắng", "M", "315000", 45);
        createImage(polo, "https://picsum.photos/seed/demo-polo-1/600/600", true, 0);

        ProductEntity hoodie = createProduct(workshop, category, "Hoodie Unisex", "450000",
                "Hoodie nỉ bông dày, phù hợp mùa lạnh.");
        ProductVariantEntity hoodieGrayL = createVariant(hoodie, "DEMO-HOOD-G-L", "Xám", "L", "450000", 30);
        ProductVariantEntity hoodieBlackXL = createVariant(hoodie, "DEMO-HOOD-B-XL", "Đen", "XL", "470000", 25);
        createImage(hoodie, "https://picsum.photos/seed/demo-hoodie-1/600/600", true, 0);
        createImage(hoodie, "https://picsum.photos/seed/demo-hoodie-2/600/600", false, 1);

        return new ProductBundle(shirt, shirtWhiteM, polo, poloRedL, hoodie, hoodieGrayL);
    }

    private ProductEntity createProduct(
            UserEntity workshop, CategoryEntity category, String name, String basePrice, String description) {
        return productRepository.save(ProductEntity.builder()
                .workshop(workshop)
                .category(category)
                .name(DEMO_PREFIX + name)
                .basePrice(bd(basePrice))
                .description(description)
                .isVisible(true)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build());
    }

    private ProductVariantEntity createVariant(
            ProductEntity product, String sku, String color, String size, String price, int stock) {
        return productVariantRepository.save(ProductVariantEntity.builder()
                .product(product)
                .skuCode(sku)
                .color(color)
                .size(size)
                .price(bd(price))
                .stockQuantity(stock)
                .build());
    }

    private void createImage(ProductEntity product, String url, boolean thumbnail, int sortOrder) {
        productImageRepository.save(ProductImageEntity.builder()
                .product(product)
                .imageUrl(url)
                .isThumbnail(thumbnail)
                .sortOrder(sortOrder)
                .build());
    }

    private void seedBidding(UserEntity customer, UserEntity workshop, UserEntity workshop2) {
        BiddingPostEntity postUniform = biddingPostRepository.save(BiddingPostEntity.builder()
                .customer(customer)
                .title(DEMO_PREFIX + "May áo thun đồng phục công ty")
                .description("Cần may 200 áo thun đồng phục, in logo 2 mặt, giao trong 14 ngày.")
                .status(PostStatus.OPEN)
                .frontDesignUrl("https://picsum.photos/seed/demo-bid-front-1/500/500")
                .backDesignUrl("https://picsum.photos/seed/demo-bid-back-1/500/500")
                .build());

        BiddingPostEntity postJacket = biddingPostRepository.save(BiddingPostEntity.builder()
                .customer(customer)
                .title(DEMO_PREFIX + "Gia công áo khoác dạ")
                .description("May 50 áo khoác dạ theo mẫu, chất liệu cao cấp.")
                .status(PostStatus.OPEN)
                .aiImageUrl("https://picsum.photos/seed/demo-bid-jacket/500/500")
                .build());

        BiddingPostEntity postVest = biddingPostRepository.save(BiddingPostEntity.builder()
                .customer(customer)
                .title(DEMO_PREFIX + "May vest cưới")
                .description("Đơn may vest đã chốt xưởng.")
                .status(PostStatus.CLOSED)
                .build());

        quoteRepository.save(QuoteEntity.builder()
                .post(postUniform)
                .workshop(workshop)
                .offeredPrice(bd("45000000"))
                .estimateDays(12)
                .status(QuoteStatus.PENDING)
                .build());

        if (workshop2 != null) {
            quoteRepository.save(QuoteEntity.builder()
                    .post(postUniform)
                    .workshop(workshop2)
                    .offeredPrice(bd("47000000"))
                    .estimateDays(10)
                    .status(QuoteStatus.PENDING)
                    .build());
        }

        quoteRepository.save(QuoteEntity.builder()
                .post(postJacket)
                .workshop(workshop)
                .offeredPrice(bd("85000000"))
                .estimateDays(20)
                .status(QuoteStatus.PENDING)
                .build());

        quoteRepository.save(QuoteEntity.builder()
                .post(postVest)
                .workshop(workshop)
                .offeredPrice(bd("12000000"))
                .estimateDays(7)
                .status(QuoteStatus.ACCEPTED)
                .build());
    }

    private List<OrderEntity> seedOrders(
            UserEntity customer, UserEntity workshop, UserAddressEntity address, ProductBundle catalog) {
        List<OrderEntity> completedOrders = new ArrayList<>();

        completedOrders.add(createReadyMadeOrder(customer, workshop, address, catalog.shirtVariant(),
                catalog.shirt().getName(), OrderStatus.COMPLETED, PaymentStatus.PAID, "520000", "30000", "GHN-DEMO-001", null));
        createReadyMadeOrder(customer, workshop, address, catalog.poloVariant(), catalog.polo().getName(),
                OrderStatus.SHIPPED, PaymentStatus.PAID, "650000", "35000", "GHN-DEMO-002", null);
        createReadyMadeOrder(customer, workshop, address, catalog.hoodieVariant(), catalog.hoodie().getName(),
                OrderStatus.PRODUCING, PaymentStatus.PAID, "950000", "40000", null, null);
        createReadyMadeOrder(customer, workshop, address, catalog.shirtVariant(), catalog.shirt().getName(),
                OrderStatus.DEPOSITED, PaymentStatus.PARTIAL_PAID, "500000", "30000", null, null);
        createReadyMadeOrder(customer, workshop, address, catalog.poloVariant(), catalog.polo().getName(),
                OrderStatus.PENDING, PaymentStatus.PAID, "640000", "30000", null, null);

        QuoteEntity customQuote = quoteRepository.findAll().stream()
                .filter(quote -> quote.getWorkshop() != null
                        && quote.getWorkshop().getId().equals(workshop.getId())
                        && QuoteStatus.ACCEPTED.equals(quote.getStatus()))
                .findFirst()
                .orElse(null);

        completedOrders.add(createCustomOrder(customer, workshop, address, customQuote,
                OrderStatus.COMPLETED, PaymentStatus.PAID, "12500000", "500000",
                "https://picsum.photos/seed/demo-order-front/500/500",
                "https://picsum.photos/seed/demo-order-back/500/500", "GHN-DEMO-C01"));
        createCustomOrder(customer, workshop, address, null,
                OrderStatus.SHIPPED, PaymentStatus.PAID, "9800000", "450000",
                "https://picsum.photos/seed/demo-order-front-2/500/500", null, "GHN-DEMO-C02");
        createCustomOrder(customer, workshop, address, null,
                OrderStatus.PRODUCING, PaymentStatus.PAID, "7600000", "400000",
                "https://picsum.photos/seed/demo-order-front-3/500/500",
                "https://picsum.photos/seed/demo-order-back-3/500/500", null);
        createCustomOrder(customer, workshop, address, null,
                OrderStatus.PENDING, PaymentStatus.PAID, "5400000", "350000", null, null, null);

        return completedOrders;
    }

    private OrderEntity createReadyMadeOrder(
            UserEntity customer,
            UserEntity workshop,
            UserAddressEntity address,
            ProductVariantEntity variant,
            String productName,
            OrderStatus status,
            PaymentStatus paymentStatus,
            String lineTotal,
            String shippingFee,
            String trackingCode,
            String batchId) {
        BigDecimal total = bd(lineTotal).add(bd(shippingFee));
        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .customer(customer)
                .workshop(workshop)
                .address(address)
                .orderType(OrderType.READY_MADE)
                .status(status)
                .paymentStatus(paymentStatus)
                .totalAmount(total)
                .shippingFee(bd(shippingFee))
                .discountAmount(BigDecimal.ZERO)
                .checkoutBatchId(batchId != null ? batchId : "DEMO-BATCH-" + UUID.randomUUID().toString().substring(0, 8))
                .trackingCode(trackingCode)
                .customerNote(DEMO_MARKER)
                .build());

        orderDetailRepository.save(OrderDetailEntity.builder()
                .order(order)
                .variant(variant)
                .productName(productName)
                .quantity(2)
                .unitPrice(variant.getPrice())
                .build());

        return order;
    }

    private OrderEntity createCustomOrder(
            UserEntity customer,
            UserEntity workshop,
            UserAddressEntity address,
            QuoteEntity quote,
            OrderStatus status,
            PaymentStatus paymentStatus,
            String lineTotal,
            String shippingFee,
            String frontDesign,
            String backDesign,
            String trackingCode) {
        BigDecimal total = bd(lineTotal).add(bd(shippingFee));
        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .customer(customer)
                .workshop(workshop)
                .address(address)
                .quote(quote)
                .orderType(OrderType.CUSTOM)
                .status(status)
                .paymentStatus(paymentStatus)
                .totalAmount(total)
                .shippingFee(bd(shippingFee))
                .discountAmount(BigDecimal.ZERO)
                .frontDesignUrl(frontDesign)
                .backDesignUrl(backDesign)
                .trackingCode(trackingCode)
                .customerNote(DEMO_MARKER + " — đơn gia công")
                .build());

        orderDetailRepository.save(OrderDetailEntity.builder()
                .order(order)
                .productName(DEMO_PREFIX + "Gia công theo yêu cầu")
                .quantity(1)
                .unitPrice(bd(lineTotal))
                .build());

        return order;
    }

    private void seedFinance(UserEntity workshop, List<OrderEntity> completedOrders) {
        OrderEntity financeOrder = completedOrders.isEmpty()
                ? orderRepository.findByWorkshopId(workshop.getId()).stream().findFirst().orElse(null)
                : completedOrders.get(0);
        if (financeOrder == null) {
            log.warn("DemoDataSeeder: không có đơn hàng để gắn giao dịch tài chính.");
            return;
        }

        walletRepository.findByUserId(workshop.getId()).ifPresentOrElse(wallet -> {
            wallet.setAvailableBalance(bd("15000000"));
            wallet.setPendingBalance(bd("3500000"));
            wallet.setAiTokenBalance(100);
            walletRepository.save(wallet);
        }, () -> walletRepository.save(WalletEntity.builder()
                .user(workshop)
                .availableBalance(bd("15000000"))
                .pendingBalance(bd("3500000"))
                .aiTokenBalance(100)
                .build()));

        bankAccountRepository.findByUserId(workshop.getId()).ifPresentOrElse(account -> {
            account.setBankName("Vietcombank");
            account.setAccountNo("0123456789");
            account.setAccountName("XUONG MAY DEMO");
            account.setIsVerified(true);
            bankAccountRepository.save(account);
        }, () -> bankAccountRepository.save(BankAccountEntity.builder()
                .user(workshop)
                .bankName("Vietcombank")
                .accountNo("0123456789")
                .accountName("XUONG MAY DEMO")
                .isVerified(true)
                .build()));

        createFinanceTransaction(workshop, financeOrder, TransactionType.ESCROW_RELEASE, TransactionDirection.IN, "5000000", 25);
        createFinanceTransaction(workshop, financeOrder, TransactionType.ESCROW_RELEASE, TransactionDirection.IN, "3000000", 18);
        createFinanceTransaction(workshop, financeOrder, TransactionType.ESCROW_RELEASE, TransactionDirection.IN, "4500000", 10);
        createFinanceTransaction(workshop, financeOrder, TransactionType.ESCROW_RELEASE, TransactionDirection.IN, "2000000", 3);
        createFinanceTransaction(workshop, financeOrder, TransactionType.ESCROW_HOLD, TransactionDirection.IN, "3500000", 5);
        createFinanceTransaction(workshop, financeOrder, TransactionType.COMMISSION_FEE, TransactionDirection.OUT, "450000", 10);
        createFinanceTransaction(workshop, financeOrder, TransactionType.PAYOUT, TransactionDirection.OUT, "2000000", 15);

        payoutRequestRepository.findByWorkshopId(workshop.getId()).stream().findAny().ifPresentOrElse(
                existing -> log.debug("Demo payouts already exist"),
                () -> {
                    payoutRequestRepository.save(PayoutRequestEntity.builder()
                            .workshop(workshop)
                            .amount(bd("1500000"))
                            .status(PayoutStatus.PENDING)
                            .build());
                    payoutRequestRepository.save(PayoutRequestEntity.builder()
                            .workshop(workshop)
                            .amount(bd("3000000"))
                            .status(PayoutStatus.APPROVED)
                            .approvedAt(Instant.now().minus(7, ChronoUnit.DAYS))
                            .adminNote("Đã chuyển khoản")
                            .build());
                    payoutRequestRepository.save(PayoutRequestEntity.builder()
                            .workshop(workshop)
                            .amount(bd("1000000"))
                            .status(PayoutStatus.REJECTED)
                            .adminNote("Thông tin tài khoản chưa khớp")
                            .build());
                });
    }

    private void createFinanceTransaction(
            UserEntity workshop,
            OrderEntity order,
            TransactionType type,
            TransactionDirection direction,
            String amount,
            int daysAgo) {
        if (order == null) {
            return;
        }
        TransactionEntity tx = transactionRepository.save(TransactionEntity.builder()
                .user(workshop)
                .order(order)
                .amount(bd(amount))
                .type(type)
                .direction(direction)
                .status(TransactionStatus.SUCCESS)
                .description(DEMO_MARKER + " " + type.name())
                .transactionCode("DEMO-" + type.name() + "-" + UUID.randomUUID().toString().substring(0, 8))
                .paymentMethod("WALLET")
                .build());
        backdate(tx.getId(), daysAgo);
    }

    private void backdate(Long transactionId, int daysAgo) {
        Instant createdAt = LocalDate.now().minusDays(daysAgo).atStartOfDay(ZoneId.systemDefault()).toInstant();
        entityManager.createNativeQuery("UPDATE transactions SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", transactionId)
                .executeUpdate();
    }

    private void seedReviews(
            UserEntity customer,
            UserEntity workshop,
            ProductBundle catalog,
            List<OrderEntity> completedOrders) {
        if (completedOrders.isEmpty()) {
            return;
        }

        OrderEntity order1 = completedOrders.get(0);
        ReviewEntity review1 = reviewRepository.save(ReviewEntity.builder()
                .order(order1)
                .user(customer)
                .workshop(workshop)
                .product(catalog.shirt())
                .rating(5)
                .comment("Áo đẹp, form chuẩn, giao hàng nhanh. Rất hài lòng!")
                .status(ReviewStatus.ACTIVE)
                .build());
        reviewReplyRepository.save(ReviewReplyEntity.builder()
                .review(review1)
                .workshop(workshop)
                .content("Cảm ơn bạn đã tin tưởng Xưởng May Demo. Rất mong được phục vụ thêm!")
                .build());
        reviewImageRepository.save(ReviewImageEntity.builder()
                .review(review1)
                .imageUrl("https://picsum.photos/seed/demo-review-1/400/400")
                .build());

        if (completedOrders.size() > 1) {
            OrderEntity order2 = completedOrders.get(1);
            ReviewEntity review2 = reviewRepository.save(ReviewEntity.builder()
                    .order(order2)
                    .user(customer)
                    .workshop(workshop)
                    .product(catalog.polo())
                    .rating(4)
                    .comment("Chất lượng tốt, chỉ hơi chậm giao hàng một chút.")
                    .status(ReviewStatus.ACTIVE)
                    .build());
            reviewImageRepository.save(ReviewImageEntity.builder()
                    .review(review2)
                    .imageUrl("https://picsum.photos/seed/demo-review-2/400/400")
                    .build());
        }
    }

    private void updateWorkshopRating(UserEntity workshop) {
        workshopProfileRepository.findById(workshop.getId()).ifPresent(profile -> {
            profile.setRatingAvg(4.3);
            workshopProfileRepository.save(profile);
        });
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private record ProductBundle(
            ProductEntity shirt,
            ProductVariantEntity shirtVariant,
            ProductEntity polo,
            ProductVariantEntity poloVariant,
            ProductEntity hoodie,
            ProductVariantEntity hoodieVariant) {
    }
}
