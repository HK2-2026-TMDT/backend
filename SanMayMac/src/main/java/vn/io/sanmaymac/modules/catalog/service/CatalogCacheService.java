package vn.io.sanmaymac.modules.catalog.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import vn.io.sanmaymac.common.enums.ProductApprovalStatus;
import vn.io.sanmaymac.modules.catalog.dto.CategoryResponseRecord;
import vn.io.sanmaymac.modules.catalog.dto.ProductDetailCacheRecord;
import vn.io.sanmaymac.modules.catalog.dto.ProductImageResponseRecord;
import vn.io.sanmaymac.modules.catalog.dto.ProductSummaryCacheRecord;
import vn.io.sanmaymac.modules.catalog.dto.ProductVariantResponseRecord;
import vn.io.sanmaymac.modules.catalog.entity.CategoryEntity;
import vn.io.sanmaymac.modules.catalog.entity.ProductEntity;
import vn.io.sanmaymac.modules.catalog.repository.CategoryRepository;
import vn.io.sanmaymac.modules.catalog.repository.ProductImageRepository;
import vn.io.sanmaymac.modules.catalog.repository.ProductRepository;
import vn.io.sanmaymac.modules.catalog.repository.ProductVariantRepository;

@Service
public class CatalogCacheService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;

    public CatalogCacheService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            ProductVariantRepository productVariantRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Cacheable(cacheNames = "catalog:categories", key = "'all'")
    public List<CategoryResponseRecord> getCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> new CategoryResponseRecord(
                        category.getId(),
                        category.getName(),
                        category.getParent() != null ? category.getParent().getId() : null))
                .toList();
    }

    @Cacheable(cacheNames = "catalog:newest-products", key = "'top20'")
    public List<ProductSummaryCacheRecord> getNewestProducts() {
        return productRepository
                .findTop20ByIsVisibleTrueAndApprovalStatusOrderByCreatedAtDesc(ProductApprovalStatus.APPROVED)
                .stream()
                .map(this::toSummaryCacheRecord)
                .toList();
    }

    @Cacheable(cacheNames = "catalog:product-detail", key = "#productId")
    public ProductDetailCacheRecord getProductDetail(Long productId) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        List<ProductImageResponseRecord> images = productImageRepository.findByProductId(product.getId())
                .stream()
                .map(image -> new ProductImageResponseRecord(
                        image.getId(), image.getImageUrl(), image.getIsThumbnail()))
                .toList();
        List<ProductVariantResponseRecord> variants = productVariantRepository.findByProductId(product.getId())
                .stream()
                .map(variant -> new ProductVariantResponseRecord(
                        variant.getId(),
                        variant.getSkuCode(),
                        variant.getColor(),
                        variant.getSize(),
                        variant.getPrice(),
                        variant.getStockQuantity()))
                .toList();
        return new ProductDetailCacheRecord(
                product.getId(),
                product.getName(),
                product.getBasePrice(),
                product.getDescription(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getWorkshop() != null ? product.getWorkshop().getId() : null,
                images,
                variants,
                product.getCreatedAt());
    }

    private ProductSummaryCacheRecord toSummaryCacheRecord(ProductEntity product) {
        var images = productImageRepository.findByProductId(product.getId());
        String thumbnailUrl = images.stream()
                .filter(image -> Boolean.TRUE.equals(image.getIsThumbnail()))
                .map(image -> image.getImageUrl())
                .findFirst()
                .orElseGet(() -> images.isEmpty() ? null : images.get(0).getImageUrl());
        return new ProductSummaryCacheRecord(
                product.getId(),
                product.getName(),
                product.getBasePrice(),
                thumbnailUrl,
                product.getWorkshop() != null ? product.getWorkshop().getId() : null);
    }
}