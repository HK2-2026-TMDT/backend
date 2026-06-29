package vn.io.sanmaymac.modules.catalog.controller;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import vn.io.sanmaymac.common.dto.ApiResponse;
import vn.io.sanmaymac.modules.catalog.dto.CategoryResponseRecord;
import vn.io.sanmaymac.modules.catalog.dto.ProductCreateRequest;
import vn.io.sanmaymac.modules.catalog.dto.ProductDetailResponseRecord;
import vn.io.sanmaymac.modules.catalog.dto.ProductImageRequest;
import vn.io.sanmaymac.modules.catalog.dto.ProductSummaryResponseRecord;
import vn.io.sanmaymac.modules.catalog.dto.ProductUpdateRequest;
import vn.io.sanmaymac.modules.catalog.dto.ProductVariantRequest;
import vn.io.sanmaymac.modules.catalog.dto.ProductVariantResponseRecord;
import vn.io.sanmaymac.modules.catalog.dto.ProductVisibilityUpdateRequest;
import vn.io.sanmaymac.modules.catalog.dto.StockUpdateRequest;
import vn.io.sanmaymac.modules.catalog.service.CatalogService;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/catalog")
@Validated
public class CatalogController {
	private final CatalogService catalogService;

	public CatalogController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@GetMapping("/categories")
	public ResponseEntity<ApiResponse<List<CategoryResponseRecord>>> listCategories() {
		return ResponseEntity.ok(ApiResponse.success("OK", catalogService.listCategories()));
	}

	@GetMapping("/products")
	public ResponseEntity<ApiResponse<Page<ProductSummaryResponseRecord>>> listProducts(
			@RequestParam(required = false) String keyword,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false) Long workshopId,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success(
				"OK",
				catalogService.listPublicProducts(keyword, categoryId, workshopId, minPrice, maxPrice, pageable)));
	}

	@GetMapping("/products/{productId}")
	public ResponseEntity<ApiResponse<ProductDetailResponseRecord>> getProductDetail(@PathVariable Long productId) {
		return ResponseEntity.ok(ApiResponse.success("OK", catalogService.getProductDetail(productId)));
	}

	@GetMapping("/products/newest")
	public ResponseEntity<ApiResponse<List<ProductSummaryResponseRecord>>> listNewestProducts() {
		return ResponseEntity.ok(ApiResponse.success("OK", catalogService.listNewestProducts()));
	}

	@GetMapping("/me/favorites/products")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<List<ProductSummaryResponseRecord>>> listFavoriteProducts() {
		return ResponseEntity.ok(ApiResponse.success("OK", catalogService.listFavoriteProducts()));
	}

	@PostMapping("/products/{productId}/favorites")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<Void>> addFavoriteProduct(@PathVariable Long productId) {
		catalogService.addFavoriteProduct(productId);
		return ResponseEntity.ok(ApiResponse.success("Added", null));
	}

	@DeleteMapping("/products/{productId}/favorites")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<ApiResponse<Void>> removeFavoriteProduct(@PathVariable Long productId) {
		catalogService.removeFavoriteProduct(productId);
		return ResponseEntity.ok(ApiResponse.success("Removed", null));
	}

	@PostMapping("/products")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<ProductDetailResponseRecord>> createProduct(
			@Valid @RequestBody ProductCreateRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Created", catalogService.createProduct(request)));
	}

	@PutMapping("/products/{productId}")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<ProductDetailResponseRecord>> updateProduct(
			@PathVariable Long productId,
			@Valid @RequestBody ProductUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", catalogService.updateProduct(productId, request)));
	}

	@DeleteMapping("/products/{productId}")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long productId) {
		catalogService.deleteProduct(productId);
		return ResponseEntity.ok(ApiResponse.success("Deleted", null));
	}

	@PutMapping("/products/{productId}/visibility")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<ProductDetailResponseRecord>> updateProductVisibility(
			@PathVariable Long productId,
			@Valid @RequestBody ProductVisibilityUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", catalogService.updateProductVisibility(productId, request)));
	}

	@GetMapping("/products/me")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<Page<ProductSummaryResponseRecord>>> listMyProducts(Pageable pageable) {
		return ResponseEntity.ok(ApiResponse.success("OK", catalogService.listMyProducts(pageable)));
	}

	@PostMapping("/products/{productId}/variants")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<ProductVariantResponseRecord>> addVariant(
			@PathVariable Long productId,
			@Valid @RequestBody ProductVariantRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Created", catalogService.addVariant(productId, request)));
	}

	@PutMapping("/variants/{variantId}")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<ProductVariantResponseRecord>> updateVariant(
			@PathVariable Long variantId,
			@Valid @RequestBody ProductVariantRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", catalogService.updateVariant(variantId, request)));
	}

	@PutMapping("/variants/{variantId}/stock")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<ProductVariantResponseRecord>> updateStock(
			@PathVariable Long variantId,
			@Valid @RequestBody StockUpdateRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Updated", catalogService.updateVariantStock(variantId, request)));
	}

	@DeleteMapping("/variants/{variantId}")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<Void>> deleteVariant(@PathVariable Long variantId) {
		catalogService.deleteVariant(variantId);
		return ResponseEntity.ok(ApiResponse.success("Deleted", null));
	}

	@PutMapping("/products/{productId}/images")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<ProductDetailResponseRecord>> replaceImages(
			@PathVariable Long productId,
			@RequestBody List<ProductImageRequest> images) {
		return ResponseEntity.ok(ApiResponse.success(
				"Updated",
				catalogService.replaceProductImages(productId, images)));
	}

	@PostMapping(value = "/products/{productId}/images/upload", consumes = "multipart/form-data")
	@PreAuthorize("hasRole('WORKSHOP')")
	public ResponseEntity<ApiResponse<ProductDetailResponseRecord>> uploadImage(
			@PathVariable Long productId,
			@RequestPart("image") MultipartFile image,
			@RequestParam(required = false, defaultValue = "false") Boolean isThumbnail) {
		return ResponseEntity.ok(ApiResponse.success(
				"Uploaded",
				catalogService.addProductImageUpload(productId, image, isThumbnail)));
	}
}
