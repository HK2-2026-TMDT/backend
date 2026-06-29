package vn.io.sanmaymac.modules.workshop.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import vn.io.sanmaymac.common.dto.ApiResponse;
import vn.io.sanmaymac.modules.finance.dto.BankAccountRequest;
import vn.io.sanmaymac.modules.finance.dto.BankAccountResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.PayoutResponseRecord;
import vn.io.sanmaymac.modules.message.dto.WorkshopMessageThreadDetailResponseRecord;
import vn.io.sanmaymac.modules.message.dto.WorkshopMessageThreadResponseRecord;
import vn.io.sanmaymac.modules.message.service.WorkshopMessageService;
import vn.io.sanmaymac.modules.notification.dto.WorkshopNotificationResponseRecord;
import vn.io.sanmaymac.modules.notification.service.WorkshopNotificationService;
import vn.io.sanmaymac.modules.order.dto.OrderDetailResponseRecord;
import vn.io.sanmaymac.modules.order.dto.OrderTimelineResponseRecord;
import vn.io.sanmaymac.modules.review.dto.ReviewResponseRecord;
import vn.io.sanmaymac.modules.review.dto.ReviewSummaryRecord;
import vn.io.sanmaymac.modules.review.service.ReviewService;
import vn.io.sanmaymac.modules.user.dto.PortfolioItemRequest;
import vn.io.sanmaymac.modules.user.dto.PortfolioItemResponseRecord;
import vn.io.sanmaymac.modules.user.dto.ReputationResponseRecord;
import vn.io.sanmaymac.modules.user.dto.WorkshopKycRequest;
import vn.io.sanmaymac.modules.user.dto.WorkshopKycResponseRecord;
import vn.io.sanmaymac.modules.user.dto.WorkshopProfileRequest;
import vn.io.sanmaymac.modules.user.dto.WorkshopProfileResponseRecord;
import vn.io.sanmaymac.modules.user.service.UserService;
import vn.io.sanmaymac.modules.finance.service.FinanceService;
import vn.io.sanmaymac.modules.order.service.OrderService;
import vn.io.sanmaymac.modules.workshop.dto.WorkshopDashboardSummaryResponseRecord;
import vn.io.sanmaymac.modules.workshop.dto.WorkshopMessageCreateRequest;
import vn.io.sanmaymac.modules.workshop.dto.WorkshopStatsResponseRecord;
import vn.io.sanmaymac.modules.workshop.service.WorkshopDashboardService;

@RestController
@RequestMapping("/api/workshop")
@Validated
public class WorkshopController {
    private final UserService userService;
    private final OrderService orderService;
    private final FinanceService financeService;
    private final ReviewService reviewService;
    private final WorkshopNotificationService notificationService;
    private final WorkshopMessageService messageService;
    private final WorkshopDashboardService dashboardService;

