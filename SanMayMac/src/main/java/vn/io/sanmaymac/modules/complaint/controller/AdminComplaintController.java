package vn.io.sanmaymac.modules.complaint.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import vn.io.sanmaymac.common.enums.ComplaintStatus;
import vn.io.sanmaymac.modules.complaint.dto.AdminUpdateComplaintStatusRequest;
import vn.io.sanmaymac.modules.complaint.dto.ComplaintResponseRecord;
import vn.io.sanmaymac.modules.complaint.dto.DisputeResponseRecord;
import vn.io.sanmaymac.modules.complaint.service.ComplaintService;

@RestController
@RequestMapping("/api/admin/complaints")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminComplaintController {
    private final ComplaintService complaintService;

    public AdminComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ComplaintResponseRecord>>> listComplaints(
            @RequestParam(required = false) ComplaintStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", complaintService.listComplaintsForAdmin(status, pageable)));
    }

    @GetMapping("/{complaintId}")
    public ResponseEntity<ApiResponse<ComplaintResponseRecord>> getComplaint(@PathVariable Long complaintId) {
        return ResponseEntity.ok(ApiResponse.success("OK", complaintService.getComplaintForAdmin(complaintId)));
    }

    @PutMapping("/{complaintId}/status")
    public ResponseEntity<ApiResponse<ComplaintResponseRecord>> updateStatus(
            @PathVariable Long complaintId,
            @Valid @RequestBody AdminUpdateComplaintStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Updated", complaintService.updateComplaintStatus(complaintId, request)));
    }

    @PostMapping("/{complaintId}/escalate")
    public ResponseEntity<ApiResponse<DisputeResponseRecord>> escalateToDispute(@PathVariable Long complaintId) {
        return ResponseEntity.ok(ApiResponse.success("Escalated", complaintService.escalateToDispute(complaintId)));
    }
}
