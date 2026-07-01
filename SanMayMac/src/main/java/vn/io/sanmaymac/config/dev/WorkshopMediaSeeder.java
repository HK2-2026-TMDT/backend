package vn.io.sanmaymac.config.dev;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.entity.WorkshopPortfolioEntity;
import vn.io.sanmaymac.modules.user.entity.WorkshopProfileEntity;
import vn.io.sanmaymac.modules.user.repository.UserRepository;
import vn.io.sanmaymac.modules.user.repository.WorkshopPortfolioRepository;
import vn.io.sanmaymac.modules.user.repository.WorkshopProfileRepository;

@Slf4j
@Component
@Profile({"docker", "local"})
@ConditionalOnProperty(name = "app.dev.seed-demo-data", havingValue = "true", matchIfMissing = true)
@Order(4)
@RequiredArgsConstructor
public class WorkshopMediaSeeder implements ApplicationRunner {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String DEMO_DESCRIPTION_MARKER = "Tài khoản demo xưởng may";

    private final UserRepository userRepository;
    private final WorkshopProfileRepository workshopProfileRepository;
    private final WorkshopPortfolioRepository workshopPortfolioRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<WorkshopMediaRecord> records = loadRecords();
        if (records.isEmpty()) {
            log.warn("WorkshopMediaSeeder: không có dữ liệu workshop-media-seed.json — bỏ qua.");
            return;
        }

        int updated = 0;
        int portfolioAdded = 0;
        for (WorkshopMediaRecord record : records) {
            UserEntity user = userRepository.findByEmail(record.email()).orElse(null);
            if (user == null) {
                log.warn("WorkshopMediaSeeder: không tìm thấy {} — bỏ qua.", record.email());
                continue;
            }

            WorkshopProfileEntity profile = workshopProfileRepository.findByUserId(user.getId()).orElse(null);
            if (profile == null) {
                log.warn("WorkshopMediaSeeder: chưa có profile cho {} — bỏ qua.", record.email());
                continue;
            }

            if (applyBranding(user, profile, record)) {
                updated++;
            }
            portfolioAdded += seedPortfolio(user, record.portfolio());
        }

        log.info("WorkshopMediaSeeder: cập nhật {} xưởng, thêm {} mục portfolio.", updated, portfolioAdded);
    }

    private boolean applyBranding(UserEntity user, WorkshopProfileEntity profile, WorkshopMediaRecord record) {
        boolean changed = false;

        if (isBlank(user.getAvatarUrl()) && !isBlank(record.avatarUrl())) {
            user.setAvatarUrl(record.avatarUrl());
            userRepository.save(user);
            changed = true;
        }

        if (isBlank(profile.getLogoUrl()) && !isBlank(record.logoUrl())) {
            profile.setLogoUrl(record.logoUrl());
            changed = true;
        }

        if (shouldUpdateDescription(profile.getDescription()) && !isBlank(record.description())) {
            profile.setDescription(record.description());
            changed = true;
        }

        if (changed) {
            workshopProfileRepository.save(profile);
        }
        return changed;
    }

    private int seedPortfolio(UserEntity user, List<PortfolioSeedRecord> items) {
        if (items == null || items.isEmpty()) {
            return 0;
        }
        if (!workshopPortfolioRepository.findByWorkshopId(user.getId()).isEmpty()) {
            return 0;
        }

        int added = 0;
        for (PortfolioSeedRecord item : items) {
            if (isBlank(item.title()) || isBlank(item.imageUrl())) {
                continue;
            }
            workshopPortfolioRepository.save(WorkshopPortfolioEntity.builder()
                    .workshop(user)
                    .title(item.title())
                    .imageUrl(item.imageUrl())
                    .description(item.description())
                    .build());
            added++;
        }
        return added;
    }

    private boolean shouldUpdateDescription(String description) {
        return description == null
                || description.isBlank()
                || description.contains(DEMO_DESCRIPTION_MARKER)
                || description.startsWith("Dữ liệu demo tham khảo từ");
    }

    private List<WorkshopMediaRecord> loadRecords() {
        try {
            ClassPathResource resource = new ClassPathResource("dev/workshop-media-seed.json");
            if (!resource.exists()) {
                return List.of();
            }
            try (InputStream input = resource.getInputStream()) {
                return JSON.readValue(input, new TypeReference<>() {});
            }
        } catch (Exception ex) {
            log.error("WorkshopMediaSeeder: không đọc được workshop-media-seed.json", ex);
            return List.of();
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record WorkshopMediaRecord(
            String email,
            String avatarUrl,
            String logoUrl,
            String description,
            List<PortfolioSeedRecord> portfolio) {
    }

    private record PortfolioSeedRecord(String title, String imageUrl, String description) {
    }
}
