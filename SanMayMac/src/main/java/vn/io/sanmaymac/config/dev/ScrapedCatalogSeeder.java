package vn.io.sanmaymac.config.dev;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.io.sanmaymac.common.enums.ProductApprovalStatus;
import vn.io.sanmaymac.modules.catalog.entity.CategoryEntity;
import vn.io.sanmaymac.modules.catalog.entity.ProductEntity;
import vn.io.sanmaymac.modules.catalog.entity.ProductImageEntity;
import vn.io.sanmaymac.modules.catalog.entity.ProductVariantEntity;
import vn.io.sanmaymac.modules.catalog.repository.CategoryRepository;
import vn.io.sanmaymac.modules.catalog.repository.ProductImageRepository;
import vn.io.sanmaymac.modules.catalog.repository.ProductRepository;
import vn.io.sanmaymac.modules.catalog.repository.ProductVariantRepository;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.repository.UserRepository;
import vn.io.sanmaymac.modules.user.repository.WorkshopProfileRepository;

@Slf4j
@Component
@Profile({"docker", "local"})
@ConditionalOnProperty(name = "app.dev.seed-demo-data", havingValue = "true", matchIfMissing = true)
@Order(3)
@RequiredArgsConstructor
public class ScrapedCatalogSeeder implements ApplicationRunner {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final UserRepository userRepository;
    private final WorkshopProfileRepository workshopProfileRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<ScrapedSourceRecord> sources = loadSources();
        if (sources.isEmpty()) {
            log.warn("ScrapedCatalogSeeder: không có dữ liệu scraped-catalog-sources.json — bỏ qua.");
            return;
        }

        int imported = 0;
        Set<Long> brandedWorkshops = new HashSet<>();
        for (ScrapedSourceRecord source : sources) {
            UserEntity workshop = userRepository.findByEmail(source.workshopEmail()).orElse(null);
            if (workshop == null) {
                log.warn("ScrapedCatalogSeeder: không tìm thấy {} — bỏ qua {}", source.workshopEmail(), source.sourceId());
                continue;
            }
            if (hasSourceProducts(workshop.getId(), source.productPrefix())) {
                log.info("ScrapedCatalogSeeder: {} đã seed — bỏ qua.", source.sourceId());
                continue;
            }

            CategoryEntity category = ensureCategory(source.category());
            if (brandedWorkshops.add(workshop.getId())) {
                updateWorkshopBranding(workshop, source);
            }

            int index = 1;
            for (ScrapedProductRecord item : source.products()) {
                seedProduct(workshop, category, source, item, index++);
                imported++;
            }
            log.info("ScrapedCatalogSeeder: import {} sản phẩm từ {} ({})",
                    source.products().size(), source.sourceName(), source.sourceUrl());
        }

        if (imported > 0) {
            log.info("ScrapedCatalogSeeder: tổng cộng {} sản phẩm demo từ {} nguồn web.",
                    imported, sources.size());
        }
    }

    private List<ScrapedSourceRecord> loadSources() {
        try (InputStream input = new ClassPathResource("dev/scraped-catalog-sources.json").getInputStream()) {
            return JSON.readValue(input, new TypeReference<List<ScrapedSourceRecord>>() {});
        } catch (Exception ex) {
            log.error("ScrapedCatalogSeeder: không đọc được dev/scraped-catalog-sources.json", ex);
            return List.of();
        }
    }

    private boolean hasSourceProducts(Long workshopId, String prefix) {
        return productRepository.findByWorkshopId(workshopId, Pageable.ofSize(200)).getContent().stream()
                .anyMatch(product -> product.getName() != null && product.getName().startsWith(prefix));
    }

    private CategoryEntity ensureCategory(String categoryName) {
        String name = categoryName == null || categoryName.isBlank() ? "Đồng phục" : categoryName.trim();
        return categoryRepository.findAll().stream()
                .filter(category -> name.equalsIgnoreCase(category.getName()))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(CategoryEntity.builder().name(name).build()));
    }

    private void updateWorkshopBranding(UserEntity workshop, ScrapedSourceRecord source) {
        workshopProfileRepository.findByUserId(workshop.getId()).ifPresent(profile -> {
            if (source.shopName() != null && !source.shopName().isBlank()) {
                profile.setShopName(source.shopName());
            }
            if (source.logoUrl() != null && !source.logoUrl().isBlank()) {
                profile.setLogoUrl(source.logoUrl());
            }
            if (profile.getDescription() == null || profile.getDescription().isBlank()) {
                profile.setDescription("Dữ liệu demo tham khảo từ " + source.sourceUrl());
            }
            workshopProfileRepository.save(profile);
        });
    }

    private void seedProduct(
            UserEntity workshop,
            CategoryEntity category,
            ScrapedSourceRecord source,
            ScrapedProductRecord item,
            int index) {
        String description = item.description();
        if (description == null || description.isBlank()) {
            description = "Sản phẩm tham khảo từ " + source.sourceUrl();
        } else {
            description = description + "\n\n(Nguồn: " + source.sourceUrl() + ")";
        }

        String basePrice = item.basePrice() == null || item.basePrice().isBlank() ? "99000" : item.basePrice();
        String skuPrefix = source.sourceId().toUpperCase().replaceAll("[^A-Z0-9]", "");

        ProductEntity product = productRepository.save(ProductEntity.builder()
                .workshop(workshop)
                .category(category)
                .name(source.productPrefix() + " " + item.name())
                .basePrice(new BigDecimal(basePrice))
                .description(description)
                .isVisible(true)
                .approvalStatus(ProductApprovalStatus.APPROVED)
                .build());

        productVariantRepository.save(ProductVariantEntity.builder()
                .product(product)
                .skuCode(skuPrefix + "-" + index + "-FREE")
                .color("Tùy chọn")
                .size("Free size")
                .price(new BigDecimal(basePrice))
                .stockQuantity(500)
                .build());

        List<String> images = item.images() == null || item.images().isEmpty()
                ? List.of(item.thumbnail())
                : item.images();

        for (int imageIndex = 0; imageIndex < images.size(); imageIndex++) {
            String imageUrl = images.get(imageIndex);
            if (imageUrl == null || imageUrl.isBlank()) {
                continue;
            }
            productImageRepository.save(ProductImageEntity.builder()
                    .product(product)
                    .imageUrl(imageUrl)
                    .isThumbnail(imageIndex == 0)
                    .sortOrder(imageIndex)
                    .build());
        }
    }

    private record ScrapedSourceRecord(
            String sourceId,
            String sourceName,
            String sourceUrl,
            String workshopEmail,
            String shopName,
            String logoUrl,
            String category,
            String productPrefix,
            List<ScrapedProductRecord> products) {
    }

    private record ScrapedProductRecord(
            String name,
            String description,
            String thumbnail,
            String detailUrl,
            List<String> images,
            String basePrice,
            String category) {
    }
}
