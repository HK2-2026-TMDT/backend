package vn.io.sanmaymac.modules.catalog.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import vn.io.sanmaymac.common.dto.ApiResponse;
import vn.io.sanmaymac.common.enums.ProductApprovalStatus;
import vn.io.sanmaymac.modules.catalog.dto.AdminProductReviewRecord;
import vn.io.sanmaymac.modules.catalog.dto.ProductApprovalRequest;
import vn.io.sanmaymac.modules.catalog.service.CatalogService;

@RestController
@RequestMapping("/api/admin/products")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class CatalogAdminController {
	private final CatalogService catalogService;

	public CatalogAdminController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@GetMapping
	public ResponseEntity<ApiResponse<Page<AdminProductReviewRecord>>> listProducts(
			@RequestParam(required = false) ProductApprovalStatus status,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("OK", catalogService.listProductsForAdmin(status, pageable)));
	}

	@GetMapping("/{productId}")
	public ResponseEntity<ApiResponse<AdminProductReviewRecord>> getProduct(@PathVariable Long productId) {
		return ResponseEntity.ok(ApiResponse.success("OK", catalogService.getProductForAdmin(productId)));
	}

	@PutMapping("/{productId}/approval")
	public ResponseEntity<ApiResponse<AdminProductReviewRecord>> reviewProduct(
			@PathVariable Long productId,
			@Valid @RequestBody ProductApprovalRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", catalogService.reviewProduct(productId, request)));
	}
}
