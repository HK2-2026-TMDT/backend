package vn.io.sanmaymac.modules.review.controller;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import vn.io.sanmaymac.common.dto.ApiResponse;
import vn.io.sanmaymac.modules.review.dto.AdminUpdateReviewStatusRequest;
import vn.io.sanmaymac.modules.review.dto.ResolveReportRequest;
import vn.io.sanmaymac.modules.review.dto.ReviewCreateRequest;
import vn.io.sanmaymac.modules.review.dto.ReviewReplyRequest;
import vn.io.sanmaymac.modules.review.dto.ReviewReportRequest;
import vn.io.sanmaymac.modules.review.dto.ReviewReportResponseRecord;
import vn.io.sanmaymac.modules.review.dto.ReviewResponseRecord;
import vn.io.sanmaymac.modules.review.dto.ReviewSummaryRecord;
import vn.io.sanmaymac.modules.review.dto.ReviewUpdateRequest;
import vn.io.sanmaymac.modules.review.dto.UnreviewedOrderResponseRecord;
import vn.io.sanmaymac.modules.review.service.ReviewService;

@RestController
@RequestMapping("/api/reviews")
@Validated
public class ReviewController {
	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@PostMapping
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<ReviewResponseRecord>> createReview(
			@Valid @RequestBody ReviewCreateRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Created", reviewService.createReview(request)));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<ReviewResponseRecord>> updateReview(
			@PathVariable Long id,
			@Valid @RequestBody ReviewUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", reviewService.updateReview(id, request)));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<ReviewResponseRecord>> deleteReview(@PathVariable Long id) {
		return ResponseEntity.ok(ApiResponse.success("Deleted", reviewService.deleteReview(id)));
	}

	@GetMapping("/me")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<Page<ReviewResponseRecord>>> getMyReviews(Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("OK", reviewService.getMyReviews(pageable)));
	}

	@GetMapping("/me/unreviewed-orders")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<List<UnreviewedOrderResponseRecord>>> getUnreviewedOrders() {
		return ResponseEntity.ok(ApiResponse.success("OK", reviewService.getUnreviewedOrders()));
	}

	@GetMapping("/workshop")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<Page<ReviewResponseRecord>>> getWorkshopReviews(
			@RequestParam(required = false) Integer rating,
			@RequestParam(required = false) Boolean hasImages,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success(
				"OK",
				reviewService.getWorkshopReviews(rating, hasImages, pageable)));
	}

	@PostMapping("/{id}/reply")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<ReviewResponseRecord>> replyToReview(
			@PathVariable Long id,
			@Valid @RequestBody ReviewReplyRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Replied", reviewService.replyToReview(id, request)));
	}

	@PostMapping("/{id}/report")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<ReviewReportResponseRecord>> reportReview(
			@PathVariable Long id,
			@Valid @RequestBody ReviewReportRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Reported", reviewService.reportReview(id, request)));
	}

	@GetMapping("/public/workshops/{workshopId}")
	    public ResponseEntity<ApiResponse<Page<ReviewResponseRecord>>> getPublicWorkshopReviews(
		    @PathVariable Long workshopId,
		    @RequestParam(required = false) Integer rating,
		    @RequestParam(required = false) Boolean hasImages,
		    Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success(
			"OK",
			reviewService.getPublicReviewsByWorkshop(workshopId, rating, hasImages, pageable)));
	    }

	@GetMapping("/public/products/{productId}")
	    public ResponseEntity<ApiResponse<Page<ReviewResponseRecord>>> getPublicProductReviews(
		    @PathVariable Long productId,
		    @RequestParam(required = false) Integer rating,
		    @RequestParam(required = false) Boolean hasImages,
		    Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success(
			"OK",
			reviewService.getPublicReviewsByProduct(productId, rating, hasImages, pageable)));
	    }

	@GetMapping("/public/workshops/{workshopId}/summary")
	public ResponseEntity<ApiResponse<ReviewSummaryRecord>> getWorkshopSummary(
			@PathVariable Long workshopId) {
		return ResponseEntity.ok(ApiResponse.success("OK", reviewService.getWorkshopSummary(workshopId)));
	}

	@GetMapping("/public/products/{productId}/summary")
	public ResponseEntity<ApiResponse<ReviewSummaryRecord>> getProductSummary(
			@PathVariable Long productId) {
		return ResponseEntity.ok(ApiResponse.success("OK", reviewService.getProductSummary(productId)));
	}

	@GetMapping("/admin")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<ReviewResponseRecord>>> listAllReviews(
			@RequestParam(required = false) String status,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("OK", reviewService.listAllReviews(status, pageable)));
	}

	@PutMapping("/admin/{reviewId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<ReviewResponseRecord>> updateReviewStatus(
			@PathVariable Long reviewId,
			@Valid @RequestBody AdminUpdateReviewStatusRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", reviewService.updateReviewStatus(reviewId, request)));
	}

	@GetMapping("/admin/reports")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<List<ReviewReportResponseRecord>>> listReports(
			@RequestParam(required = false) String status) {
		return ResponseEntity.ok(ApiResponse.success("OK", reviewService.listReports(status)));
	}

	@PutMapping("/admin/reports/{reportId}/resolve")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<ReviewReportResponseRecord>> resolveReport(
			@PathVariable Long reportId,
			@Valid @RequestBody ResolveReportRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Resolved", reviewService.resolveReport(reportId, request)));
	}
}
