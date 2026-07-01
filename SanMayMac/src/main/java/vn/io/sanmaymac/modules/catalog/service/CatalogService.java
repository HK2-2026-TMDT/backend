package vn.io.sanmaymac.modules.catalog.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.io.sanmaymac.common.enums.ProductApprovalStatus;
import vn.io.sanmaymac.common.enums.Role;
import vn.io.sanmaymac.common.service.MediaStorageService;
import vn.io.sanmaymac.common.utils.SecurityUtils;
import vn.io.sanmaymac.modules.catalog.dto.AdminProductReviewRecord;
import vn.io.sanmaymac.modules.catalog.dto.CategoryResponseRecord;
import vn.io.sanmaymac.modules.catalog.dto.ProductApprovalRequest;
import vn.io.sanmaymac.modules.catalog.dto.ProductCreateRequest;
import vn.io.sanmaymac.modules.catalog.dto.ProductDetailCacheRecord;
import vn.io.sanmaymac.modules.catalog.dto.ProductDetailResponseRecord;
import vn.io.sanmaymac.modules.catalog.dto.ProductImageRequest;
import vn.io.sanmaymac.modules.catalog.dto.ProductImageResponseRecord;
import vn.io.sanmaymac.modules.catalog.dto.ProductSummaryResponseRecord;
import vn.io.sanmaymac.modules.catalog.dto.ProductUpdateRequest;
import vn.io.sanmaymac.modules.catalog.dto.ProductVariantRequest;
import vn.io.sanmaymac.modules.catalog.dto.ProductVariantResponseRecord;
import vn.io.sanmaymac.modules.catalog.dto.ProductVisibilityUpdateRequest;
import vn.io.sanmaymac.modules.catalog.dto.StockUpdateRequest;
import vn.io.sanmaymac.modules.catalog.entity.CategoryEntity;
import vn.io.sanmaymac.modules.catalog.entity.ProductFavoriteEntity;
import vn.io.sanmaymac.modules.catalog.entity.ProductEntity;
import vn.io.sanmaymac.modules.catalog.entity.ProductImageEntity;
import vn.io.sanmaymac.modules.catalog.entity.ProductVariantEntity;
import vn.io.sanmaymac.modules.catalog.repository.CategoryRepository;
import vn.io.sanmaymac.modules.catalog.repository.ProductFavoriteRepository;
import vn.io.sanmaymac.modules.catalog.repository.ProductImageRepository;
import vn.io.sanmaymac.modules.catalog.repository.ProductRepository;
import vn.io.sanmaymac.modules.catalog.repository.ProductVariantRepository;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.repository.UserRepository;
import vn.io.sanmaymac.modules.user.repository.WorkshopProfileRepository;
import java.time.Instant;

@Service
@Transactional
public class CatalogService {
	private static final Logger log = LoggerFactory.getLogger(CatalogService.class);

	private final ProductRepository productRepository;
	private final ProductVariantRepository productVariantRepository;
	private final ProductImageRepository productImageRepository;
	private final ProductFavoriteRepository productFavoriteRepository;
	private final CatalogCacheService catalogCacheService;
	private final CategoryRepository categoryRepository;
	private final UserRepository userRepository;
	private final WorkshopProfileRepository workshopProfileRepository;
	private final MediaStorageService mediaStorageService;

	public CatalogService(
			ProductRepository productRepository,
			ProductVariantRepository productVariantRepository,
			ProductImageRepository productImageRepository,
			ProductFavoriteRepository productFavoriteRepository,
			CatalogCacheService catalogCacheService,
			CategoryRepository categoryRepository,
			UserRepository userRepository,
			WorkshopProfileRepository workshopProfileRepository,
			MediaStorageService mediaStorageService) {
		this.productRepository = productRepository;
		this.productVariantRepository = productVariantRepository;
		this.productImageRepository = productImageRepository;
		this.productFavoriteRepository = productFavoriteRepository;
		this.catalogCacheService = catalogCacheService;
		this.categoryRepository = categoryRepository;
		this.userRepository = userRepository;
		this.workshopProfileRepository = workshopProfileRepository;
		this.mediaStorageService = mediaStorageService;
	}

