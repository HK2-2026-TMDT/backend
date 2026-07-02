package vn.io.sanmaymac.modules.finance.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
import vn.io.sanmaymac.modules.finance.dto.AdminDashboardStatsResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.AiTokenPurchaseRequest;
import vn.io.sanmaymac.modules.finance.dto.BankAccountRequest;
import vn.io.sanmaymac.modules.finance.dto.BankAccountResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.CashflowResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.CheckoutBatchSummaryResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.CommissionConfigRequest;
import vn.io.sanmaymac.modules.finance.dto.CommissionConfigResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.MomoCreatePaymentRequest;
import vn.io.sanmaymac.modules.finance.dto.MomoCreatePaymentResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.PayoutApprovalRequest;
import vn.io.sanmaymac.modules.finance.dto.PayoutRequestCreate;
import vn.io.sanmaymac.modules.finance.dto.PayoutResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.RevenueSummaryRecord;
import vn.io.sanmaymac.modules.finance.dto.TransactionResponseRecord;
import vn.io.sanmaymac.modules.finance.dto.WalletResponseRecord;
import vn.io.sanmaymac.modules.finance.service.FinanceService;

@RestController
@RequestMapping("/api/finance")
@Validated
public class FinanceController {
    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    // Customer endpoints
    @GetMapping("/wallet")
    @PreAuthorize("hasAnyRole('CUSTOMER','WORKSHOP')")
    public ResponseEntity<ApiResponse<WalletResponseRecord>> getMyWallet() {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.getMyWallet()));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('CUSTOMER','WORKSHOP')")
    public ResponseEntity<ApiResponse<List<TransactionResponseRecord>>> listMyTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.listMyTransactions(type, status)));
    }

    @PostMapping("/orders/{orderId}/pay")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<TransactionResponseRecord>> payOrder(
            @PathVariable Long orderId,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String transactionCode,
            @RequestParam(required = false) java.math.BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.payOrder(orderId, paymentMethod, transactionCode, amount)));
    }

    @PostMapping("/orders/{orderId}/pay-deposit")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<TransactionResponseRecord>> payOrderDeposit(
            @PathVariable Long orderId,
            @RequestParam(required = false) java.math.BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.payOrderDeposit(orderId, amount)));
    }

    @PostMapping("/orders/{orderId}/pay-balance")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<TransactionResponseRecord>> payOrderBalance(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.payOrderBalance(orderId)));
    }

    @PostMapping("/orders/{orderId}/momo/create")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<MomoCreatePaymentResponseRecord>> createMomoPayment(
            @PathVariable Long orderId,
            @RequestBody(required = false) MomoCreatePaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.createMomoPayment(orderId, request)));
    }

    @GetMapping("/checkout-batches/{checkoutBatchId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CheckoutBatchSummaryResponseRecord>> getCheckoutBatchSummary(
            @PathVariable String checkoutBatchId) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.getCheckoutBatchSummary(checkoutBatchId)));
    }

    @PostMapping("/checkout-batches/{checkoutBatchId}/momo/create")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<MomoCreatePaymentResponseRecord>> createBatchMomoPayment(
            @PathVariable String checkoutBatchId,
            @RequestBody(required = false) MomoCreatePaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.createMomoPaymentForCheckoutBatch(checkoutBatchId, request)));
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<java.util.Map<String, Object>> handleMomoIpn(@RequestBody java.util.Map<String, Object> payload) {
        return ResponseEntity.ok(financeService.handleMomoIpn(payload));
    }

    @PostMapping("/ai-tokens/purchase")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<TransactionResponseRecord>> purchaseAiTokens(
            @Valid @RequestBody AiTokenPurchaseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Purchased", financeService.purchaseAiTokens(request)));
    }

    // Workshop endpoints
    @PutMapping("/bank-account")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<BankAccountResponseRecord>> upsertBankAccount(
            @Valid @RequestBody BankAccountRequest request) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.upsertBankAccount(request)));
    }

    @GetMapping("/bank-account")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<BankAccountResponseRecord>> getMyBankAccount() {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.getMyBankAccount()));
    }

    @PostMapping("/payouts")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<PayoutResponseRecord>> createPayout(
            @Valid @RequestBody PayoutRequestCreate request) {
        return ResponseEntity.ok(ApiResponse.success("Created", financeService.createPayoutRequest(request)));
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

    @GetMapping("/revenue")
    @PreAuthorize("hasRole('WORKSHOP')")
    public ResponseEntity<ApiResponse<RevenueSummaryRecord>> getRevenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String groupBy) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.getWorkshopRevenue(from, to, groupBy)));
    }

    // Admin endpoints
    @GetMapping("/admin/payouts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PayoutResponseRecord>>> listAllPayouts(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.listAllPayouts(status)));
    }

    @PostMapping("/admin/payouts/{payoutId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PayoutResponseRecord>> approvePayout(
            @PathVariable Long payoutId,
            @RequestBody PayoutApprovalRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Approved", financeService.approvePayout(payoutId, request)));
    }

    @PostMapping("/admin/payouts/{payoutId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PayoutResponseRecord>> rejectPayout(
            @PathVariable Long payoutId,
            @RequestBody PayoutApprovalRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Rejected", financeService.rejectPayout(payoutId, request)));
    }

    @GetMapping("/admin/cashflow")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CashflowResponseRecord>> getCashflow(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.getCashflow(from, to)));
    }

    @GetMapping("/admin/dashboard-stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponseRecord>> getAdminDashboardStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String groupBy) {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.getAdminDashboardStats(from, to, groupBy)));
    }

    @GetMapping("/admin/commission")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommissionConfigResponseRecord>> getCommissionConfig() {
        return ResponseEntity.ok(ApiResponse.success("OK", financeService.getCommissionConfig()));
    }

    @PutMapping("/admin/commission")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CommissionConfigResponseRecord>> updateCommissionConfig(
            @Valid @RequestBody CommissionConfigRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Updated", financeService.updateCommissionConfig(request)));
    }
}
