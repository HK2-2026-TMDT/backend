package vn.io.sanmaymac.config.dev;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.io.sanmaymac.common.enums.Role;
import vn.io.sanmaymac.common.enums.UserStatus;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.entity.WorkshopProfileEntity;
import vn.io.sanmaymac.modules.user.repository.UserRepository;
import vn.io.sanmaymac.modules.user.repository.WorkshopProfileRepository;

@Slf4j
@Component
@Profile({"docker", "local"})
@ConditionalOnProperty(name = "app.dev.seed-accounts", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DevAccountSeeder implements ApplicationRunner {
    private static final List<DevAccount> DEV_ACCOUNTS = List.of(
            new DevAccount(
                    "admin@sanmaymac.vn",
                    "Admin@123",
                    "Quản trị viên Demo",
                    Role.ADMIN,
                    null),
            new DevAccount(
                    "workshop@sanmaymac.vn",
                    "Workshop@123",
                    "Xưởng May Demo",
                    Role.WORKSHOP,
                    "Xưởng May Demo"),
            new DevAccount(
                    "admin@sanmaymac.local",
                    "Admin@123",
                    "Quản trị viên Hệ thống",
                    Role.ADMIN,
                    null),
            new DevAccount(
                    "workshop1@sanmaymac.local",
                    "Workshop@123",
                    "Nguyễn Văn Hùng",
                    Role.WORKSHOP,
                    "Xưởng May Việt Tiến"));

    private final UserRepository userRepository;
    private final WorkshopProfileRepository workshopProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (DevAccount account : DEV_ACCOUNTS) {
            upsertDevAccount(account);
        }

        log.info("""
                ============================================================
                TÀI KHOẢN DEMO (chỉ dùng môi trường local/docker)
                  Admin    | admin@sanmaymac.vn     | Admin@123
                  Workshop | workshop@sanmaymac.vn | Workshop@123
                (Seed cũ: admin@sanmaymac.local / Admin@123,
                          workshop1@sanmaymac.local / Workshop@123)
                ============================================================""");
    }

    private void upsertDevAccount(DevAccount account) {
        UserEntity user = userRepository.findByEmail(account.email())
                .orElseGet(() -> UserEntity.builder()
                        .email(account.email())
                        .build());

        user.setPassword(passwordEncoder.encode(account.password()));
        user.setFullName(account.fullName());
        user.setRole(account.role());
        user.setStatus(UserStatus.ACTIVE);
        user.setIsEmailVerified(true);
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank()) {
            user.setPhoneNumber("0900000000");
        }

        UserEntity savedUser = userRepository.save(user);

        if (Role.WORKSHOP.equals(account.role())) {
            ensureWorkshopProfile(savedUser, account.shopName());
        }
    }

    private void ensureWorkshopProfile(UserEntity user, String shopName) {
        workshopProfileRepository.findById(user.getId()).ifPresentOrElse(profile -> {
            if (profile.getShopName() == null || profile.getShopName().isBlank()) {
                profile.setShopName(shopName);
            }
            if (profile.getIsVerified() == null) {
                profile.setIsVerified(true);
            }
            workshopProfileRepository.save(profile);
        }, () -> {
            WorkshopProfileEntity profile = WorkshopProfileEntity.builder()
                    .user(user)
                    .shopName(shopName)
                    .workshopAddress("123 Đường Demo, Quận 1, TP.HCM")
                    .taxCode("DEMO-TAX-001")
                    .description("Tài khoản demo xưởng may — dùng cho dev/test")
                    .productionCapacity(1000)
                    .bankName("Vietcombank")
                    .bankAccountNo("0123456789")
                    .bankAccountName(shopName)
                    .isVerified(true)
                    .ratingAvg(4.5)
                    .build();
            workshopProfileRepository.save(profile);
        });
    }

    private record DevAccount(
            String email,
            String password,
            String fullName,
            Role role,
            String shopName) {
    }
}
