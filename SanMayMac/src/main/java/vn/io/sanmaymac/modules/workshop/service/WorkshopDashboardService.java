package vn.io.sanmaymac.modules.workshop.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.io.sanmaymac.common.enums.OrderStatus;
import vn.io.sanmaymac.common.enums.Role;
import vn.io.sanmaymac.common.utils.SecurityUtils;
import vn.io.sanmaymac.modules.finance.dto.RevenueSummaryRecord;
import vn.io.sanmaymac.modules.finance.dto.WalletResponseRecord;
import vn.io.sanmaymac.modules.finance.repository.PayoutRequestRepository;
import vn.io.sanmaymac.modules.finance.service.FinanceService;
import vn.io.sanmaymac.modules.notification.repository.WorkshopNotificationRepository;
import vn.io.sanmaymac.modules.order.dto.OrderStatsResponseRecord;
import vn.io.sanmaymac.modules.order.repository.OrderRepository;
import vn.io.sanmaymac.modules.order.service.OrderService;
import vn.io.sanmaymac.modules.review.service.ReviewService;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.repository.UserRepository;
import vn.io.sanmaymac.modules.workshop.dto.WorkshopDashboardSummaryResponseRecord;
import vn.io.sanmaymac.modules.workshop.dto.WorkshopStatsResponseRecord;

@Service
@Transactional(readOnly = true)
public class WorkshopDashboardService {
    private final FinanceService financeService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final ReviewService reviewService;
    private final WorkshopNotificationRepository notificationRepository;
    private final PayoutRequestRepository payoutRequestRepository;
    private final UserRepository userRepository;

    public WorkshopDashboardService(
            FinanceService financeService,
            OrderService orderService,
            OrderRepository orderRepository,
            ReviewService reviewService,
            WorkshopNotificationRepository notificationRepository,
            PayoutRequestRepository payoutRequestRepository,
            UserRepository userRepository) {
        this.financeService = financeService;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.reviewService = reviewService;
        this.notificationRepository = notificationRepository;
        this.payoutRequestRepository = payoutRequestRepository;
        this.userRepository = userRepository;
    }

    public WorkshopDashboardSummaryResponseRecord getSummary() {
        UserEntity workshop = getCurrentWorkshop();
        WalletResponseRecord wallet = financeService.getMyWallet();
        RevenueSummaryRecord revenue = financeService.getWorkshopRevenue(null, null, "month");
        BigDecimal totalRevenue = revenue.items() == null
                ? BigDecimal.ZERO
                : revenue.items().stream()
                        .map(item -> item.totalRevenue() == null ? BigDecimal.ZERO : item.totalRevenue())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = orderRepository.findByWorkshopId(workshop.getId()).size();
        long pendingOrders = orderRepository.findByWorkshopIdAndStatus(workshop.getId(), OrderStatus.PENDING).size();
        long pendingPayouts = payoutRequestRepository.findByWorkshopId(workshop.getId()).stream()
                .filter(payout -> payout.getStatus() != null && payout.getStatus().name().equals("PENDING"))
                .count();
        var reviewSummary = reviewService.getWorkshopSummary(workshop.getId());
        double ratingAvg = reviewSummary.averageRating();
        long reviewCount = reviewSummary.totalReviews();
        long unreadNotifications = notificationRepository.countByRecipientIdAndReadAtIsNull(workshop.getId());

        return new WorkshopDashboardSummaryResponseRecord(
                wallet.availableBalance(),
                wallet.pendingBalance(),
                wallet.aiTokenBalance(),
                totalRevenue,
                totalOrders,
                pendingOrders,
                pendingPayouts,
                ratingAvg,
                reviewCount,
                unreadNotifications);
    }

    public WorkshopStatsResponseRecord getStats(java.time.LocalDate from, java.time.LocalDate to, String groupBy) {
        return new WorkshopStatsResponseRecord(
                orderService.getOrderStats(from, to, groupBy),
                financeService.getWorkshopRevenue(from, to, groupBy));
    }

    private UserEntity getCurrentWorkshop() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Unauthenticated");
        }
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!Role.WORKSHOP.equals(user.getRole())) {
            throw new IllegalStateException("User is not a workshop");
        }
        return user;
    }
}