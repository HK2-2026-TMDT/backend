package vn.io.sanmaymac.modules.auth.dto;

public record RegisterResponseRecord(Long userId, String email, String verificationToken) {
}
