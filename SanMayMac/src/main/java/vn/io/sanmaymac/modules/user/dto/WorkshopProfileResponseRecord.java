package vn.io.sanmaymac.modules.user.dto;

public record WorkshopProfileResponseRecord(
        String shopName,
        String logoUrl,
        String workshopAddress,
        Integer productionCapacity,
        String description,
        boolean isVerified,
        double ratingAvg) {
}
