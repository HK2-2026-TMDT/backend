package vn.io.sanmaymac.modules.complaint.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import vn.io.sanmaymac.common.dto.ApiResponse;
import vn.io.sanmaymac.common.enums.DisputeStatus;
import vn.io.sanmaymac.modules.complaint.dto.DisputeRequestInfoRequest;
import vn.io.sanmaymac.modules.complaint.dto.DisputeResponseRecord;
import vn.io.sanmaymac.modules.complaint.dto.DisputeRulingRequest;
import vn.io.sanmaymac.modules.complaint.dto.ViolationRecordRequest;
import vn.io.sanmaymac.modules.complaint.service.DisputeService;

@RestController
@RequestMapping("/api/admin/disputes")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminDisputeController {
    private final DisputeService disputeService;

    public AdminDisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<DisputeResponseRecord>>> listDisputes(
            @RequestParam(required = false) DisputeStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", disputeService.listDisputesForAdmin(status, pageable)));
    }

    @GetMapping("/{disputeId}")
    public ResponseEntity<ApiResponse<DisputeResponseRecord>> getDispute(@PathVariable Long disputeId) {
        return ResponseEntity.ok(ApiResponse.success("OK", disputeService.getDisputeForAdmin(disputeId)));
    }

    @PostMapping("/{disputeId}/request-info")
    public ResponseEntity<ApiResponse<DisputeResponseRecord>> requestInfo(
            @PathVariable Long disputeId,
            @Valid @RequestBody DisputeRequestInfoRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Requested", disputeService.requestAdditionalInfo(disputeId, request)));
    }

    @PostMapping("/{disputeId}/ruling")
    public ResponseEntity<ApiResponse<DisputeResponseRecord>> submitRuling(
            @PathVariable Long disputeId,
            @Valid @RequestBody DisputeRulingRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Ruled", disputeService.submitRuling(disputeId, request)));
    }

    @PostMapping("/{disputeId}/refund-customer")
    public ResponseEntity<ApiResponse<DisputeResponseRecord>> refundCustomer(@PathVariable Long disputeId) {
        return ResponseEntity.ok(ApiResponse.success("Refunded", disputeService.refundCustomer(disputeId)));
    }

    @PostMapping("/{disputeId}/release-workshop")
    public ResponseEntity<ApiResponse<DisputeResponseRecord>> releaseToWorkshop(@PathVariable Long disputeId) {
        return ResponseEntity.ok(ApiResponse.success("Released", disputeService.releaseToWorkshop(disputeId)));
    }

    @PostMapping("/{disputeId}/violation")
    public ResponseEntity<ApiResponse<DisputeResponseRecord>> saveViolation(
            @PathVariable Long disputeId,
            @Valid @RequestBody ViolationRecordRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Saved", disputeService.saveViolationRecord(disputeId, request)));
    }

    @PostMapping("/{disputeId}/close")
    public ResponseEntity<ApiResponse<DisputeResponseRecord>> closeDispute(@PathVariable Long disputeId) {
        return ResponseEntity.ok(ApiResponse.success("Closed", disputeService.closeDispute(disputeId)));
    }
}
