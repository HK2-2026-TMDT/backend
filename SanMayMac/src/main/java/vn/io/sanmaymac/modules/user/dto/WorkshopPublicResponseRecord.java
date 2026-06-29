package vn.io.sanmaymac.modules.user.dto;

public record WorkshopPublicResponseRecord(
        Long id,
        String fullName,
        String avatarUrl,
        String shopName,
        String logoUrl,
        String workshopAddress,
        Integer productionCapacity,
        String description,
        boolean isVerified,
        double ratingAvg) {
}
