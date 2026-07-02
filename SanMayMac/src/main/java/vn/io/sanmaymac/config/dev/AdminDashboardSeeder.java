package vn.io.sanmaymac.config.dev;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.io.sanmaymac.common.enums.OrderStatus;
import vn.io.sanmaymac.common.enums.OrderType;
import vn.io.sanmaymac.common.enums.PaymentStatus;
import vn.io.sanmaymac.common.enums.TransactionDirection;
import vn.io.sanmaymac.common.enums.TransactionStatus;
import vn.io.sanmaymac.common.enums.TransactionType;
import vn.io.sanmaymac.modules.finance.entity.TransactionEntity;
import vn.io.sanmaymac.modules.finance.repository.TransactionRepository;
import vn.io.sanmaymac.modules.order.entity.OrderDetailEntity;
import vn.io.sanmaymac.modules.order.entity.OrderEntity;
import vn.io.sanmaymac.modules.order.repository.OrderDetailRepository;
import vn.io.sanmaymac.modules.order.repository.OrderRepository;
import vn.io.sanmaymac.modules.user.entity.UserAddressEntity;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.repository.UserAddressRepository;
import vn.io.sanmaymac.modules.user.repository.UserRepository;

@Slf4j
@Component
@Profile({"docker", "local"})
@ConditionalOnProperty(name = "app.dev.seed-demo-data", havingValue = "true", matchIfMissing = true)
@Order(5)
@RequiredArgsConstructor
public class AdminDashboardSeeder implements ApplicationRunner {
    private static final String DASHBOARD_MARKER = "DEMO_DASHBOARD";

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final TransactionRepository transactionRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (hasDashboardSeed()) {
            log.info("AdminDashboardSeeder: dữ liệu dashboard đã tồn tại — bỏ qua.");
            return;
        }

        UserEntity customer = userRepository.findByEmail("customer@sanmaymac.vn").orElse(null);
        UserEntity workshop = userRepository.findByEmail("workshop@sanmaymac.vn").orElse(null);
        UserEntity workshop2 = userRepository.findByEmail("workshop2@sanmaymac.vn").orElse(null);
        UserEntity workshopLegacy = userRepository.findByEmail("workshop1@sanmaymac.local").orElse(null);

        if (customer == null || workshop == null) {
            log.warn("AdminDashboardSeeder: thiếu customer@ hoặc workshop@ — bỏ qua.");
            return;
        }

        UserAddressEntity address = userAddressRepository.findByUserId(customer.getId()).stream()
                .findFirst()
                .orElse(null);
        if (address == null) {
            log.warn("AdminDashboardSeeder: customer chưa có địa chỉ — bỏ qua.");
            return;
        }

        List<UserEntity> workshops = new ArrayList<>();
        workshops.add(workshop);
        if (workshop2 != null) {
            workshops.add(workshop2);
        }
        if (workshopLegacy != null) {
            workshops.add(workshopLegacy);
        }

        // Doanh thu theo tháng (6 tháng gần nhất) — tỷ lệ ~45% / 30% / 25%
        long[][] monthlyTotals = {
                {4_200_000L, 2_800_000L, 1_500_000L},
                {5_100_000L, 3_200_000L, 2_100_000L},
                {6_800_000L, 4_500_000L, 2_800_000L},
                {7_200_000L, 5_000_000L, 3_100_000L},
                {8_500_000L, 5_500_000L, 3_400_000L},
                {9_200_000L, 6_100_000L, 3_900_000L},
        };

        int created = 0;
        LocalDate today = LocalDate.now();
        for (int monthOffset = 5; monthOffset >= 0; monthOffset--) {
            LocalDate monthDate = today.minusMonths(monthOffset).withDayOfMonth(15);
            int dataIndex = 5 - monthOffset;

            for (int w = 0; w < workshops.size(); w++) {
                UserEntity ws = workshops.get(w);
                long monthTotal = monthlyTotals[dataIndex][Math.min(w, monthlyTotals[dataIndex].length - 1)];
                long firstOrder = Math.round(monthTotal * 0.55);
                long secondOrder = monthTotal - firstOrder;

                created += seedOrder(customer, ws, address, firstOrder, monthDate.minusDays(10));
                created += seedOrder(customer, ws, address, secondOrder, monthDate.minusDays(3));
            }
        }

        log.info("AdminDashboardSeeder: đã seed {} đơn hàng demo cho biểu đồ admin dashboard.", created);
    }

    private boolean hasDashboardSeed() {
        return orderRepository.findAll().stream()
                .anyMatch(order -> order.getCustomerNote() != null && order.getCustomerNote().contains(DASHBOARD_MARKER));
    }

    private int seedOrder(
            UserEntity customer,
            UserEntity workshop,
            UserAddressEntity address,
            long amount,
            LocalDate orderDate) {
        BigDecimal total = BigDecimal.valueOf(amount);
        BigDecimal shipping = BigDecimal.valueOf(30_000);

        OrderEntity order = orderRepository.save(OrderEntity.builder()
                .customer(customer)
                .workshop(workshop)
                .address(address)
                .orderType(OrderType.READY_MADE)
                .status(OrderStatus.COMPLETED)
                .paymentStatus(PaymentStatus.PAID)
                .totalAmount(total.add(shipping))
                .shippingFee(shipping)
                .discountAmount(BigDecimal.ZERO)
                .checkoutBatchId("DEMO-DASH-" + UUID.randomUUID().toString().substring(0, 8))
                .trackingCode("GHN-DASH-" + UUID.randomUUID().toString().substring(0, 6))
                .customerNote(DASHBOARD_MARKER + " — đơn demo biểu đồ admin")
                .build());

        orderDetailRepository.save(OrderDetailEntity.builder()
                .order(order)
                .productName("[DEMO] Sản phẩm dashboard")
                .quantity(1)
                .unitPrice(total)
                .build());

        backdateOrder(order.getId(), orderDate);

        TransactionEntity release = transactionRepository.save(TransactionEntity.builder()
                .user(workshop)
                .order(order)
                .amount(total.multiply(BigDecimal.valueOf(0.9)))
                .type(TransactionType.ESCROW_RELEASE)
                .direction(TransactionDirection.IN)
                .status(TransactionStatus.SUCCESS)
                .description(DASHBOARD_MARKER + " ESCROW_RELEASE")
                .transactionCode("DASH-REL-" + UUID.randomUUID().toString().substring(0, 8))
                .paymentMethod("WALLET")
                .build());
        backdateTransaction(release.getId(), orderDate.plusDays(2));

        TransactionEntity commission = transactionRepository.save(TransactionEntity.builder()
                .user(workshop)
                .order(order)
                .amount(total.multiply(BigDecimal.valueOf(0.1)))
                .type(TransactionType.COMMISSION_FEE)
                .direction(TransactionDirection.OUT)
                .status(TransactionStatus.SUCCESS)
                .description(DASHBOARD_MARKER + " COMMISSION_FEE")
                .transactionCode("DASH-COM-" + UUID.randomUUID().toString().substring(0, 8))
                .paymentMethod("WALLET")
                .build());
        backdateTransaction(commission.getId(), orderDate.plusDays(2));

        return 1;
    }

    private void backdateOrder(Long orderId, LocalDate date) {
        Instant createdAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        entityManager.createNativeQuery("UPDATE orders SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", orderId)
                .executeUpdate();
    }

    private void backdateTransaction(Long transactionId, LocalDate date) {
        Instant createdAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        entityManager.createNativeQuery("UPDATE transactions SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", transactionId)
                .executeUpdate();
    }
}
