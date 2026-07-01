package vn.io.sanmaymac.modules.bidding.controller;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import vn.io.sanmaymac.modules.bidding.dto.AcceptQuoteRequest;
import vn.io.sanmaymac.modules.bidding.dto.BiddingDesignRecord;
import vn.io.sanmaymac.modules.bidding.dto.BiddingDesignUploadResponseRecord;
import vn.io.sanmaymac.modules.bidding.dto.BiddingPostCreateRequest;
import vn.io.sanmaymac.modules.bidding.dto.BiddingPostDetailRecord;
import vn.io.sanmaymac.modules.bidding.dto.BiddingPostSummaryRecord;
import vn.io.sanmaymac.modules.bidding.dto.BiddingPostUpdateRequest;
import vn.io.sanmaymac.modules.bidding.dto.QuoteCreateRequest;
import vn.io.sanmaymac.modules.bidding.dto.QuoteResponseRecord;
import vn.io.sanmaymac.modules.bidding.dto.QuoteUpdateRequest;
import vn.io.sanmaymac.modules.bidding.dto.UpdatePostStatusRequest;
import vn.io.sanmaymac.modules.order.dto.OrderDetailResponseRecord;
import vn.io.sanmaymac.modules.bidding.service.BiddingService;

@RestController
@RequestMapping("/api/bidding")
@Validated
public class BiddingController {
	private final BiddingService biddingService;

	public BiddingController(BiddingService biddingService) {
		this.biddingService = biddingService;
	}

	@PostMapping("/posts")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<BiddingPostDetailRecord>> createPost(
			@Valid @RequestBody BiddingPostCreateRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Created", biddingService.createPost(request)));
	}

	@PostMapping(value = "/designs/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<BiddingDesignUploadResponseRecord>> uploadDesignFiles(
			@RequestPart("frontDesign") MultipartFile frontDesign,
			@RequestPart("backDesign") MultipartFile backDesign) {
		return ResponseEntity.ok(ApiResponse.success("Uploaded", biddingService.uploadDesignFiles(frontDesign, backDesign)));
	}

	@PostMapping(value = "/designs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<BiddingDesignRecord>> saveDesign(
			@RequestPart("frontDesign") MultipartFile frontDesign,
			@RequestPart("backDesign") MultipartFile backDesign,
			@RequestParam(required = false) String name) {
		return ResponseEntity.ok(ApiResponse.success("Saved", biddingService.saveDesign(frontDesign, backDesign, name)));
	}

	@GetMapping("/designs/me")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<List<BiddingDesignRecord>>> listMyDesigns() {
		return ResponseEntity.ok(ApiResponse.success("OK", biddingService.listMyDesigns()));
	}

	@GetMapping("/posts/me")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<Page<BiddingPostSummaryRecord>>> listMyPosts(Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("OK", biddingService.listMyPosts(pageable)));
	}

	@PutMapping("/posts/{postId}")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<BiddingPostDetailRecord>> updatePost(
			@PathVariable Long postId,
			@Valid @RequestBody BiddingPostUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", biddingService.updatePost(postId, request)));
	}

	@DeleteMapping("/posts/{postId}")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long postId) {
		biddingService.deletePost(postId);
		return ResponseEntity.ok(ApiResponse.success("Deleted", null));
	}

	@GetMapping("/posts/{postId}")
	@PreAuthorize("hasAnyRole('CUSTOMER','WORKSHOP','ADMIN')")
	public ResponseEntity<ApiResponse<BiddingPostDetailRecord>> getPostDetail(@PathVariable Long postId) {
		return ResponseEntity.ok(ApiResponse.success("OK", biddingService.getPostDetail(postId)));
	}

	@GetMapping("/posts/{postId}/quotes")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<Page<QuoteResponseRecord>>> listPostQuotes(
			@PathVariable Long postId,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("OK", biddingService.listPostQuotes(postId, pageable)));
	}

	@PostMapping("/posts/{postId}/quotes/{quoteId}/accept")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<OrderDetailResponseRecord>> acceptQuote(
			@PathVariable Long postId,
			@PathVariable Long quoteId,
			@Valid @RequestBody AcceptQuoteRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Accepted", biddingService.acceptQuote(postId, quoteId, request)));
	}

	@GetMapping("/posts/open")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<Page<BiddingPostSummaryRecord>>> exploreOpenPosts(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) String sort,
			@RequestParam(required = false) Integer maxQuotes,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("OK", biddingService.exploreOpenPosts(keyword, sort, maxQuotes, pageable)));
	}

	@GetMapping("/posts/{postId}/quotes/me")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<QuoteResponseRecord>> getMyQuoteOnPost(@PathVariable Long postId) {
		return ResponseEntity.ok(ApiResponse.success("OK", biddingService.getMyQuoteOnPost(postId)));
	}

	@PostMapping("/posts/{postId}/quotes")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<QuoteResponseRecord>> submitQuote(
			@PathVariable Long postId,
			@Valid @RequestBody QuoteCreateRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Submitted", biddingService.submitQuote(postId, request)));
	}

	@PutMapping("/quotes/{quoteId}")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<QuoteResponseRecord>> updateQuote(
			@PathVariable Long quoteId,
			@Valid @RequestBody QuoteUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", biddingService.updateQuote(quoteId, request)));
	}

	@PostMapping("/quotes/{quoteId}/withdraw")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<QuoteResponseRecord>> withdrawQuote(@PathVariable Long quoteId) {
		return ResponseEntity.ok(ApiResponse.success("Withdrawn", biddingService.withdrawQuote(quoteId)));
	}

	@GetMapping("/quotes/me")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<Page<QuoteResponseRecord>>> listMyQuotes(
			@RequestParam(required = false) String status,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("OK", biddingService.listMyQuotes(status, pageable)));
	}

	@GetMapping("/admin/posts")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<BiddingPostSummaryRecord>>> listAllPosts(
			@RequestParam(required = false) String status,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("OK", biddingService.listAllPosts(status, pageable)));
	}

	@PutMapping("/admin/posts/{postId}/status")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<BiddingPostDetailRecord>> updatePostStatus(
			@PathVariable Long postId,
			@Valid @RequestBody UpdatePostStatusRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", biddingService.updatePostStatus(postId, request)));
	}

	@GetMapping("/admin/quotes")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<Page<QuoteResponseRecord>>> listAllQuotes(
			@RequestParam(required = false) String status,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("OK", biddingService.listAllQuotes(status, pageable)));
	}
}
