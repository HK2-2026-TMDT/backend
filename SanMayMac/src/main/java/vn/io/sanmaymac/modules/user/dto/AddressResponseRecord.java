package vn.io.sanmaymac.modules.user.dto;

public record AddressResponseRecord(
        Long id,
        String receiverName,
        String phone,
        String detailedAddress,
        Integer provinceId,
        Integer districtId,
        String wardCode,
        String provinceName,
        String districtName,
        String wardName,
        String fullAddress,
        boolean isDefault) {
}
