package vn.io.sanmaymac.modules.finance.dto;

import jakarta.validation.constraints.NotBlank;

public record BankAccountRequest(
        @NotBlank String bankName,
        @NotBlank String accountNo,
        @NotBlank String accountName) {
}
