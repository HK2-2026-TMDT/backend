package vn.io.sanmaymac.modules.user.dto;

import vn.io.sanmaymac.common.enums.KycStatus;

public record WorkshopKycResponseRecord(
        String taxCode,
        String licenseUrl,
        KycStatus status,
        String adminNote) {
}
