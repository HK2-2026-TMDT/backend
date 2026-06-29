package vn.io.sanmaymac.modules.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.mail.internet.MimeMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import vn.io.sanmaymac.common.utils.SecurityUtils;
import vn.io.sanmaymac.common.enums.Role;
import vn.io.sanmaymac.common.enums.UserStatus;
import vn.io.sanmaymac.modules.auth.dto.AuthResponseRecord;
import vn.io.sanmaymac.modules.auth.dto.FirebaseLoginRequest;
import vn.io.sanmaymac.modules.auth.dto.ForgotPasswordRequest;
import vn.io.sanmaymac.modules.auth.dto.LoginRequest;
import vn.io.sanmaymac.modules.auth.dto.PasswordResetTokenResponseRecord;
import vn.io.sanmaymac.modules.auth.dto.RefreshTokenRequest;
import vn.io.sanmaymac.modules.auth.dto.RegisterRequest;
import vn.io.sanmaymac.modules.auth.dto.RegisterAdminRequest;
import vn.io.sanmaymac.modules.auth.dto.RegisterResponseRecord;
import vn.io.sanmaymac.modules.auth.dto.RegisterWorkshopRequest;
import vn.io.sanmaymac.modules.auth.dto.ResetPasswordRequest;
import vn.io.sanmaymac.modules.auth.entity.EmailVerificationTokenEntity;
import vn.io.sanmaymac.modules.auth.entity.PasswordResetTokenEntity;
import vn.io.sanmaymac.modules.auth.entity.RefreshTokenEntity;
import vn.io.sanmaymac.modules.auth.repository.EmailVerificationTokenRepository;
import vn.io.sanmaymac.modules.auth.repository.PasswordResetTokenRepository;
import vn.io.sanmaymac.modules.auth.repository.RefreshTokenRepository;
import vn.io.sanmaymac.modules.user.entity.UserEntity;
import vn.io.sanmaymac.modules.user.entity.WorkshopProfileEntity;
import vn.io.sanmaymac.modules.user.repository.UserRepository;
import vn.io.sanmaymac.modules.user.repository.WorkshopProfileRepository;
import vn.io.sanmaymac.security.jwt.JwtTokenProvider;

@Service
@Transactional
public class AuthService {
    private final ObjectProvider<FirebaseAuth> firebaseAuthProvider;
    private final UserRepository userRepository;
    private final WorkshopProfileRepository workshopProfileRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final long verifyTokenExpMinutes;
    private final long resetTokenExpMinutes;
    private final long refreshTokenExpMinutes;
    private final String verifyEmailUrlTemplate;
    private final String bootstrapAdminSecret;

    public AuthService(
            ObjectProvider<FirebaseAuth> firebaseAuthProvider,
            UserRepository userRepository,
            WorkshopProfileRepository workshopProfileRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder,
            JavaMailSender mailSender,
            @Value("${app.auth.verify-token-exp-minutes:60}") long verifyTokenExpMinutes,
            @Value("${app.auth.reset-token-exp-minutes:30}") long resetTokenExpMinutes,
            @Value("${app.auth.refresh-token-exp-minutes:43200}") long refreshTokenExpMinutes,
            @Value("${app.auth.verify-email-url-template:http://localhost:8080/api/auth/verify-email?token=%s}") String verifyEmailUrlTemplate,
            @Value("${app.auth.bootstrap-admin-secret:}") String bootstrapAdminSecret) {
        this.firebaseAuthProvider = firebaseAuthProvider;
        this.userRepository = userRepository;
        this.workshopProfileRepository = workshopProfileRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.verifyTokenExpMinutes = verifyTokenExpMinutes;
        this.resetTokenExpMinutes = resetTokenExpMinutes;
        this.refreshTokenExpMinutes = refreshTokenExpMinutes;
        this.verifyEmailUrlTemplate = verifyEmailUrlTemplate;
        this.bootstrapAdminSecret = bootstrapAdminSecret;
    }

    public AuthResponseRecord loginWithFirebase(FirebaseLoginRequest request) {
        FirebaseToken token = verifyFirebaseToken(request.idToken());
        String email = token.getEmail();
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Firebase token does not contain email");
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseGet(() -> createUserFromFirebase(token));

        boolean trustedSocialProvider = isTrustedSocialProvider(token);
        if ((Boolean.TRUE.equals(token.isEmailVerified()) || trustedSocialProvider)
                && !Boolean.TRUE.equals(user.getIsEmailVerified())) {
            user.setIsEmailVerified(true);
            if (UserStatus.UNVERIFIED.equals(user.getStatus())) {
                user.setStatus(UserStatus.ACTIVE);
            }
            userRepository.save(user);
        }

        ensureActiveUser(user);
        return buildAuthResponse(user);
    }

