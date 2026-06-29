package vn.io.sanmaymac.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterWorkshopRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank String fullName,
        String phoneNumber,
        @NotBlank String shopName,
        @NotBlank String workshopAddress,
        Integer productionCapacity,
        String description,
        String taxCode,
        String bankName,
        String bankAccountNo,
        String bankAccountName) {
}