    public WorkshopController(
            UserService userService,
            OrderService orderService,
            FinanceService financeService,
            ReviewService reviewService,
            WorkshopNotificationService notificationService,
            WorkshopMessageService messageService,
            WorkshopDashboardService dashboardService) {
        this.userService = userService;
        this.orderService = orderService;
        this.financeService = financeService;
        this.reviewService = reviewService;
        this.notificationService = notificationService;
        this.messageService = messageService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<WorkshopDashboardSummaryResponseRecord>> getDashboardSummary() {
        return ResponseEntity.ok(ApiResponse.success("OK", dashboardService.getSummary()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<WorkshopStatsResponseRecord>> getWorkshopStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String groupBy) {
        return ResponseEntity.ok(ApiResponse.success("OK", dashboardService.getStats(from, to, groupBy)));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<WorkshopProfileResponseRecord>> getWorkshopProfile() {
        return ResponseEntity.ok(ApiResponse.success("OK", userService.getWorkshopProfile()));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<WorkshopProfileResponseRecord>> updateWorkshopProfile(
            @Valid @RequestBody WorkshopProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Updated", userService.updateWorkshopProfile(request)));
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<WorkshopProfileResponseRecord>> uploadWorkshopAvatar(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Uploaded", userService.uploadWorkshopAvatar(file)));
    }

    @PostMapping(value = "/profile/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<WorkshopProfileResponseRecord>> uploadWorkshopLogo(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Uploaded", userService.uploadWorkshopLogo(file)));
    }

    @GetMapping("/kyc")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<WorkshopKycResponseRecord>> getWorkshopKyc() {
        return ResponseEntity.ok(ApiResponse.success("OK", userService.getWorkshopKyc()));
    }

    @PutMapping("/kyc")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<WorkshopKycResponseRecord>> submitWorkshopKyc(
            @Valid @RequestBody WorkshopKycRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Submitted", userService.submitWorkshopKyc(request)));
    }

    @PostMapping(value = "/kyc/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<WorkshopKycResponseRecord>> uploadWorkshopKyc(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Uploaded", userService.uploadWorkshopKycDocument(file)));
    }

    @GetMapping("/portfolio")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<List<PortfolioItemResponseRecord>>> listPortfolio() {
        return ResponseEntity.ok(ApiResponse.success("OK", userService.listPortfolio()));
    }

    @PostMapping("/portfolio")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<PortfolioItemResponseRecord>> addPortfolioItem(
            @Valid @RequestBody PortfolioItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Created", userService.addPortfolioItem(request)));
    }

    @PutMapping("/portfolio/{id}")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<PortfolioItemResponseRecord>> updatePortfolioItem(
            @PathVariable Long id,
            @Valid @RequestBody PortfolioItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Updated", userService.updatePortfolioItem(id, request)));
    }

    @PostMapping(value = "/portfolio/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<PortfolioItemResponseRecord>> uploadPortfolioItem(
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(ApiResponse.success(
                "Created",
                userService.addPortfolioItemWithUpload(title, description, image)));
    }

    @DeleteMapping("/portfolio/{id}")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<Void>> deletePortfolioItem(@PathVariable Long id) {
        userService.deletePortfolioItem(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    @GetMapping("/reputation")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<ReputationResponseRecord>> getReputation() {
        return ResponseEntity.ok(ApiResponse.success("OK", userService.getReputation()));
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<Page<ReviewResponseRecord>>> getWorkshopReviews(
            @RequestParam(required = false) Long workshopId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean hasImages,
            Pageable pageable) {
        if (workshopId != null) {
            return ResponseEntity.ok(ApiResponse.success("OK", reviewService.getPublicReviewsByWorkshop(workshopId, rating, hasImages, pageable)));
        }
        return ResponseEntity.ok(ApiResponse.success("OK", reviewService.getWorkshopReviews(rating, hasImages, pageable)));
    }

    @GetMapping("/reviews/summary")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<ReviewSummaryRecord>> getWorkshopReviewSummary() {
        return ResponseEntity.ok(ApiResponse.success("OK", reviewService.getWorkshopSummary(userService.getCurrentWorkshopId())));
    }

    @GetMapping("/orders/{orderId}")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<OrderDetailResponseRecord>> getWorkshopOrderDetail(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("OK", orderService.getWorkshopOrderDetail(orderId)));
    }

    @GetMapping("/orders/{orderId}/timeline")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<OrderTimelineResponseRecord>> getWorkshopOrderTimeline(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("OK", orderService.getWorkshopOrderTimeline(orderId)));
    }

    @GetMapping("/notifications")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<List<WorkshopNotificationResponseRecord>>> listNotifications() {
        return ResponseEntity.ok(ApiResponse.success("OK", notificationService.listMyNotifications()));
    }

    @GetMapping("/notifications/unread-count")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<Long>> getUnreadNotificationCount() {
        return ResponseEntity.ok(ApiResponse.success("OK", notificationService.getUnreadCount()));
    }

    @PutMapping("/notifications/{notificationId}/read")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<WorkshopNotificationResponseRecord>> markNotificationAsRead(
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(ApiResponse.success("Updated", notificationService.markAsRead(notificationId)));
    }

    @DeleteMapping("/notifications/{notificationId}")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteMyNotification(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }

    @GetMapping("/messages/threads")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<List<WorkshopMessageThreadResponseRecord>>> listMessageThreads() {
        return ResponseEntity.ok(ApiResponse.success("OK", messageService.listMyThreads()));
    }

    @GetMapping("/messages/{threadId}")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<WorkshopMessageThreadDetailResponseRecord>> getMessageThread(@PathVariable Long threadId) {
        return ResponseEntity.ok(ApiResponse.success("OK", messageService.getThread(threadId)));
    }

    @PostMapping("/messages/{threadId}")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<WorkshopMessageThreadDetailResponseRecord>> sendMessage(
            @PathVariable Long threadId,
            @Valid @RequestBody WorkshopMessageCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Sent", messageService.sendMessage(threadId, request.content())));
    }

    @GetMapping("/bank-account")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<BankAccountResponseRecord>> getBankAccount() {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.getMyBankAccount()));
    }

    @PutMapping("/bank-account")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<BankAccountResponseRecord>> upsertBankAccount(
            @Valid @RequestBody BankAccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.upsertBankAccount(request)));
    }

    @GetMapping("/payouts")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<List<PayoutResponseRecord>>> listMyPayouts() {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.listMyPayouts()));
    }

    @GetMapping("/payouts/{payoutId}")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<PayoutResponseRecord>> getMyPayout(@PathVariable Long payoutId) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.getMyPayout(payoutId)));
    }

    @PostMapping("/payouts/{payoutId}/cancel")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<PayoutResponseRecord>> cancelMyPayout(@PathVariable Long payoutId) {
        return ResponseEntity.ok(ApiResponse.success("Cancelled", financeService.cancelMyPayout(payoutId)));
    }
}