    public AuthResponseRecord login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        ensureActiveUser(user);
        return buildAuthResponse(user);
    }

    public RegisterResponseRecord register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email already exists");
        }

        UserEntity user = UserEntity.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .isEmailVerified(false)
                .role(Role.CUSTOMER)
                .status(UserStatus.UNVERIFIED)
                .build();

        UserEntity savedUser = userRepository.save(user);
        EmailVerificationTokenEntity token = createEmailVerificationToken(savedUser);
        sendVerificationEmail(savedUser, token.getToken());
        return new RegisterResponseRecord(savedUser.getId(), savedUser.getEmail(), token.getToken());
    }

    public RegisterResponseRecord registerAdmin(RegisterAdminRequest request, String providedBootstrapSecret) {
        ensureCanRegisterAdmin(providedBootstrapSecret);
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email already exists");
        }

        UserEntity user = UserEntity.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .isEmailVerified(true)
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        UserEntity savedUser = userRepository.save(user);
        return new RegisterResponseRecord(savedUser.getId(), savedUser.getEmail(), null);
    }

    private void ensureCanRegisterAdmin(String providedBootstrapSecret) {
        if (isCurrentUserAdmin()) {
            return;
        }

        boolean hasAdmin = userRepository.existsByRole(Role.ADMIN);
        if (!hasAdmin
                && bootstrapAdminSecret != null
                && !bootstrapAdminSecret.isBlank()
                && bootstrapAdminSecret.equals(providedBootstrapSecret)) {
            return;
        }

        if (!hasAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Bootstrap secret is required to create the first admin account");
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can create admin account");
    }

    private boolean isCurrentUserAdmin() {
        String email = SecurityUtils.getCurrentUserEmail();
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            return false;
        }
        return userRepository.findByEmail(email)
                .map(user -> Role.ADMIN.equals(user.getRole()))
                .orElse(false);
    }

    public RegisterResponseRecord registerWorkshop(RegisterWorkshopRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Email already exists");
        }

        UserEntity user = UserEntity.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phoneNumber(request.phoneNumber())
                .isEmailVerified(false)
                .role(Role.WORKSHOP)
                .status(UserStatus.UNVERIFIED)
                .build();

        UserEntity savedUser = userRepository.save(user);

        WorkshopProfileEntity profile = WorkshopProfileEntity.builder()
                .user(savedUser)
                .shopName(request.shopName())
                .workshopAddress(request.workshopAddress())
                .productionCapacity(request.productionCapacity())
                .description(request.description())
                .taxCode(request.taxCode())
                .bankName(request.bankName())
                .bankAccountNo(request.bankAccountNo())
                .bankAccountName(request.bankAccountName())
                .isVerified(false)
                .ratingAvg(0.0)
                .build();

        workshopProfileRepository.save(profile);

        EmailVerificationTokenEntity token = createEmailVerificationToken(savedUser);
        sendVerificationEmail(savedUser, token.getToken());
        return new RegisterResponseRecord(savedUser.getId(), savedUser.getEmail(), token.getToken());
    }

    public void verifyEmail(String tokenValue) {
        EmailVerificationTokenEntity token = emailVerificationTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        validateTokenState(token.getExpiresAt(), token.getUsedAt());

        UserEntity user = token.getUser();
        user.setIsEmailVerified(true);
        if (UserStatus.UNVERIFIED.equals(user.getStatus())) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        emailVerificationTokenRepository.save(token);
    }

    public PasswordResetTokenResponseRecord forgotPassword(ForgotPasswordRequest request) {
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));

        PasswordResetTokenEntity token = createPasswordResetToken(user);
        return new PasswordResetTokenResponseRecord(token.getToken());
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetTokenEntity token = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset token"));

        validateTokenState(token.getExpiresAt(), token.getUsedAt());

        UserEntity user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);
    }

    public AuthResponseRecord refreshToken(RefreshTokenRequest request) {
        RefreshTokenEntity token = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        validateTokenState(token.getExpiresAt(), token.getUsedAt());

        UserEntity user = token.getUser();
        ensureActiveUser(user);

        token.setUsedAt(Instant.now());
        refreshTokenRepository.save(token);

        RefreshTokenEntity newToken = createRefreshToken(user);
        return buildAuthResponse(user, newToken.getToken());
    }

    private FirebaseToken verifyFirebaseToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("idToken is required");
        }
        FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
        if (firebaseAuth == null) {
            throw new IllegalStateException("Firebase is not configured. Set FIREBASE_SERVICE_ACCOUNT_JSON in .env");
        }
        try {
            return firebaseAuth.verifyIdToken(idToken);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Firebase token", ex);
        }
    }

    private AuthResponseRecord buildAuthResponse(UserEntity user) {
        RefreshTokenEntity refreshToken = createRefreshToken(user);
        return buildAuthResponse(user, refreshToken.getToken());
    }

    private AuthResponseRecord buildAuthResponse(UserEntity user, String refreshToken) {
        String jwt = jwtTokenProvider.generateToken(user);
        return new AuthResponseRecord(
                jwt,
                "Bearer",
                jwtTokenProvider.getExpirationSeconds(),
                user.getId(),
                user.getRole().name(),
                refreshToken);
    }

    private UserEntity createUserFromFirebase(FirebaseToken token) {
        boolean emailVerified = token.isEmailVerified() || isTrustedSocialProvider(token);
        UserStatus status = emailVerified ? UserStatus.ACTIVE : UserStatus.UNVERIFIED;

        UserEntity user = UserEntity.builder()
                .email(token.getEmail())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fullName(token.getName())
                .avatarUrl(token.getPicture())
                .isEmailVerified(emailVerified)
                .role(Role.CUSTOMER)
                .status(status)
                .build();

        return userRepository.save(user);
    }

    private boolean isTrustedSocialProvider(FirebaseToken token) {
        if (token == null || token.getClaims() == null) {
            return false;
        }
        Object firebaseClaim = token.getClaims().get("firebase");
        if (!(firebaseClaim instanceof Map<?, ?> firebaseMap)) {
            return false;
        }
        Object provider = firebaseMap.get("sign_in_provider");
        if (!(provider instanceof String providerId)) {
            return false;
        }
        return "google.com".equals(providerId) || "facebook.com".equals(providerId);
    }

    private EmailVerificationTokenEntity createEmailVerificationToken(UserEntity user) {
        Instant expiresAt = Instant.now().plus(verifyTokenExpMinutes, ChronoUnit.MINUTES);
        EmailVerificationTokenEntity token = EmailVerificationTokenEntity.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(expiresAt)
                .build();
        return emailVerificationTokenRepository.save(token);
    }

    private void sendVerificationEmail(UserEntity user, String tokenValue) {
        String verificationUrl = String.format(verifyEmailUrlTemplate, tokenValue);
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(user.getEmail());
            helper.setSubject("Verify your SanMayMac account");
            helper.setText(buildVerificationEmailBody(user, verificationUrl), true);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to send verification email", ex);
        }
    }

    private String buildVerificationEmailBody(UserEntity user, String verificationUrl) {
        String fullName = user.getFullName() == null || user.getFullName().isBlank()
                ? user.getEmail()
                : user.getFullName();
        return """
                <div style="font-family:Arial,sans-serif;line-height:1.6">
                  <p>Hi %s,</p>
                  <p>Thanks for registering at SanMayMac. Please verify your email by clicking the link below:</p>
                  <p><a href="%s">Verify email</a></p>
                  <p>If the button does not work, open this URL:</p>
                  <p>%s</p>
                  <p>This link expires in %d minutes.</p>
                </div>
                """.formatted(fullName, verificationUrl, verificationUrl, verifyTokenExpMinutes);
    }

    private PasswordResetTokenEntity createPasswordResetToken(UserEntity user) {
        Instant expiresAt = Instant.now().plus(resetTokenExpMinutes, ChronoUnit.MINUTES);
        PasswordResetTokenEntity token = PasswordResetTokenEntity.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(expiresAt)
                .build();
        return passwordResetTokenRepository.save(token);
    }

    private RefreshTokenEntity createRefreshToken(UserEntity user) {
        Instant expiresAt = Instant.now().plus(refreshTokenExpMinutes, ChronoUnit.MINUTES);
        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiresAt(expiresAt)
                .build();
        return refreshTokenRepository.save(token);
    }

    private void validateTokenState(Instant expiresAt, Instant usedAt) {
        if (usedAt != null) {
            throw new IllegalStateException("Token already used");
        }
        if (Instant.now().isAfter(expiresAt)) {
            throw new IllegalStateException("Token expired");
        }
    }

    private void ensureActiveUser(UserEntity user) {
        if (UserStatus.LOCKED.equals(user.getStatus())
                || UserStatus.DEACTIVATED.equals(user.getStatus())
                || UserStatus.BANNED.equals(user.getStatus())) {
            throw new IllegalStateException("User account is not active");
        }
        if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new IllegalStateException("Email is not verified");
        }
    }
}
