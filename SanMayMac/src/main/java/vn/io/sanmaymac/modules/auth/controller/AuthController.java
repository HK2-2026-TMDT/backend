package vn.io.sanmaymac.modules.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import vn.io.sanmaymac.common.dto.ApiResponse;
import vn.io.sanmaymac.modules.auth.dto.AuthMeResponseRecord;
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
import vn.io.sanmaymac.modules.auth.dto.VerifyEmailRequest;
import vn.io.sanmaymac.modules.auth.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthMeResponseRecord>> getCurrentUser() {
        return ResponseEntity.ok(ApiResponse.success("OK", authService.getCurrentUser()));
    }

    @PostMapping("/firebase")
    public ResponseEntity<ApiResponse<AuthResponseRecord>> loginWithFirebase(
            @Valid @RequestBody FirebaseLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login success", authService.loginWithFirebase(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseRecord>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login success", authService.login(request)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponseRecord>> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Register success", authService.register(request)));
    }

    @PostMapping("/register/workshop")
    public ResponseEntity<ApiResponse<RegisterResponseRecord>> registerWorkshop(
            @Valid @RequestBody RegisterWorkshopRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Workshop registered successfully", authService.registerWorkshop(request)));
    }

    @PostMapping("/register/admin")
    public ResponseEntity<ApiResponse<RegisterResponseRecord>> registerAdmin(
            @Valid @RequestBody RegisterAdminRequest request,
            @RequestHeader(value = "X-Admin-Bootstrap-Secret", required = false) String bootstrapSecret) {
        return ResponseEntity.ok(ApiResponse.success(
                "Admin registered successfully",
                authService.registerAdmin(request, bootstrapSecret)));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        authService.verifyEmail(request.token());
        return ResponseEntity.ok(ApiResponse.success("Email verified", null));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmailByToken(
            @RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<PasswordResetTokenResponseRecord>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Reset token generated", authService.forgotPassword(request)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password updated", null));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponseRecord>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authService.refreshToken(request)));
    }
}
