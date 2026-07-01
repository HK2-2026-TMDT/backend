package vn.io.sanmaymac.modules.complaint.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import vn.io.sanmaymac.common.dto.ApiResponse;
import vn.io.sanmaymac.modules.complaint.dto.ComplaintCreateRequest;
import vn.io.sanmaymac.modules.complaint.dto.ComplaintResponseRecord;
import vn.io.sanmaymac.modules.complaint.dto.ImageUploadResponseRecord;
import vn.io.sanmaymac.modules.complaint.service.ComplaintService;

@RestController
@RequestMapping("/api/complaints")
@Validated
public class ComplaintController {
    private final ComplaintService complaintService;

    public ComplaintController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ComplaintResponseRecord>> createComplaint(
            @Valid @RequestBody ComplaintCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Created", complaintService.createComplaint(request)));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ImageUploadResponseRecord>> uploadEvidence(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Uploaded", complaintService.uploadEvidence(file)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Page<ComplaintResponseRecord>>> getMyComplaints(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("OK", complaintService.getMyComplaints(pageable)));
    }

    @GetMapping("/me/{complaintId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ComplaintResponseRecord>> getMyComplaint(@PathVariable Long complaintId) {
        return ResponseEntity.ok(ApiResponse.success("OK", complaintService.getMyComplaint(complaintId)));
    }

    @GetMapping("/by-order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<ComplaintResponseRecord>> getByOrder(@PathVariable Long orderId) {
        ComplaintResponseRecord record = complaintService.getComplaintByOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success("OK", record));
    }
}
