package vn.io.sanmaymac.modules.auth.dto;

import java.time.Instant;

public record AuthMeResponseRecord(
        Long id,
        String email,
        String name,
        String role,
        String avatar,
        Instant createdAt) {
}