	public List<CategoryResponseRecord> listCategories() {
		return catalogCacheService.getCategories();
	}

	@Caching(evict = {
			@CacheEvict(cacheNames = "catalog:newest-products", allEntries = true)
	})
	public ProductDetailResponseRecord createProduct(ProductCreateRequest request) {
		UserEntity workshop = getWorkshopUser();
		CategoryEntity category = categoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new IllegalArgumentException("Category not found"));

		ProductEntity product = ProductEntity.builder()
				.name(request.name())
				.category(category)
				.workshop(workshop)
				.basePrice(request.basePrice())
				.description(request.description())
				.isVisible(false)
				.approvalStatus(ProductApprovalStatus.PENDING)
				.build();
		ProductEntity saved = productRepository.save(product);

		replaceVariants(saved, request.variants());
		replaceImages(saved, request.images());
		return buildProductDetail(saved, false);
	}

	@Caching(evict = {
			@CacheEvict(cacheNames = "catalog:newest-products", allEntries = true),
			@CacheEvict(cacheNames = "catalog:product-detail", key = "#productId")
	})
	public ProductDetailResponseRecord updateProduct(Long productId, ProductUpdateRequest request) {
		ProductEntity product = getProductForWorkshop(productId);
		CategoryEntity category = categoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new IllegalArgumentException("Category not found"));
		product.setName(request.name());
		product.setCategory(category);
		product.setBasePrice(request.basePrice());
		product.setDescription(request.description());
		markProductPendingReview(product);
		productRepository.save(product);

		if (request.variants() != null) {
			replaceVariants(product, request.variants());
		}
		if (request.images() != null) {
			replaceImages(product, request.images());
		}
		return buildProductDetail(product, false);
	}

	@Caching(evict = {
			@CacheEvict(cacheNames = "catalog:newest-products", allEntries = true),
			@CacheEvict(cacheNames = "catalog:product-detail", key = "#productId")
	})
	public void deleteProduct(Long productId) {
		ProductEntity product = getProductForWorkshop(productId);
		productVariantRepository.deleteByProductId(product.getId());
		productImageRepository.deleteByProductId(product.getId());
		productFavoriteRepository.deleteByProductId(product.getId());
		productRepository.delete(product);
	}

	public List<ProductSummaryResponseRecord> listNewestProducts() {
		Set<Long> favoriteIds = getFavoriteProductIdsForCurrentCustomer();
		return catalogCacheService.getNewestProducts().stream()
				.map(product -> new ProductSummaryResponseRecord(
						product.id(),
						product.name(),
						product.basePrice(),
						product.thumbnailUrl(),
						product.workshopId(),
						favoriteIds.contains(product.id()),
						true,
						ProductApprovalStatus.APPROVED,
						null,
						null,
						null,
						null,
						null,
						null))
				.toList();
	}

	public List<ProductSummaryResponseRecord> listFavoriteProducts() {
		UserEntity customer = getCustomerUser();
		Set<Long> favoriteIds = getFavoriteProductIdsForCurrentCustomer();
		return productFavoriteRepository.findByCustomerId(customer.getId())
				.stream()
				.sorted(Comparator.comparing(
						ProductFavoriteEntity::getCreatedAt,
						Comparator.nullsLast(Comparator.naturalOrder())).reversed())
				.map(ProductFavoriteEntity::getProduct)
				.filter(product -> product != null)
				.filter(this::isPubliclyAccessible)
				.map(product -> toProductSummary(product, favoriteIds))
				.toList();
	}

	public void addFavoriteProduct(Long productId) {
		UserEntity customer = getCustomerUser();
		ProductEntity product = productRepository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("Product not found"));
		if (!isPubliclyAccessible(product)) {
			throw new IllegalArgumentException("Product not available");
		}
		if (productFavoriteRepository.existsByCustomerIdAndProductId(customer.getId(), productId)) {
			return;
		}
		ProductFavoriteEntity favorite = ProductFavoriteEntity.builder()
				.customer(customer)
				.product(product)
				.build();
		productFavoriteRepository.save(favorite);
	}

	public void removeFavoriteProduct(Long productId) {
		UserEntity customer = getCustomerUser();
		productFavoriteRepository.findByCustomerIdAndProductId(customer.getId(), productId)
				.ifPresent(productFavoriteRepository::delete);
	}

	public Page<ProductSummaryResponseRecord> listMyProducts(Pageable pageable) {
		UserEntity workshop = getWorkshopUser();
		Set<Long> favoriteIds = getFavoriteProductIdsForCurrentCustomer();
		return productRepository.findByWorkshopId(workshop.getId(), pageable)
				.map(product -> toProductSummary(product, favoriteIds));
	}

	public Page<ProductSummaryResponseRecord> listPublicProducts(
			String keyword,
			Long categoryId,
			Long workshopId,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Pageable pageable) {
		List<ProductEntity> products = productRepository.findAll();
		List<ProductEntity> filtered = products.stream()
				.filter(this::isPubliclyAccessible)
				.filter(product -> keyword == null || keyword.isBlank()
						|| containsIgnoreCase(product.getName(), keyword)
						|| containsIgnoreCase(product.getDescription(), keyword))
				.filter(product -> categoryId == null
						|| product.getCategory() != null && categoryId.equals(product.getCategory().getId()))
				.filter(product -> workshopId == null
						|| product.getWorkshop() != null && workshopId.equals(product.getWorkshop().getId()))
				.filter(product -> minPrice == null
						|| normalizePrice(product.getBasePrice()).compareTo(minPrice) >= 0)
				.filter(product -> maxPrice == null
						|| normalizePrice(product.getBasePrice()).compareTo(maxPrice) <= 0)
				.sorted(buildProductComparator(pageable))
				.toList();

		int start = Math.toIntExact(pageable.getOffset());
		int end = Math.min(start + pageable.getPageSize(), filtered.size());
		Set<Long> favoriteIds = getFavoriteProductIdsForCurrentCustomer();
		List<ProductSummaryResponseRecord> content = start >= end
				? List.of()
				: filtered.subList(start, end).stream().map(product -> toProductSummary(product, favoriteIds)).toList();
		return new PageImpl<>(content, pageable, filtered.size());
	}

	public ProductDetailResponseRecord getProductDetail(Long productId) {
		ProductEntity product = productRepository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("Product not found"));
		if (!isPubliclyAccessible(product) && !canViewHiddenProduct(product)) {
			throw new IllegalArgumentException("Product not found");
		}
		boolean isFavorite = getFavoriteProductIdsForCurrentCustomer().contains(productId);
		return buildProductDetail(product, isFavorite);
	}

	public Page<AdminProductReviewRecord> listProductsForAdmin(ProductApprovalStatus status, Pageable pageable) {
		Page<ProductEntity> page = status == null
				? productRepository.findAll(pageable)
				: productRepository.findByApprovalStatus(status, pageable);
		return page.map(this::toAdminProductReview);
	}

	public AdminProductReviewRecord getProductForAdmin(Long productId) {
		ProductEntity product = productRepository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("Product not found"));
		return toAdminProductReview(product);
	}

	@Caching(evict = {
			@CacheEvict(cacheNames = "catalog:newest-products", allEntries = true),
			@CacheEvict(cacheNames = "catalog:product-detail", key = "#productId")
	})
	public AdminProductReviewRecord reviewProduct(Long productId, ProductApprovalRequest request) {
		ProductEntity product = productRepository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("Product not found"));
		if (request.approved()) {
			product.setApprovalStatus(ProductApprovalStatus.APPROVED);
			product.setIsVisible(true);
		} else {
			product.setApprovalStatus(ProductApprovalStatus.REJECTED);
			product.setIsVisible(false);
		}
		product.setAdminNote(request.adminNote());
		product.setReviewedAt(Instant.now());
		productRepository.save(product);
		return toAdminProductReview(product);
	}

	@Caching(evict = {
			@CacheEvict(cacheNames = "catalog:newest-products", allEntries = true),
			@CacheEvict(cacheNames = "catalog:product-detail", key = "#productId")
	})
	public ProductDetailResponseRecord updateProductVisibility(Long productId, ProductVisibilityUpdateRequest request) {
		ProductEntity product = getProductForWorkshop(productId);
		if (Boolean.TRUE.equals(request.isVisible())
				&& !ProductApprovalStatus.APPROVED.equals(product.getApprovalStatus())) {
			throw new IllegalStateException("Product must be approved by admin before publishing");
		}
		product.setIsVisible(Boolean.TRUE.equals(request.isVisible()));
		productRepository.save(product);
		return buildProductDetail(product, false);
	}

	public ProductVariantResponseRecord addVariant(Long productId, ProductVariantRequest request) {
		ProductEntity product = getProductForWorkshop(productId);
		ProductVariantEntity variant = ProductVariantEntity.builder()
				.product(product)
				.skuCode(request.skuCode())
				.color(request.color())
				.size(request.size())
				.price(request.price())
				.stockQuantity(request.stockQuantity())
				.build();
		ProductVariantResponseRecord response = toVariantResponse(productVariantRepository.save(variant));
		evictProductDetailCache(productId);
		return response;
	}

	public ProductVariantResponseRecord updateVariant(Long variantId, ProductVariantRequest request) {
		ProductVariantEntity variant = getVariantForWorkshop(variantId);
		if (request.skuCode() != null) {
			variant.setSkuCode(request.skuCode());
		}
		if (request.color() != null) {
			variant.setColor(request.color());
		}
		if (request.size() != null) {
			variant.setSize(request.size());
		}
		if (request.price() != null) {
			variant.setPrice(request.price());
		}
		if (request.stockQuantity() != null) {
			variant.setStockQuantity(request.stockQuantity());
		}
		productVariantRepository.save(variant);
		evictProductDetailCache(variant.getProduct().getId());
		return toVariantResponse(variant);
	}

	public ProductVariantResponseRecord updateVariantStock(Long variantId, StockUpdateRequest request) {
		ProductVariantEntity variant = getVariantForWorkshop(variantId);
		variant.setStockQuantity(request.stockQuantity());
		productVariantRepository.save(variant);
		evictProductDetailCache(variant.getProduct().getId());
		return toVariantResponse(variant);
	}

	public void deleteVariant(Long variantId) {
		ProductVariantEntity variant = getVariantForWorkshop(variantId);
		Long productId = variant.getProduct().getId();
		productVariantRepository.delete(variant);
		evictProductDetailCache(productId);
	}

	@Caching(evict = {
			@CacheEvict(cacheNames = "catalog:newest-products", allEntries = true),
			@CacheEvict(cacheNames = "catalog:product-detail", key = "#productId")
	})
	public ProductDetailResponseRecord replaceProductImages(Long productId, List<ProductImageRequest> images) {
		ProductEntity product = getProductForWorkshop(productId);
		replaceImages(product, images);
		return buildProductDetail(product, false);
	}

	@Caching(evict = {
			@CacheEvict(cacheNames = "catalog:newest-products", allEntries = true),
			@CacheEvict(cacheNames = "catalog:product-detail", key = "#productId")
	})
	public ProductDetailResponseRecord addProductImageUpload(Long productId, MultipartFile image, Boolean isThumbnail) {
		ProductEntity product = getProductForWorkshop(productId);
		List<ProductImageEntity> existing = productImageRepository.findByProductId(product.getId());
		boolean wantThumbnail = Boolean.TRUE.equals(isThumbnail) || existing.isEmpty();
		if (wantThumbnail) {
			for (ProductImageEntity current : existing) {
				if (Boolean.TRUE.equals(current.getIsThumbnail())) {
					current.setIsThumbnail(false);
					productImageRepository.save(current);
				}
			}
		}
		ProductImageEntity entity = ProductImageEntity.builder()
				.product(product)
				.imageUrl(mediaStorageService.store(image, "product-image"))
				.isThumbnail(wantThumbnail)
				.sortOrder(nextImageSortOrder(existing))
				.build();
		productImageRepository.save(entity);
		return buildProductDetail(product, false);
	}

	private UserEntity getWorkshopUser() {
		UserEntity user = getCurrentUser();
		if (!Role.WORKSHOP.equals(user.getRole())) {
			throw new IllegalStateException("User is not a workshop");
		}
		return user;
	}

	private UserEntity getCustomerUser() {
		UserEntity user = getCurrentUser();
		if (!Role.CUSTOMER.equals(user.getRole())) {
			throw new IllegalStateException("User is not a customer");
		}
		return user;
	}

	private UserEntity getCurrentUser() {
		String email = SecurityUtils.getCurrentUserEmail();
		if (email == null || email.isBlank()) {
			throw new IllegalStateException("Unauthenticated");
		}
		return userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
	}

	private UserEntity getCurrentUserIfPresent() {
		String email = SecurityUtils.getCurrentUserEmail();
		if (email == null || email.isBlank()) {
			return null;
		}
		return userRepository.findByEmail(email).orElse(null);
	}

	private boolean canViewHiddenProduct(ProductEntity product) {
		UserEntity user = getCurrentUserIfPresent();
		if (user == null || user.getRole() == null) {
			return false;
		}
		if (Role.ADMIN.equals(user.getRole())) {
			return true;
		}
		return Role.WORKSHOP.equals(user.getRole())
				&& product.getWorkshop() != null
				&& product.getWorkshop().getId().equals(user.getId());
	}

	private Set<Long> getFavoriteProductIdsForCurrentCustomer() {
		UserEntity user = getCurrentUserIfPresent();
		if (user == null || !Role.CUSTOMER.equals(user.getRole())) {
			return Set.of();
		}
		return productFavoriteRepository.findByCustomerId(user.getId()).stream()
				.map(favorite -> favorite.getProduct() != null ? favorite.getProduct().getId() : null)
				.filter(productId -> productId != null)
				.collect(Collectors.toSet());
	}

	private ProductEntity getProductForWorkshop(Long productId) {
		UserEntity workshop = getWorkshopUser();
		ProductEntity product = productRepository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("Product not found"));
		if (product.getWorkshop() == null || !product.getWorkshop().getId().equals(workshop.getId())) {
			throw new IllegalStateException("No permission for product");
		}
		return product;
	}

	private ProductVariantEntity getVariantForWorkshop(Long variantId) {
		UserEntity workshop = getWorkshopUser();
		ProductVariantEntity variant = productVariantRepository.findById(variantId)
				.orElseThrow(() -> new IllegalArgumentException("Variant not found"));
		ProductEntity product = variant.getProduct();
		if (product == null || product.getWorkshop() == null
				|| !product.getWorkshop().getId().equals(workshop.getId())) {
			throw new IllegalStateException("No permission for variant");
		}
		return variant;
	}

	private void replaceVariants(ProductEntity product, List<ProductVariantRequest> variants) {
		productVariantRepository.deleteByProductId(product.getId());
		if (variants == null || variants.isEmpty()) {
			return;
		}
		for (ProductVariantRequest variant : variants) {
			ProductVariantEntity entity = ProductVariantEntity.builder()
					.product(product)
					.skuCode(variant.skuCode())
					.color(variant.color())
					.size(variant.size())
					.price(variant.price())
					.stockQuantity(variant.stockQuantity())
					.build();
			productVariantRepository.save(entity);
		}
	}

	private void replaceImages(ProductEntity product, List<ProductImageRequest> images) {
		productImageRepository.deleteByProductId(product.getId());
		if (images == null || images.isEmpty()) {
			return;
		}
		boolean thumbnailSet = false;
		for (int index = 0; index < images.size(); index++) {
			ProductImageRequest image = images.get(index);
			boolean isThumbnail = Boolean.TRUE.equals(image.isThumbnail()) && !thumbnailSet;
			if (isThumbnail) {
				thumbnailSet = true;
			}
			Integer sortOrder = image.sortOrder() != null ? image.sortOrder() : index;
			ProductImageEntity entity = ProductImageEntity.builder()
					.product(product)
					.imageUrl(image.imageUrl())
					.isThumbnail(isThumbnail)
					.sortOrder(sortOrder)
					.build();
			productImageRepository.save(entity);
		}
	}

	private ProductDetailResponseRecord buildProductDetail(ProductDetailCacheRecord product, boolean isFavorite) {
		ProductEntity source = productRepository.findById(product.id())
				.orElseThrow(() -> new IllegalArgumentException("Product not found"));
		return new ProductDetailResponseRecord(
				product.id(),
				product.name(),
				product.basePrice(),
				product.description(),
				product.categoryId(),
				resolveCategoryName(source),
				product.workshopId(),
				resolveWorkshopName(source),
				sortImages(product.images()),
				product.variants(),
				isFavorite,
				product.createdAt(),
				source.getIsVisible(),
				source.getApprovalStatus(),
				source.getAdminNote());
	}

	private ProductDetailResponseRecord buildProductDetail(ProductEntity product, boolean isFavorite) {
		List<ProductImageResponseRecord> images = sortedProductImages(product.getId());
		List<ProductVariantResponseRecord> variants = productVariantRepository.findByProductId(product.getId())
				.stream()
				.map(this::toVariantResponse)
				.toList();
		return new ProductDetailResponseRecord(
				product.getId(),
				product.getName(),
				product.getBasePrice(),
				product.getDescription(),
				product.getCategory() != null ? product.getCategory().getId() : null,
				resolveCategoryName(product),
				product.getWorkshop() != null ? product.getWorkshop().getId() : null,
				resolveWorkshopName(product),
				images,
				variants,
				isFavorite,
				product.getCreatedAt(),
				product.getIsVisible(),
				product.getApprovalStatus(),
				product.getAdminNote());
	}

	private List<ProductImageResponseRecord> sortedProductImages(Long productId) {
		return productImageRepository.findByProductId(productId)
				.stream()
				.sorted(Comparator
						.comparing(ProductImageEntity::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparing(ProductImageEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())))
				.map(this::toImageResponse)
				.toList();
	}

	private List<ProductImageResponseRecord> sortImages(List<ProductImageResponseRecord> images) {
		if (images == null || images.isEmpty()) {
			return List.of();
		}
		return images.stream()
				.sorted(Comparator
						.comparing(ProductImageResponseRecord::sortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparing(ProductImageResponseRecord::id, Comparator.nullsLast(Comparator.naturalOrder())))
				.toList();
	}

	private ProductImageResponseRecord toImageResponse(ProductImageEntity image) {
		return new ProductImageResponseRecord(
				image.getId(),
				image.getImageUrl(),
				image.getIsThumbnail(),
				image.getSortOrder());
	}

	private String resolveCategoryName(ProductEntity product) {
		return product.getCategory() != null ? product.getCategory().getName() : null;
	}

	private String resolveWorkshopName(ProductEntity product) {
		if (product.getWorkshop() == null) {
			return null;
		}
		return workshopProfileRepository.findByUserId(product.getWorkshop().getId())
				.map(profile -> profile.getShopName())
				.orElse(null);
	}

	private int nextImageSortOrder(List<ProductImageEntity> existing) {
		return existing.stream()
				.map(ProductImageEntity::getSortOrder)
				.filter(order -> order != null)
				.max(Integer::compareTo)
				.orElse(-1) + 1;
	}

	private void evictProductDetailCache(Long productId) {
		if (productId == null) {
			return;
		}
		catalogCacheService.evictProductDetail(productId);
	}

	private ProductVariantResponseRecord toVariantResponse(ProductVariantEntity variant) {
		return new ProductVariantResponseRecord(
				variant.getId(),
				variant.getSkuCode(),
				variant.getColor(),
				variant.getSize(),
				variant.getPrice(),
				variant.getStockQuantity());
	}

	private ProductSummaryResponseRecord toProductSummary(ProductEntity product, Set<Long> favoriteIds) {
		List<ProductImageEntity> images = productImageRepository.findByProductId(product.getId());
		String thumbnailUrl = images.stream()
				.filter(image -> Boolean.TRUE.equals(image.getIsThumbnail()))
				.map(ProductImageEntity::getImageUrl)
				.findFirst()
				.orElseGet(() -> images.isEmpty() ? null : images.get(0).getImageUrl());
		return new ProductSummaryResponseRecord(
				product.getId(),
				product.getName(),
				product.getBasePrice(),
				thumbnailUrl,
				product.getWorkshop() != null ? product.getWorkshop().getId() : null,
				favoriteIds.contains(product.getId()),
				product.getIsVisible(),
				product.getApprovalStatus(),
				product.getAdminNote(),
				product.getCategory() != null ? product.getCategory().getName() : null,
				product.getDescription(),
				(int) productVariantRepository.countByProductId(product.getId()),
				images.size(),
				product.getCreatedAt());
	}

	private AdminProductReviewRecord toAdminProductReview(ProductEntity product) {
		List<ProductImageEntity> images = productImageRepository.findByProductId(product.getId());
		String thumbnailUrl = images.stream()
				.filter(image -> Boolean.TRUE.equals(image.getIsThumbnail()))
				.map(ProductImageEntity::getImageUrl)
				.findFirst()
				.orElseGet(() -> images.isEmpty() ? null : images.get(0).getImageUrl());
		Long workshopId = product.getWorkshop() != null ? product.getWorkshop().getId() : null;
		String workshopName = workshopId == null
				? null
				: workshopProfileRepository.findByUserId(workshopId)
						.map(profile -> profile.getShopName())
						.orElse(null);
		return new AdminProductReviewRecord(
				product.getId(),
				product.getName(),
				product.getDescription(),
				product.getBasePrice(),
				thumbnailUrl,
				product.getCategory() != null ? product.getCategory().getId() : null,
				product.getCategory() != null ? product.getCategory().getName() : null,
				workshopId,
				workshopName,
				product.getApprovalStatus(),
				product.getIsVisible(),
				product.getAdminNote(),
				product.getCreatedAt());
	}

	private void markProductPendingReview(ProductEntity product) {
		product.setApprovalStatus(ProductApprovalStatus.PENDING);
		product.setIsVisible(false);
		product.setAdminNote(null);
		product.setReviewedAt(null);
	}

	private boolean isPubliclyAccessible(ProductEntity product) {
		return ProductApprovalStatus.APPROVED.equals(product.getApprovalStatus())
				&& Boolean.TRUE.equals(product.getIsVisible());
	}

	private Comparator<ProductEntity> buildProductComparator(Pageable pageable) {
		Comparator<ProductEntity> comparator = null;
		if (pageable != null && pageable.getSort().isSorted()) {
			for (Sort.Order order : pageable.getSort()) {
				Comparator<ProductEntity> orderComparator = comparatorForProperty(order.getProperty());
				if (order.isDescending()) {
					orderComparator = orderComparator.reversed();
				}
				comparator = comparator == null ? orderComparator : comparator.thenComparing(orderComparator);
			}
		}
		if (comparator == null) {
			comparator = Comparator.comparing(
					ProductEntity::getCreatedAt,
					Comparator.nullsLast(Comparator.naturalOrder())).reversed();
		}
		return comparator;
	}

	private Comparator<ProductEntity> comparatorForProperty(String property) {
		return switch (property) {
			case "name" -> Comparator.comparing(
					ProductEntity::getName,
					Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
			case "basePrice" -> Comparator.comparing(
					ProductEntity::getBasePrice,
					Comparator.nullsLast(Comparator.naturalOrder()));
			case "createdAt" -> Comparator.comparing(
					ProductEntity::getCreatedAt,
					Comparator.nullsLast(Comparator.naturalOrder()));
			case "id" -> Comparator.comparing(
					ProductEntity::getId,
					Comparator.nullsLast(Comparator.naturalOrder()));
			default -> Comparator.comparing(
					ProductEntity::getCreatedAt,
					Comparator.nullsLast(Comparator.naturalOrder()));
		};
	}

	private boolean containsIgnoreCase(String value, String keyword) {
		if (value == null || keyword == null) {
			return false;
		}
		return value.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
	}

	private BigDecimal normalizePrice(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
