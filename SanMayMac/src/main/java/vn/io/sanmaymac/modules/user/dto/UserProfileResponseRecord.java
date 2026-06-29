package vn.io.sanmaymac.modules.user.dto;

public record UserProfileResponseRecord(
        Long id,
        String email,
        String fullName,
        String phoneNumber,
        String avatarUrl) {
}
