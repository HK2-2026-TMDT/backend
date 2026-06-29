package vn.io.sanmaymac.modules.user.dto;

import java.time.Instant;

public record AdminUserResponseRecord(
        Long id,
        String email,
        String fullName,
        String phoneNumber,
        String avatarUrl,
        String role,
        String status,
        Instant createdAt) {
}
